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
    val note: String
) {
    val signed: Long get() = if (type == "in") amount else -amount
}

/** Kategori dibedakan lewat glyph, bukan warna (DESIGN.md § Color aturan 2). */
data class Cat(val id: String, val name: String, val glyph: String)

data class Goal(val id: String, val name: String, val target: Long, val saved: Long)

data class Saku(
    val tx: List<Tx> = emptyList(),
    val cats: List<Cat> = defaultCats(),
    val goals: List<Goal> = emptyList(),
    val dark: Boolean = false
)

val glyphChoices = listOf("food", "car", "home", "bag", "film", "heart", "money", "dot")

fun defaultCats() = listOf(
    Cat("makan", "Makan", "food"),
    Cat("transport", "Transport", "car"),
    Cat("tagihan", "Tagihan", "home"),
    Cat("belanja", "Belanja", "bag"),
    Cat("hiburan", "Hiburan", "film"),
    Cat("kesehatan", "Kesehatan", "heart"),
    Cat("gaji", "Gaji", "money"),
    Cat("lain", "Lainnya", "dot")
)

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

/** Pengeluaran bulan `c` per kategori, terbesar dulu. */
fun Saku.spendingByCat(c: Calendar): List<Pair<Cat, Long>> = monthTxs(c)
    .filter { it.type == "out" }
    .groupBy { it.cat }
    .map { (id, list) -> (catOf(id) ?: Cat(id, "Lainnya", "dot")) to list.sumOf { it.amount } }
    .filter { it.second > 0 }
    .sortedByDescending { it.second }

/** Total pengeluaran n bulan terakhir yang berakhir di bulan `cursor`, urut lama → baru. */
fun Saku.trend(n: Int, cursor: Calendar): List<Pair<Calendar, Long>> {
    val out = mutableListOf<Pair<Calendar, Long>>()
    for (k in n - 1 downTo 0) {
        val c = (cursor.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -k)
        }
        out += c to outcome(c)
    }
    return out
}

fun Saku.catOf(id: String): Cat? = cats.firstOrNull { it.id == id }
fun Saku.countFor(catId: String): Int = tx.count { it.cat == catId }

object Fmt {
    private val ID: Locale = Locale.forLanguageTag("id-ID")
    private val nf: NumberFormat = NumberFormat.getIntegerInstance(ID)
    private val raw = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthYear = SimpleDateFormat("MMMM yyyy", ID)
    private val monthAlone = SimpleDateFormat("MMMM", ID)
    private val monthAbbr = SimpleDateFormat("MMM", ID)
    private val dayHuman = SimpleDateFormat("EEE d MMM", ID)

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

    fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())
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
                        note = t.optString("note", "")
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
                    Goal(g.getString("id"), g.optString("name", ""), g.optLong("target"), g.optLong("saved"))
                }
            } ?: emptyList()
            Saku(tx = tx, cats = cats, goals = goals, dark = o.optBoolean("dark", false))
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
                    put("id", g.id); put("name", g.name); put("target", g.target); put("saved", g.saved)
                })
            }
        })
        o.put("dark", s.dark)
        prefs.edit().putString("db", o.toString()).apply()
    }
}
