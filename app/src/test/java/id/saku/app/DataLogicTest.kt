package id.saku.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DataLogicTest {

    private fun cal(year: Int, monthZeroBased: Int, day: Int = 1): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, monthZeroBased, day, 12, 0, 0)
        }

    @Test
    fun fmt_formatsNumbersRupiahAndMinusSign() {
        assertEquals("16.421.404", Fmt.num(16_421_404L))
        assertEquals("\u221218.500", Fmt.num(-18_500L))
        assertEquals("Rp 16.421.404", Fmt.rp(16_421_404L))
        assertEquals("Rp \u22125.000", Fmt.rp(-5_000L))

        assertEquals("0", Fmt.sign(0L))
        assertEquals("+6.500.000", Fmt.sign(6_500_000L))
        assertEquals("\u221218.500", Fmt.sign(-18_500L))

        assertEquals("0", Fmt.out(0L))
        assertEquals("0", Fmt.out(-100L))
        assertEquals("\u221218.500", Fmt.out(18_500L))

        assertEquals("16/11", Fmt.tgl("2026-11-16"))
        assertEquals("16.421.404", Fmt.masked(16_421_404L, false))
        assertEquals("\u2022\u2022\u2022\u2022\u2022\u2022", Fmt.masked(16_421_404L, true))
        assertEquals("Rp 16.421.404", Fmt.rpMasked(16_421_404L, false))
        assertEquals("Rp \u2022\u2022\u2022\u2022\u2022\u2022", Fmt.rpMasked(16_421_404L, true))
    }

    @Test
    fun fmt_parsesAndTypesTimeAndMonthLabels() {
        assertEquals("09:05", Fmt.parseTime("0905"))
        assertEquals("09:05", Fmt.parseTime("09:05"))
        assertEquals("14:30", Fmt.parseTime("1430"))
        assertNull(Fmt.parseTime("09"))
        assertNull(Fmt.parseTime("2400"))
        assertNull(Fmt.parseTime("1260"))

        assertEquals("09", Fmt.typeTime("09"))
        assertEquals("09:05", Fmt.typeTime("0905"))
        assertEquals("09:05", Fmt.typeTime("090599"))

        assertTrue(Fmt.weekdayShort("2026-11-16").isNotBlank())
        assertTrue(Fmt.ymLabel("2026-11").contains("2026"))
    }

    @Test
    fun txSignedAndSignedFor_handleInOutAndTransfer() {
        val inc = Tx("1", "2026-11-01", "09:00", "in", 100_000L, "gaji", "", account = "tunai")
        val out = Tx("2", "2026-11-02", "10:00", "out", 25_000L, "makan", "", account = "ewallet")
        val xfer = Tx("3", "2026-11-03", "11:00", "transfer", 40_000L, "lain", "", account = "tunai", toAccount = "ewallet")

        assertEquals(100_000L, inc.signed)
        assertEquals(-25_000L, out.signed)
        assertEquals(0L, xfer.signed)

        assertEquals(100_000L, inc.signedFor("tunai"))
        assertEquals(0L, inc.signedFor("ewallet"))
        assertEquals(-25_000L, out.signedFor("ewallet"))
        assertEquals(-40_000L, xfer.signedFor("tunai"))
        assertEquals(40_000L, xfer.signedFor("ewallet"))
        assertEquals(0L, xfer.signedFor("bank"))
    }

    @Test
    fun accountBalances_sumMatchesTotalBalance_acrossTransfersAndFallbackAccount() {
        val saku = Saku(
            tx = listOf(
                Tx("1", "2026-11-01", "08:00", "in", 500_000L, "gaji", "", account = "tunai"),
                Tx("2", "2026-11-02", "09:00", "transfer", 150_000L, "lain", "", account = "tunai", toAccount = "ewallet"),
                Tx("3", "2026-11-03", "12:00", "out", 30_000L, "makan", "", account = "deleted_acc"),
                Tx("4", "2026-11-04", "13:00", "out", 20_000L, "transport", "", account = "ewallet")
            )
        )

        val perAcc = saku.accountBalances()
        assertEquals(500_000L - 150_000L - 30_000L, perAcc["tunai"])
        assertEquals(150_000L - 20_000L, perAcc["ewallet"])
        assertEquals(saku.balance(), perAcc.values.sum())
    }

    @Test
    fun monthSnapshot_emptyMonth_returnsZeroes() {
        val saku = Saku()
        val snap = saku.monthSnapshot(cal(2026, Calendar.NOVEMBER))

        assertTrue(snap.rows.isEmpty())
        assertTrue(snap.running.isEmpty())
        assertEquals(0L, snap.income)
        assertEquals(0L, snap.outcome)
        assertEquals(0L, snap.diff)
        assertEquals(0L, snap.avgOut)
        assertEquals(0, snap.activeDays)
        assertTrue(snap.spendingByCat.isEmpty())
    }

    @Test
    fun monthSnapshot_crossMonthRunningBalancesAndTransferRules() {
        val octIn = Tx("1", "2026-10-25", "09:00", "in", 1_000_000L, "gaji", "", account = "tunai")
        val novOut = Tx("2", "2026-11-02", "12:00", "out", 200_000L, "makan", "", account = "tunai")
        val novXfer = Tx("3", "2026-11-05", "15:00", "transfer", 300_000L, "lain", "", account = "tunai", toAccount = "ewallet")
        val novIn = Tx("4", "2026-11-10", "18:00", "in", 500_000L, "gaji", "", account = "ewallet")
        val decIn = Tx("5", "2026-12-01", "08:00", "in", 900_000L, "gaji", "", account = "tunai")

        val saku = Saku(tx = listOf(octIn, novOut, novXfer, novIn, decIn))
        val nov = cal(2026, Calendar.NOVEMBER)

        // Unfiltered November snapshot
        val unfiltered = saku.monthSnapshot(nov)
        assertEquals(listOf("4", "3", "2"), unfiltered.rows.map { it.id })
        assertEquals(saku.balanceAtMonthEnd(nov), unfiltered.running.first())
        assertEquals(listOf(1_300_000L, 800_000L, 800_000L), unfiltered.running)
        assertEquals(unfiltered.running, saku.runningBalances(nov, unfiltered.rows))
        assertEquals(500_000L, unfiltered.income)
        assertEquals(200_000L, unfiltered.outcome)
        assertEquals(300_000L, unfiltered.diff)
        assertEquals(200_000L, unfiltered.avgOut)
        assertEquals(3, unfiltered.activeDays)
        assertEquals(listOf("makan" to 200_000L), unfiltered.spendingByCat.map { it.first.id to it.second })

        // Filtered for ewallet: transfer TO ewallet counts as income, NOT spendingByCat
        val ewalletSnap = saku.monthSnapshot(nov, accountFilter = "ewallet")
        assertEquals(listOf("4", "3"), ewalletSnap.rows.map { it.id })
        assertEquals(listOf(800_000L, 300_000L), ewalletSnap.running)
        assertEquals(800_000L, ewalletSnap.income)
        assertEquals(0L, ewalletSnap.outcome)
        assertTrue(ewalletSnap.spendingByCat.isEmpty())

        // Filtered for tunai: transfer FROM tunai counts as outcome, NOT spendingByCat
        val tunaiSnap = saku.monthSnapshot(nov, accountFilter = "tunai")
        assertEquals(listOf("3", "2"), tunaiSnap.rows.map { it.id })
        assertEquals(listOf(500_000L, 800_000L), tunaiSnap.running)
        assertEquals(0L, tunaiSnap.income)
        assertEquals(500_000L, tunaiSnap.outcome)
        assertEquals(listOf("makan" to 200_000L), tunaiSnap.spendingByCat.map { it.first.id to it.second })
    }

    @Test
    fun trendFast_usesFixedWindowsAndSumsOnlyExpenses() {
        val txs = listOf(
            Tx("1", "2026-02-10", "10:00", "out", 50_000L, "makan", ""),
            Tx("2", "2026-07-05", "11:00", "out", 120_000L, "makan", ""),
            Tx("3", "2026-11-16", "12:00", "out", 80_000L, "transport", ""),
            Tx("4", "2026-11-17", "12:00", "in", 999_000L, "gaji", ""),
            Tx("5", "2026-11-18", "12:00", "transfer", 40_000L, "lain", "", toAccount = "ewallet")
        )

        val saku6 = Saku(tx = txs, months = 6)
        val firstHalf = saku6.trendFast(cal(2026, Calendar.MARCH))
        assertEquals(6, firstHalf.size)
        assertEquals(Calendar.JANUARY, firstHalf.first().first.get(Calendar.MONTH))
        assertEquals(50_000L, firstHalf[1].second)

        val secondHalf = saku6.trendFast(cal(2026, Calendar.NOVEMBER))
        assertEquals(6, secondHalf.size)
        assertEquals(Calendar.JULY, secondHalf.first().first.get(Calendar.MONTH))
        assertEquals(120_000L, secondHalf[0].second)
        assertEquals(80_000L, secondHalf[4].second)

        val saku12 = Saku(tx = txs, months = 12)
        val fullYear = saku12.trendFast(cal(2026, Calendar.NOVEMBER))
        assertEquals(12, fullYear.size)
        assertEquals(50_000L, fullYear[1].second)
        assertEquals(120_000L, fullYear[6].second)
        assertEquals(80_000L, fullYear[10].second)
    }

    @Test
    fun glassGuard_blocksHomeAndNonOverlayKeys() {
        val tainted = mapOf("home" to true, "dock" to false, "fab" to true, "unknown" to true)
        val sanitized = tainted.safeUiGlass()
        assertEquals(mapOf("dock" to false, "fab" to true), sanitized)

        val saku = Saku(glass = true, uiGlass = tainted)
        assertFalse(saku.glassPref("home"))
        assertFalse(saku.glassOn("home"))
        assertFalse(saku.glassPref("unknown"))
        assertFalse(saku.glassPref("dock"))
        assertTrue(saku.glassPref("fab"))
        assertFalse(saku.glassPref("sheet"))
    }
}
