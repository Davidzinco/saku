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
    val amount: Long,
    val cat: String,
    val note: String
)

data class Cat(val id: String, val name: String, val color: Long, val icon: String)

data class Goal(val id: String, val name: String, val target: Long, val saved: Long)

data class Saku(
    val tx: List<Tx> = emptyList(),
    val cats: List<Cat> = defaultCats(),
    val goals: List<Goal> = emptyList(),
    val dark: Boolean = true
)

fun defaultCats() = listOf(
    Cat("makan", "Makan", 0xFFFF7A6B, "restaurant"),
    Cat("transport", "Transport", 0xFFE0B341, "directions_car"),
    Cat("tagihan", "Tagihan", 0xFF5FB3E0, "home"),
    Cat("belanja", "Belanja", 0xFFA98BF5, "shopping_bag"),
    Cat("hiburan", "Hiburan", 0xFFE07BB0, "movie"),
    Cat("kesehatan", "Kesehatan", 0xFF57D9C0, "favorite"),
    Cat("gaji", "Gaji", 0xFF7BE495, "payments"),
    Cat("lain", "Lainnya", 0xFF8B9499, "more_horiz"),
)

fun Saku.monthOf(c: Calendar): List<Tx> {
    val y = c.get(Calendar.YEAR)
    val m = c.get(Calendar.MONTH)
    return tx.filter {
        val d = Fmt.parse(it.date).let { p -> Calendar.getInstance().apply { time = p } }
        d.get(Calendar.YEAR) == y && d.get(Calendar.MONTH) == m
    }
}

fun Saku.income(c: Calendar): Long = monthOf(c).filter { it.type == "in" }.sumOf { it.amount }
fun Saku.outcome(c: Calendar): Long = monthOf(c).filter { it.type == "out" }.sumOf { it.amount }
fun Saku.balance(): Long = tx.sumOf { if (it.type == "in") it.amount else -it.amount }

/** total pengeluaran n bulan terakhir, urut lama -> baru */
fun Saku.trend(n: Int): List<Pair<Calendar, Long>> {
    val out = mutableListOf<Pair<Calendar, Long>>()
    for (k in n - 1 downTo 0) {
        val c = Calendar.getInstance()
        c.add(Calendar.MONTH, -k)
        out += c to outcome(c)
    }
    return out
}

fun Saku.catOf(id: String): Cat? = cats.firstOrNull { it.id == id }

object Fmt {
    private val nf: NumberFormat = NumberFormat.getNumberInstance(Locale("in", "ID"))
    private val raw = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val human = SimpleDateFormat("d MMM yyyy", Locale("in", "ID"))
    private val monthYear = SimpleDateFormat("MMM yyyy", Locale("in", "ID"))

    fun rp(v: Long): String = "Rp " + nf.format(v)
    fun rpSigned(v: Long): String = (if (v < 0) "\u2212" else "") + "Rp " + nf.format(Math.abs(v))
    fun num(v: Long): String = nf.format(v)
    fun short(v: Long): String {
        val a = Math.abs(v)
        return when {
            a >= 1_000_000 -> String.format(Locale.US, "%.1f", v / 1_000_000.0).replace('.', ',') + "jt"
            a >= 1_000 -> Math.round(v / 1000.0).toString() + "rb"
            else -> v.toString()
        }
    }

    fun today(): Date = Date()
    fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())
    fun iso(d: Date): String = raw.format(d)
    fun parse(iso: String): Date = raw.parse(iso) ?: Date()
    fun human(iso: String): String = human.format(parse(iso))
    fun monthLabel(y: Int, m: Int): String {
        val c = Calendar.getInstance(Locale("in", "ID"))
        c.set(y, m, 1)
        return monthYear.format(c.time)
    }
    fun dayLabel(iso: String): String {
        val cal = Calendar.getInstance()
        val d = parse(iso)
        val todayIso = raw.format(Date())
        if (iso == todayIso) return "Hari ini"
        cal.time = Date(); cal.add(Calendar.DAY_OF_YEAR, -1)
        if (iso == raw.format(cal.time)) return "Kemarin"
        val h = SimpleDateFormat("EEE d MMM", Locale("in", "ID"))
        return h.format(d)
    }
}

class Store(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("saku", Context.MODE_PRIVATE)

    fun load(): Saku {
        val s = prefs.getString("db", null) ?: return seed()
        return try {
            val o = JSONObject(s)
            val tx = o.getJSONArray("tx").let { a ->
                (0 until a.length()).map { i ->
                    val t = a.getJSONObject(i)
                    Tx(t.getString("id"), t.getString("date"), t.optString("time", "12:00"),
                        t.getString("type"), t.getLong("amount"), t.getString("cat"), t.optString("note"))
                }
            }
            val goals = o.optJSONArray("goals")?.let { a ->
                (0 until a.length()).map { i ->
                    val g = a.getJSONObject(i)
                    Goal(g.getString("id"), g.getString("name"), g.getLong("target"), g.getLong("saved"))
                }
            } ?: emptyList()
            val cats = o.optJSONArray("cats")?.let { a ->
                (0 until a.length()).map { i ->
                    val c = a.getJSONObject(i)
                    Cat(c.getString("id"), c.getString("name"), c.getLong("color"), c.getString("icon"))
                }
            } ?: defaultCats()
            Saku(tx = tx, cats = cats, goals = goals, dark = o.optBoolean("dark", true))
        } catch (e: Exception) { seed() }
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
        o.put("goals", JSONArray().apply {
            s.goals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id); put("name", g.name); put("target", g.target); put("saved", g.saved)
                })
            }
        })
        o.put("cats", JSONArray().apply {
            s.cats.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("color", c.color); put("icon", c.icon)
                })
            }
        })
        o.put("dark", s.dark)
        prefs.edit().putString("db", o.toString()).apply()
    }

    fun seed(): Saku {
        val tx = mutableListOf<Tx>()
        val cal = Calendar.getInstance()
        val now = Date()
        repeat(6) { k ->
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -k)
            fun put(day: Int, type: String, amount: Long, cat: String, note: String, time: String) {
                val d = Calendar.getInstance()
                d.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), day, 12, 0)
                if (d.time.after(now)) return
                tx += Tx(System.nanoTime().toString(36) + day + k, Fmt.iso(d.time), time, type, amount, cat, note)
            }
            val f = listOf(1.0, 0.86, 1.28, 0.92, 1.14, 0.74)[k]
            put(2, "in", 6500000, "gaji", "Gaji bulanan", "08:05")
            put(3, "out", 1800000, "tagihan", "Kos", "09:15")
            put(4, "out", (380000 * f).toLong(), "makan", "Belanja mingguan", "17:40")
            put(6, "out", 320000, "transport", "Ojek & bensin", "07:20")
            put(9, "out", (420000 * f).toLong(), "makan", "Makan siang", "12:30")
            put(12, "out", (450000 * f).toLong(), "belanja", "Belanja bulanan", "16:10")
            put(14, "out", (260000 * f).toLong(), "makan", "Kopi & jajan", "15:45")
            put(18, "out", (180000 * f).toLong(), "hiburan", "Nonton", "19:20")
        }
        val s = Saku(
            tx = tx.sortedByDescending { it.date + it.time },
            goals = listOf(
                Goal("g1", "Dana darurat", 10_000_000, 3_200_000),
                Goal("g2", "Laptop baru", 12_000_000, 4_500_000)
            )
        )
        save(s)
        return s
    }
}
