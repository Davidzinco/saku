package id.saku.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Tx(
    val id: String,
    val date: String,      // yyyy-MM-dd
    val time: String,      // HH:mm
    val type: String,      // "in" | "out"
    val amount: Long,      // selalu positif
    val cat: String,
    val note: String,
    val goal: String = "",  // id target nabung, kalau transaksi ini setoran/penarikan
    val photo: String = ""  // path berkas di filesDir/photos/, opsional (V1.3)
) {
    val signed: Long get() = if (type == "in") amount else -amount
}

/** Kategori dibedakan lewat glyph, bukan warna (DESIGN.md § Color aturan 2). */
data class Cat(val id: String, val name: String, val glyph: String)

data class Goal(val id: String, val name: String, val target: Long)

data class Saku(
    val tx: List<Tx> = emptyList(),
    val cats: List<Cat> = defaultCats(),
    val goals: List<Goal> = emptyList(),
    val dark: Boolean = true,
    val paletteId: String = "mono",
    val customAccent: Long = 0xFF23418F,
    val customTone: String = "hijau",
    val chart: String = "batang",   // "batang" | "garis"
    val months: Int = 6,            // 6 | 12
    val glass: Boolean = true       // dock & FAB liquid glass (V1.3), bisa dimatikan di Theme
)

val glyphChoices = listOf("food", "car", "home", "bag", "film", "heart", "money", "wallet", "dot")

const val CAT_NABUNG = "nabung"

fun defaultCats() = listOf(
    Cat("makan", "Makan", "food"),
    Cat("transport", "Transport", "car"),
    Cat("tagihan", "Tagihan", "home"),
    Cat("belanja", "Belanja", "bag"),
    Cat("hiburan", "Hiburan", "film"),
    Cat("kesehatan", "Kesehatan", "heart"),
    Cat("gaji", "Gaji", "money"),
    Cat(CAT_NABUNG, "Nabung", "wallet"),
    Cat("lain", "Lainnya", "dot")
)

fun Saku.palette(): Palette = if (paletteId == "custom") {
    Palette("custom", "Custom", toneOf(customTone), customAccent)
} else {
    Palettes.firstOrNull { it.id == paletteId } ?: Palettes[0]
}

fun Saku.tokens(): Tok = buildTokens(palette(), dark)

fun Saku.monthTxs(c: Calendar): List<Tx> {
    val prefix = String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    return tx.filter { it.date.startsWith(prefix) }
        .sortedWith(compareByDescending<Tx> { it.date }.thenByDescending { it.time })
}

fun Saku.income(c: Calendar): Long = monthTxs(c).filter { it.type == "in" }.sumOf { it.amount }
fun Saku.outcome(c: Calendar): Long = monthTxs(c).filter { it.type == "out" }.sumOf { it.amount }
fun Saku.balance(): Long = tx.sumOf { it.signed }

/** Saldo di akhir bulan `c` = semua transaksi sampai akhir bulan itu. */
fun Saku.balanceAtMonthEnd(c: Calendar): Long {
    val next = (c.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, 1)
    }
    val key = Fmt.iso(next.time)
    return tx.filter { it.date < key }.sumOf { it.signed }
}

/**
 * Saldo berjalan untuk daftar bulan yang sudah urut terbaru → terlama.
 * saldoSetelah(i) = saldoAkhirBulan − Σ amount(j) untuk j < i (DESIGN.md § Perhitungan saldo berjalan).
 */
fun Saku.runningBalances(c: Calendar, rows: List<Tx>): List<Long> {
    var acc = balanceAtMonthEnd(c)
    return rows.map { row ->
        val here = acc
        acc -= row.signed
        here
    }
}

/** Saldo berjalan untuk satu transaksi, dipakai sheet detail (V1.3). */
fun Saku.saldoOf(tx: Tx): Long {
    val c = Calendar.getInstance().apply {
        time = Fmt.parse(tx.date)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val rows = monthTxs(c)
    val i = rows.indexOfFirst { it.id == tx.id }
    return if (i < 0) balance() else runningBalances(c, rows)[i]
}

/** Pengeluaran bulan `c` per kategori, terbesar dulu. */
fun Saku.spendingByCat(c: Calendar): List<Pair<Cat, Long>> = monthTxs(c)
    .filter { it.type == "out" }
    .groupBy { it.cat }
    .map { (id, list) -> (catOf(id) ?: Cat(id, "Lainnya", "dot")) to list.sumOf { it.amount } }
    .filter { it.second > 0 }
    .sortedByDescending { it.second }

/**
 * Jendela grafik: 12 bulan = Jan–Des tahun `cursor`; 6 bulan = Jan–Jun / Jul–Des.
 * Jendela tidak bergeser saat pindah bulan di dalamnya.
 */
fun Saku.chartWindow(cursor: Calendar): List<Calendar> {
    val year = cursor.get(Calendar.YEAR)
    val month = cursor.get(Calendar.MONTH)
    val start = if (months >= 12) 0 else if (month <= 5) 0 else 6
    val count = if (months >= 12) 12 else 6
    return (start until start + count).map { m ->
        Calendar.getInstance().apply {
            clear()
            set(year, m, 1, 12, 0, 0)
        }
    }
}

fun Saku.trend(cursor: Calendar): List<Pair<Calendar, Long>> =
    chartWindow(cursor).map { it to outcome(it) }

fun sameMonth(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)

fun Saku.catOf(id: String): Cat? = cats.firstOrNull { it.id == id }
fun Saku.countFor(catId: String): Int = tx.count { it.cat == catId }

/** Saldo target nabung = setoran − penarikan, diturunkan dari transaksi (selalu sinkron dengan saldo). */
fun Saku.savedOf(goalId: String): Long = tx.filter { it.goal == goalId }.sumOf { -it.signed }
fun Saku.savedTotal(): Long = goals.sumOf { savedOf(it.id) }
fun Saku.goalOf(id: String): Goal? = goals.firstOrNull { it.id == id }

fun Saku.withTx(tx: Tx): Saku = copy(
    tx = (this.tx.filter { it.id != tx.id } + tx)
        .sortedWith(compareByDescending<Tx> { it.date }.thenByDescending { it.time })
)

fun Saku.withoutTx(id: String): Saku = copy(tx = tx.filter { it.id != id })

object Fmt {
    private val ID: Locale = Locale.forLanguageTag("id-ID")
    private val nf: NumberFormat = NumberFormat.getIntegerInstance(ID)
    private val raw = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthYear = SimpleDateFormat("MMMM yyyy", ID)
    private val monthAlone = SimpleDateFormat("MMMM", ID)
    private val monthAbbr = SimpleDateFormat("MMM", ID)
    private val dayHuman = SimpleDateFormat("EEE d MMM", ID)
    private val dayFull = SimpleDateFormat("d MMMM yyyy", ID)

    /** 16.421.404 (minus selalu U+2212) */
    fun num(v: Long): String = nf.format(v).replace('-', '\u2212')

    /** Rp 16.421.404 */
    fun rp(v: Long): String = "Rp " + num(v)

    /** +6.500.000 / −18.500 (minus U+2212); nol tanpa tanda */
    fun sign(v: Long): String = when {
        v == 0L -> "0"
        v < 0L -> "\u2212" + num(-v)
        else -> "+" + num(v)
    }

    /** −18.500 untuk pengeluaran; nol tanpa tanda */
    fun out(v: Long): String = if (v <= 0L) "0" else "\u2212" + num(v)

    /** 16/11 */
    fun tgl(dateIso: String): String = dateIso.substring(8, 10) + "/" + dateIso.substring(5, 7)

    fun iso(d: Date): String = raw.format(d)
    fun parse(s: String): Date = raw.parse(s) ?: Date()

    fun monthLabel(c: Calendar): String = monthYear.format(c.time)
    fun monthName(c: Calendar): String = monthAlone.format(c.time)
    fun monthShort(c: Calendar): String = monthAbbr.format(c.time)

    fun dayLabel(dateIso: String): String {
        val now = Calendar.getInstance()
        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        return when (dateIso) {
            iso(now.time) -> "Hari ini"
            iso(yesterday.time) -> "Kemarin"
            else -> dayHuman.format(parse(dateIso))
        }
    }

    /** 16 November 2026 */
    fun longDate(dateIso: String): String = dayFull.format(parse(dateIso))

    fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())

    /** "9:5" / "0905" / "09:05" → "09:05"; null kalau belum valid. */
    fun parseTime(rawInput: String): String? {
        val digits = rawInput.filter { it.isDigit() }.take(4)
        if (digits.length < 3) return null
        val h = digits.take(2).toIntOrNull() ?: return null
        val m = digits.drop(2).padEnd(2, '0').toIntOrNull() ?: return null
        if (h > 23 || m > 59) return null
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    /** "0905" → "09:05" saat mengetik; sisanya dibiarkan apa adanya. */
    fun typeTime(rawInput: String): String {
        val digits = rawInput.filter { it.isDigit() }.take(4)
        return when {
            digits.length <= 2 -> digits
            else -> digits.take(2) + ":" + digits.drop(2)
        }
    }
}

class Store(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("saku", Context.MODE_PRIVATE)

    /** Aplikasi mulai dari nol — tidak ada data contoh yang ditanam. */
    fun load(): Saku {
        val raw = prefs.getString("db", null) ?: return Saku()
        return try {
            val o = JSONObject(raw)
            val tx = o.optJSONArray("tx")?.let { a ->
                (0 until a.length()).map { i ->
                    val t = a.getJSONObject(i)
                    Tx(
                        id = t.getString("id"),
                        date = t.getString("date"),
                        time = t.optString("time", "12:00"),
                        type = t.optString("type", "out"),
                        amount = t.optLong("amount", 0L),
                        cat = t.optString("cat", "lain"),
                        note = t.optString("note", ""),
                        goal = t.optString("goal", ""),
                        photo = t.optString("photo", "")
                    )
                }
            } ?: emptyList()
            val cats = o.optJSONArray("cats")?.let { a ->
                (0 until a.length()).map { i ->
                    val c = a.getJSONObject(i)
                    Cat(c.getString("id"), c.optString("name", "Lainnya"), c.optString("glyph", "dot"))
                }
            }?.takeIf { it.isNotEmpty() } ?: defaultCats()
            val goals = o.optJSONArray("goals")?.let { a ->
                (0 until a.length()).map { i ->
                    val g = a.getJSONObject(i)
                    Goal(g.getString("id"), g.optString("name", ""), g.optLong("target"))
                }
            } ?: emptyList()
            Saku(
                tx = tx,
                cats = cats,
                goals = goals,
                dark = o.optBoolean("dark", true),
                paletteId = o.optString("palette", "mono"),
                customAccent = o.optLong("accent", 0xFF23418F),
                customTone = o.optString("tone", "hijau"),
                chart = o.optString("chart", "batang"),
                months = if (o.optInt("months", 6) >= 12) 12 else 6,
                glass = o.optBoolean("glass", true)
            )
        } catch (e: Exception) {
            Saku()
        }
    }

    fun save(s: Saku) {
        val o = JSONObject()
        o.put("tx", JSONArray().apply {
            s.tx.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("date", t.date); put("time", t.time)
                    put("type", t.type); put("amount", t.amount); put("cat", t.cat); put("note", t.note)
                    put("goal", t.goal); put("photo", t.photo)
                })
            }
        })
        o.put("cats", JSONArray().apply {
            s.cats.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("glyph", c.glyph)
                })
            }
        })
        o.put("goals", JSONArray().apply {
            s.goals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id); put("name", g.name); put("target", g.target)
                })
            }
        })
        o.put("dark", s.dark)
        o.put("palette", s.paletteId)
        o.put("accent", s.customAccent)
        o.put("tone", s.customTone)
        o.put("chart", s.chart)
        o.put("months", s.months)
        o.put("glass", s.glass)
        prefs.edit().putString("db", o.toString()).apply()
    }
}

/** Aksi nabung yang memunculkan sheet dari level aplikasi. */
sealed interface GoalSheet {
    data class Setor(val goal: Goal) : GoalSheet
    data class Tarik(val goal: Goal) : GoalSheet
    data class Ubah(val goal: Goal) : GoalSheet
}

/** Setoran: transaksi keluar berkategori Nabung, jadi saldo akhir ikut turun. */
fun Saku.deposit(goal: Goal, amount: Long): Saku {
    val tx = Tx(
        id = "t" + System.nanoTime().toString(36),
        date = Fmt.iso(Date()),
        time = Fmt.nowTime(),
        type = "out",
        amount = amount,
        cat = CAT_NABUNG,
        note = "Setor · " + goal.name,
        goal = goal.id
    )
    val withCat = if (cats.none { it.id == CAT_NABUNG }) {
        copy(cats = cats + Cat(CAT_NABUNG, "Nabung", "wallet"))
    } else {
        this
    }
    return withCat.withTx(tx)
}

/** Penarikan: transaksi masuk, saldo akhir naik lagi. */
fun Saku.withdraw(goal: Goal, amount: Long): Saku = withTx(
    Tx(
        id = "t" + System.nanoTime().toString(36),
        date = Fmt.iso(Date()),
        time = Fmt.nowTime(),
        type = "in",
        amount = amount,
        cat = CAT_NABUNG,
        note = "Tarik · " + goal.name,
        goal = goal.id
    )
)
