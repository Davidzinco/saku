package id.saku.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    val goal: String = "",            // id target nabung, kalau transaksi ini setoran/penarikan
    val photo: String = "",           // V1.3, path tunggal — tetap dibaca untuk data lama
    val photos: List<String> = emptyList()   // V1.4 #7, beberapa lampiran
) {
    val signed: Long get() = if (type == "in") amount else -amount
    /** Satu sumber kebenaran untuk foto: data lama (photo) maupun baru (photos). */
    val allPhotos: List<String>
        get() = photos.ifEmpty { listOfNotNull(photo.takeIf { it.isNotEmpty() }) }
}

/** Kategori dibedakan lewat glyph, bukan warna (DESIGN.md § Color aturan 2). */
data class Cat(val id: String, val name: String, val glyph: String)

/**
 * V1.5 #5 — `image` adalah path absolut ke WebP di `filesDir/goals/`.
 * Field punya default "" supaya data V1.4 (dan backup lama) tetap terbaca.
 */
data class Goal(val id: String, val name: String, val target: Long, val image: String = "")

/**
 * Area yang bisa dipilih sendiri antara liquid glass dan kertas (V1.4 #8).
 *
 * PERBAIKAN CRASH 2026-11-18 — "Ringkasan Home" dikeluarkan dari daftar.
 * Panel itu berada DI DALAM subtree yang direkam oleh `sakuLayerBackdrop`.
 * `LayerBackdrop.drawBackdrop()` memanggil `drawLayer(graphicsLayer)` pada layer
 * yang sedang merekam subtree itu; kalau `drawBackdrop` ikut terekam, display
 * list memuat dirinya sendiri dan HWUI mengulang `RenderNode::prepareTreeImpl`
 * sampai stack RenderThread habis (SIGSEGV, bukan exception Kotlin).
 * Kaca hanya aman pada permukaan yang merupakan SAUDARA dari Box
 * `layerBackdrop`: Dock, FAB, dan Sheet.
 */
val glassAreas = listOf(
    "dock" to "Dock",
    "fab" to "Tombol catat",
    "sheet" to "Sheet"
)

private val glassKeys = glassAreas.map { it.first }.toSet()

/** Buang preferensi area yang tidak lagi didukung (mis. `"home"` dari V1.4 awal). */
fun Map<String, Boolean>.safeUiGlass(): Map<String, Boolean> = filterKeys { it in glassKeys }

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
    val glass: Boolean = true,      // master Kertas/Kaca (V1.3)
    val uiGlass: Map<String, Boolean> = emptyMap(),
    val showNabungHome: Boolean = true,
    val nabungScroll: Boolean = false,
    val nabungPinned: String = ""
)

/**
 * Default area kaca: dock + FAB menyala, sheet tetap kertas.
 * Hard guard: area di luar `glassKeys` (terutama `"home"`) selalu mati, apa pun
 * isi DataStore lama atau berkas backup — ini mencegah preferensi yang sudah
 * terlanjur tersimpan menyalakan lagi jalur yang bikin crash.
 */
fun Saku.glassPref(area: String): Boolean {
    if (area !in glassKeys) return false
    return uiGlass[area] ?: (area == "dock" || area == "fab")
}

fun Saku.glassOn(area: String): Boolean = glass && glassPref(area)

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

/**
 * V1.4 #1 — satu kali lintasan untuk seluruh batang, bukan `outcome()` per bulan
 * yang menyaring seluruh daftar transaksi berulang kali tiap kali jari bergerak.
 */
fun Saku.trendFast(cursor: Calendar): List<Pair<Calendar, Long>> {
    val totals = HashMap<String, Long>()
    tx.forEach { t ->
        if (t.type == "out" && t.date.length >= 7) {
            val k = t.date.substring(0, 7)
            totals[k] = (totals[k] ?: 0L) + t.amount
        }
    }
    return chartWindow(cursor).map { c ->
        val k = String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
        c to (totals[k] ?: 0L)
    }
}

fun Saku.trend(cursor: Calendar): List<Pair<Calendar, Long>> = trendFast(cursor)

fun sameMonth(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)

fun Saku.catOf(id: String): Cat? = cats.firstOrNull { it.id == id }
fun Saku.countFor(catId: String): Int = tx.count { it.cat == catId }

/** Saldo target nabung = setoran − penarikan, diturunkan dari transaksi (selalu sinkron dengan saldo). */
fun Saku.savedOf(goalId: String): Long = tx.filter { it.goal == goalId }.sumOf { -it.signed }

/**
 * V1.5 #5 — satu lintasan untuk semua target. Dipakai di halaman Nabung dan
 * ringkasan Home: kalau ada N target, `savedOf()` lama menyaring seluruh daftar
 * transaksi N kali. Di sini cukup sekali, lalu `remember(saku.tx)` di UI.
 */
fun Saku.savedMap(): Map<String, Long> {
    val result = HashMap<String, Long>()
    tx.forEach { t ->
        if (t.goal.isNotEmpty()) result[t.goal] = (result[t.goal] ?: 0L) - t.signed
    }
    return result
}

fun Saku.savedTotal(): Long {
    val ids = goals.mapTo(HashSet()) { it.id }
    return tx.sumOf { if (it.goal.isNotEmpty() && it.goal in ids) -it.signed else 0L }
}
fun Saku.goalOf(id: String): Goal? = goals.firstOrNull { it.id == id }

/** V1.5 #5 — riwayat setoran/penarikan satu target, terbaru dulu. */
fun Saku.goalHistory(goalId: String): List<Tx> = tx.filter { it.goal == goalId }
    .sortedWith(compareByDescending<Tx> { it.date }.thenByDescending { it.time })

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
    private val dayWithYear = SimpleDateFormat("EEE d MMM yyyy", ID)
    private val weekdayFmt = SimpleDateFormat("EEEE", ID)

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

    /** "Sen 16 Nov 2026 · 07:30" untuk riwayat nabung (V1.5 #5). */
    fun dateTime(dateIso: String, time: String): String =
        dayWithYear.format(parse(dateIso)) + " · " + time

    /** "Kamis" untuk baris detail transaksi (V1.5 #4). */
    fun weekday(dateIso: String): String = weekdayFmt.format(parse(dateIso))

    fun nowTime(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())

    /** Versi aplikasi dibaca dari PackageManager, bukan hardcode di UI. */
    fun appVersion(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.5"
    } catch (e: Exception) {
        "1.5"
    }

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

    fun stamp(): String = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
}

/* ---------------------------------------------------------------------------
 * Serialisasi JSON portabel: dipakai migrasi dari SharedPreferences, backup ZIP,
 * dan impor. Formatnya sengaja tetap kompatibel dengan berkas V1.3.
 * ------------------------------------------------------------------------- */

fun encodeTx(t: Tx): String = JSONObject().apply {
    put("id", t.id); put("date", t.date); put("time", t.time)
    put("type", t.type); put("amount", t.amount); put("cat", t.cat)
    put("note", t.note); put("goal", t.goal)
    if (t.photo.isNotEmpty()) put("photo", t.photo)
    put("photos", JSONArray().apply { t.allPhotos.forEach { put(it) } })
}.toString()

fun decodeTx(raw: String): Tx = JSONObject(raw).let { t ->
    Tx(
        id = t.getString("id"),
        date = t.optString("date", Fmt.iso(Date())),
        time = t.optString("time", "12:00"),
        type = t.optString("type", "out"),
        amount = t.optLong("amount", 0L),
        cat = t.optString("cat", "lain"),
        note = t.optString("note", ""),
        goal = t.optString("goal", ""),
        photo = t.optString("photo", ""),
        photos = t.optJSONArray("photos")?.let { a ->
            (0 until a.length()).map { a.optString(it, "") }.filter { it.isNotEmpty() }
        } ?: emptyList()
    )
}

private fun encodeCats(list: List<Cat>): String = JSONArray().apply {
    list.forEach { put(JSONObject().apply { put("id", it.id); put("name", it.name); put("glyph", it.glyph) }) }
}.toString()

private fun decodeCats(raw: String?): List<Cat> = try {
    if (raw.isNullOrBlank()) defaultCats() else JSONArray(raw).let { a ->
        (0 until a.length()).map { i ->
            val c = a.getJSONObject(i)
            Cat(c.getString("id"), c.optString("name", "Lainnya"), c.optString("glyph", "dot"))
        }
    }.takeIf { it.isNotEmpty() } ?: defaultCats()
} catch (e: Exception) {
    defaultCats()
}

private fun encodeGoals(list: List<Goal>): String = JSONArray().apply {
    list.forEach {
        put(JSONObject().apply {
            put("id", it.id); put("name", it.name); put("target", it.target)
            // V1.5 #5: path gambar target ikut tersimpan (absolut di perangkat,
            // dibuat relatif oleh Backup.portableJson saat ekspor).
            if (it.image.isNotEmpty()) put("image", it.image)
        })
    }
}.toString()

private fun decodeGoals(raw: String?): List<Goal> = try {
    if (raw.isNullOrBlank()) emptyList() else JSONArray(raw).let { a ->
        (0 until a.length()).mapNotNull { i ->
            runCatching {
                val g = a.getJSONObject(i)
                Goal(
                    g.getString("id"),
                    g.optString("name", ""),
                    g.optLong("target"),
                    g.optString("image", "")
                )
            }.getOrNull()
        }
    }
} catch (e: Exception) {
    emptyList()
}

fun encodeSaku(s: Saku): String = JSONObject().apply {
    put("v", 2)
    put("tx", JSONArray().apply { s.tx.forEach { put(JSONObject(encodeTx(it))) } })
    put("cats", JSONArray(encodeCats(s.cats)))
    put("goals", JSONArray(encodeGoals(s.goals)))
    put("dark", s.dark)
    put("palette", s.paletteId)
    put("accent", s.customAccent)
    put("tone", s.customTone)
    put("chart", s.chart)
    put("months", s.months)
    put("glass", s.glass)
    put("ui", JSONObject().apply { s.uiGlass.forEach { (k, v) -> put(k, v) } })
    put("showNabungHome", s.showNabungHome)
    put("nabungScroll", s.nabungScroll)
    put("nabungPinned", s.nabungPinned)
}.toString()

fun decodeSaku(raw: String): Saku = try {
    val o = JSONObject(raw)
    val tx = o.optJSONArray("tx")?.let { a ->
        (0 until a.length()).mapNotNull { i -> runCatching { decodeTx(a.getJSONObject(i).toString()) }.getOrNull() }
    }?.sortedWith(compareByDescending<Tx> { it.date }.thenByDescending { it.time }) ?: emptyList()
    val ui = o.optJSONObject("ui")?.let { u ->
        u.keys().asSequence().associateWith { u.optBoolean(it, false) }
    } ?: emptyMap()
    Saku(
        tx = tx,
        cats = decodeCats(o.optJSONArray("cats")?.toString()),
        goals = decodeGoals(o.optJSONArray("goals")?.toString()),
        dark = o.optBoolean("dark", true),
        paletteId = o.optString("palette", "mono"),
        customAccent = o.optLong("accent", 0xFF23418F),
        customTone = o.optString("tone", "hijau"),
        chart = o.optString("chart", "batang"),
        months = if (o.optInt("months", 6) >= 12) 12 else 6,
        glass = o.optBoolean("glass", true),
        uiGlass = ui.safeUiGlass(),
        showNabungHome = o.optBoolean("showNabungHome", true),
        nabungScroll = o.optBoolean("nabungScroll", false),
        nabungPinned = o.optString("nabungPinned", "")
    )
} catch (e: Exception) {
    Saku()
}

/** Validasi ringan sebelum hasil impor dipakai (V1.4 #3). */
fun Saku.isSane(): Boolean =
    tx.size <= 100_000 && cats.isNotEmpty() && tx.all {
        it.id.isNotBlank() && it.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
            it.time.matches(Regex("\\d{2}:\\d{2}")) && it.amount >= 0L
    }

/* ---------------------------------------------------------------------------
 * V1.4 #5 — DataStore, bukan lagi satu string JSON di SharedPreferences.
 * Tiap transaksi punya key sendiri, jadi `save()` hanya menulis record yang
 * berubah. Data lama dimigrasikan sekali saat `load()` pertama kali.
 * ------------------------------------------------------------------------- */

private val Context.sakuDataStore: DataStore<Preferences> by preferencesDataStore(name = "saku_v2")

private val K_IDS = stringSetPreferencesKey("tx_ids")
private val K_CATS = stringPreferencesKey("cats")
private val K_GOALS = stringPreferencesKey("goals")
private val K_DARK = booleanPreferencesKey("dark")
private val K_PALETTE = stringPreferencesKey("palette")
private val K_ACCENT = longPreferencesKey("accent")
private val K_TONE = stringPreferencesKey("tone")
private val K_CHART = stringPreferencesKey("chart")
private val K_MONTHS = intPreferencesKey("months")
private val K_GLASS = booleanPreferencesKey("glass")
private val K_UI = stringPreferencesKey("ui")
private val K_SHOW_NABUNG = booleanPreferencesKey("showNabungHome")
private val K_NABUNG_SCROLL = booleanPreferencesKey("nabungScroll")
private val K_NABUNG_PINNED = stringPreferencesKey("nabungPinned")

private fun txKey(id: String) = stringPreferencesKey("tx_" + id)

private fun encodeUiGlass(s: Saku): String =
    JSONObject().apply { s.uiGlass.safeUiGlass().forEach { (k, v) -> put(k, v) } }.toString()

private fun writeAllInto(p: MutablePreferences, s: Saku) {
    s.tx.forEach { p[txKey(it.id)] = encodeTx(it) }
    p[K_IDS] = s.tx.map { it.id }.toSet()
    p[K_CATS] = encodeCats(s.cats)
    p[K_GOALS] = encodeGoals(s.goals)
    p[K_DARK] = s.dark
    p[K_PALETTE] = s.paletteId
    p[K_ACCENT] = s.customAccent
    p[K_TONE] = s.customTone
    p[K_CHART] = s.chart
    p[K_MONTHS] = s.months
    p[K_GLASS] = s.glass
    p[K_UI] = encodeUiGlass(s)
    p[K_SHOW_NABUNG] = s.showNabungHome
    p[K_NABUNG_SCROLL] = s.nabungScroll
    p[K_NABUNG_PINNED] = s.nabungPinned
}

private fun decodePrefs(p: Preferences): Saku {
    val ids = p[K_IDS] ?: emptySet()
    val tx = ids.mapNotNull { id ->
        p[txKey(id)]?.let { runCatching { decodeTx(it) }.getOrNull() }
    }.sortedWith(compareByDescending<Tx> { it.date }.thenByDescending { it.time })
    val ui = try {
        JSONObject(p[K_UI] ?: "{}").let { u ->
            u.keys().asSequence().associateWith { u.optBoolean(it, false) }
        }
    } catch (e: Exception) {
        emptyMap()
    }
    return Saku(
        tx = tx,
        cats = decodeCats(p[K_CATS]),
        goals = decodeGoals(p[K_GOALS]),
        dark = p[K_DARK] ?: true,
        paletteId = p[K_PALETTE] ?: "mono",
        customAccent = p[K_ACCENT] ?: 0xFF23418F,
        customTone = p[K_TONE] ?: "hijau",
        chart = p[K_CHART] ?: "batang",
        months = if ((p[K_MONTHS] ?: 6) >= 12) 12 else 6,
        glass = p[K_GLASS] ?: true,
        uiGlass = ui.safeUiGlass(),
        showNabungHome = p[K_SHOW_NABUNG] ?: true,
        nabungScroll = p[K_NABUNG_SCROLL] ?: false,
        nabungPinned = p[K_NABUNG_PINNED] ?: ""
    )
}

class Store(private val ctx: Context) {
    private val ds: DataStore<Preferences> = ctx.sakuDataStore
    private val legacy = ctx.getSharedPreferences("saku", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var snapshot: Saku = Saku()

    /** Aplikasi mulai dari nol — tidak ada data contoh yang ditanam. */
    fun load(): Saku {
        // Simpan nilai MENTAH K_UI sebelum decodePrefs menyaringnya, supaya
        // perbandingan di bawah benar-benar mendeteksi kunci terlarang di disk.
        var storedUi: String? = null
        val decoded = try {
            val p = runBlocking(Dispatchers.IO) { ds.data.first() }
            storedUi = p[K_UI]
            if (p.asMap().isEmpty()) {
                val raw = legacy.getString("db", null)
                val migrated = if (raw.isNullOrBlank()) Saku() else decodeSaku(raw)
                runBlocking(Dispatchers.IO) { ds.edit { writeAllInto(it, migrated) } }
                legacy.edit().remove("db").apply()
                migrated
            } else {
                decodePrefs(p)
            }
        } catch (e: Exception) {
            val raw = legacy.getString("db", null)
            if (raw.isNullOrBlank()) Saku() else decodeSaku(raw)
        }
        val s = decoded.copy(uiGlass = decoded.uiGlass.safeUiGlass())
        // Bersihkan DataStore yang masih menyimpan area terlarang ("home").
        // Perangkat yang sudah telanjur memilih Kaca pada Ringkasan Home tidak
        // boleh bisa menyalakan lagi jalur rekursif itu setelah update.
        val cleanUi = encodeUiGlass(s)
        if (storedUi != null && storedUi != cleanUi) {
            scope.launch {
                try {
                    ds.edit { p -> p[K_UI] = cleanUi }
                } catch (e: Exception) {
                    // IO gagal tidak boleh menjatuhkan UI; guard glassPref tetap berlaku.
                }
            }
        }
        snapshot = s
        return s
    }

    /** Menulis cuma record yang berubah — tidak lagi seluruh database. */
    fun save(s: Saku) {
        val prev = snapshot
        snapshot = s
        scope.launch {
            try {
                ds.edit { p -> applyDiff(p, prev, s) }
            } catch (e: Exception) {
                // Jangan biarkan kegagalan IO menjatuhkan UI; state di memori tetap benar.
            }
        }
    }

    /** Dipakai setelah restore: paksa tulis ulang seluruh key. */
    fun replace(s: Saku) {
        snapshot = s
        scope.launch {
            try {
                ds.edit { p -> writeAllInto(p, s) }
            } catch (e: Exception) {
            }
        }
    }

    private fun applyDiff(p: MutablePreferences, prev: Saku, s: Saku) {
        if (prev.tx != s.tx) {
            val oldIds = prev.tx.map { it.id }.toSet()
            val newIds = s.tx.map { it.id }.toSet()
            (oldIds - newIds).forEach { p.remove(txKey(it)) }
            val oldMap = prev.tx.associateBy { it.id }
            s.tx.forEach { t -> if (oldMap[t.id] != t) p[txKey(t.id)] = encodeTx(t) }
            p[K_IDS] = newIds
        }
        if (prev.cats != s.cats) p[K_CATS] = encodeCats(s.cats)
        if (prev.goals != s.goals) p[K_GOALS] = encodeGoals(s.goals)
        if (prev.dark != s.dark) p[K_DARK] = s.dark
        if (prev.paletteId != s.paletteId) p[K_PALETTE] = s.paletteId
        if (prev.customAccent != s.customAccent) p[K_ACCENT] = s.customAccent
        if (prev.customTone != s.customTone) p[K_TONE] = s.customTone
        if (prev.chart != s.chart) p[K_CHART] = s.chart
        if (prev.months != s.months) p[K_MONTHS] = s.months
        if (prev.glass != s.glass) p[K_GLASS] = s.glass
        if (prev.uiGlass != s.uiGlass) {
            p[K_UI] = encodeUiGlass(s)
        }
        if (prev.showNabungHome != s.showNabungHome) p[K_SHOW_NABUNG] = s.showNabungHome
        if (prev.nabungScroll != s.nabungScroll) p[K_NABUNG_SCROLL] = s.nabungScroll
        if (prev.nabungPinned != s.nabungPinned) p[K_NABUNG_PINNED] = s.nabungPinned
    }
}

/** Aksi nabung yang memunculkan sheet dari level aplikasi. */
sealed interface GoalSheet {
    data class Setor(val goal: Goal) : GoalSheet
    data class Tarik(val goal: Goal) : GoalSheet
    data class Ubah(val goal: Goal) : GoalSheet
    /** V1.5 #5: ketuk kartu target membuka detail + riwayat menabung. */
    data class Detail(val goal: Goal) : GoalSheet
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
