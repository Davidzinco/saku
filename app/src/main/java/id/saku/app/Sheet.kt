package id.saku.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Data awal opsional dari notifikasi e-wallet atau pintasan widget (V1.6.8 #3). */
data class TxPrefill(
    val type: String = "out",
    val amount: Long = 0L,
    val note: String = "",
    val accountId: String = ""
)

/**
 * Sheet catat/ubah transaksi: nominal berformat, sumber dana / pindah saldo,
 * Date, Time (dial radial 2 cincin 1-24 + AM/PM di kepala), foto pada note,
 * dan hapus dua ketuk.
 */
@Composable
fun TxSheet(
    t: Tok,
    cats: List<Cat>,
    accounts: List<Account>,
    multiAccount: Boolean,
    initial: Tx?,
    prefill: TxPrefill? = null,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (Tx) -> Unit,
    onDelete: (String) -> Unit
) {
    val defaultAcc = accounts.firstOrNull()?.id ?: "tunai"
    val secondAcc = accounts.getOrNull(1)?.id ?: defaultAcc
    var type by remember { mutableStateOf(initial?.type ?: prefill?.type ?: "out") }
    var digits by remember {
        val initAmt = initial?.amount ?: prefill?.amount?.takeIf { it > 0L }
        mutableStateOf(initAmt?.toString() ?: "")
    }
    var cat by remember {
        val initType = initial?.type ?: prefill?.type ?: "out"
        val initialList = catsFor(initType, cats)
        mutableStateOf(initialList.firstOrNull { it.id == initial?.cat }?.id ?: initialList.first().id)
    }
    var accountId by remember {
        val initAcc = initial?.account?.takeIf { a -> accounts.any { it.id == a } }
            ?: prefill?.accountId?.takeIf { a -> accounts.any { it.id == a } }
            ?: defaultAcc
        mutableStateOf(initAcc)
    }
    var toAccountId by remember {
        val initTo = initial?.toAccount?.takeIf { a -> accounts.any { it.id == a } && a != accountId }
            ?: accounts.firstOrNull { it.id != accountId }?.id
            ?: secondAcc
        mutableStateOf(initTo)
    }
    var note by remember { mutableStateOf(initial?.note ?: prefill?.note ?: "") }
    var dateIso by remember { mutableStateOf(initial?.date ?: Fmt.iso(Date())) }
    val initialTime = remember { initial?.time ?: Fmt.nowTime() }
    var hour by remember { mutableStateOf(initialTime.substringBefore(":").toIntOrNull() ?: 12) }
    var minute by remember { mutableStateOf(initialTime.substringAfter(":").toIntOrNull() ?: 0) }
    var calOpen by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // V1.4 #7: beberapa lampiran, maksimal 6 per transaksi.
    var photos by remember { mutableStateOf(initial?.allPhotos ?: emptyList<String>()) }
    val picker = rememberPhotoActions { path ->
        if (photos.size < 6) photos = photos + path
    }

    val focus = LocalFocusManager.current
    val amount = digits.toLongOrNull() ?: 0L
    val isTransfer = type == "transfer"
    val invalid = amount <= 0L || (isTransfer && accountId == toAccountId)
    val visibleCats = catsFor(type, cats)
    val editing = initial != null
    val canMulti = multiAccount && accounts.size >= 2

    SheetScaffold(
        t = t,
        onDismiss = onDismiss,
        visible = visible,
        footer = {
            PrimaryButton(
                label = when {
                    editing -> "Simpan perubahan"
                    isTransfer -> "Simpan pindah saldo"
                    else -> "Simpan transaksi"
                },
                t = t,
                enabled = !invalid,
                onDisabledClick = { touched = true }
            ) {
                onSave(
                    Tx(
                        id = initial?.id ?: ("t" + System.nanoTime().toString(36)),
                        date = dateIso,
                        time = pad2(hour) + ":" + pad2(minute),
                        type = type,
                        amount = amount,
                        cat = if (isTransfer) "lain" else cat,
                        note = note.trim().ifBlank {
                            if (isTransfer) {
                                val fromName = accounts.firstOrNull { it.id == accountId }?.name ?: "Asal"
                                val toName = accounts.firstOrNull { it.id == toAccountId }?.name ?: "Tujuan"
                                "Pindah $fromName \u2192 $toName"
                            } else ""
                        },
                        goal = initial?.goal ?: "",
                        photos = photos,
                        account = accountId,
                        toAccount = if (isTransfer) toAccountId else ""
                    )
                )
            }
            if (editing) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    label = if (confirmDelete) "Yakin hapus transaksi ini?" else "Hapus transaksi",
                    t = t
                ) {
                    if (confirmDelete) onDelete(initial.id) else confirmDelete = true
                }
            }
        }
    ) {
        SheetTitle(t, if (editing) "Ubah transaksi" else "Catat transaksi", onDismiss)
        Spacer(Modifier.height(14.dp))

        // Jenis (Keluar / Masuk / Pindah bila multi sumber dana aktif)
        val typeChoices = if (canMulti) {
            listOf("out" to "Keluar", "in" to "Masuk", "transfer" to "Pindah")
        } else {
            listOf("out" to "Keluar", "in" to "Masuk")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            typeChoices.forEach { (value, label) ->
                val on = type == value
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) t.accentWash else Color.Transparent)
                        .border(1.dp, if (on) t.accent else t.hair, RoundedCornerShape(10.dp))
                        .tap {
                            focus.clearFocus()
                            type = value
                            if (value != "transfer") {
                                val list = catsFor(value, cats)
                                if (list.none { it.id == cat }) cat = list.first().id
                            } else if (toAccountId == accountId) {
                                toAccountId = accounts.firstOrNull { it.id != accountId }?.id ?: accountId
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        Type.label.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                        if (on) t.accent else t.muted
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))

        // Nominal
        Text("Nominal", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        FieldShell(t, error = touched && amount <= 0L, height = 68.dp) {
            Text("Rp", Type.currency, t.muted2)
            Spacer(Modifier.width(8.dp))
            AmountField(
                digits = digits,
                onDigits = { digits = it },
                t = t,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onFocus = { focused ->
                    if (wasFocused && !focused) touched = true
                    wasFocused = focused
                }
            )
        }
        if (touched && amount <= 0L) {
            Text("Nominal harus lebih dari nol.", Type.label, t.error, Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(16.dp))

        // Sumber dana (V1.6.8 #1)
        if (canMulti) {
            Text(if (isTransfer) "Dari sumber dana" else "Sumber dana", Type.label, t.muted2)
            Spacer(Modifier.height(8.dp))
            AccountChips(t, accounts, accountId) { picked ->
                focus.clearFocus()
                accountId = picked
                if (isTransfer && toAccountId == picked) {
                    toAccountId = accounts.firstOrNull { it.id != picked }?.id ?: picked
                }
            }
            if (isTransfer) {
                Spacer(Modifier.height(12.dp))
                Text("Ke sumber dana", Type.label, t.muted2)
                Spacer(Modifier.height(8.dp))
                AccountChips(t, accounts.filter { it.id != accountId }, toAccountId) { picked ->
                    focus.clearFocus()
                    toAccountId = picked
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Kategori (tidak perlu untuk pindah saldo antar sumber dana)
        if (!isTransfer) {
            Text("Kategori", Type.label, t.muted2)
            Spacer(Modifier.height(8.dp))
            CatChips(t, visibleCats, cat) {
                focus.clearFocus()
                cat = it
            }
            Spacer(Modifier.height(16.dp))
        }

        // Catatan
        Text("Catatan", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        FieldShell(t) {
            PlainField(
                value = note,
                onValueChange = { note = it.take(60) },
                placeholder = "mis. Makan siang",
                t = t,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            // V1.4 #7/#9: aksi foto jadi ikon di dalam Note, tanpa label dan
            // tanpa teks "Opsional. Lampirkan struk …".
            PhotoNoteIcons(t, picker)
        }
        if (photos.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            PhotoStrip(t, photos) { path ->
                deletePhoto(path)
                photos = photos - path
            }
        }
        Spacer(Modifier.height(16.dp))

        // Tanggal
        Text("Tanggal", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        FieldShell(t) {
            Box(
                Modifier.weight(1f).fillMaxHeight().tap {
                    focus.clearFocus()
                    calOpen = !calOpen
                },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(Fmt.dayLabel(dateIso) + " \u00b7 " + Fmt.tgl(dateIso), Type.body, t.ink)
            }
            TouchTarget {
                Box(
                    Modifier.fillMaxSize().tap {
                        focus.clearFocus()
                        calOpen = !calOpen
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Glyph(if (calOpen) Ic.Up else Ic.Down, t.muted2, 16.dp)
                }
            }
        }

        if (calOpen) {
            Spacer(Modifier.height(8.dp))
            CalendarBox(t, dateIso) {
                dateIso = it
                calOpen = false
            }
        }

        Spacer(Modifier.height(16.dp))

        // V1.6 #2 — Time Input: field ringkas berikon jam + dial radial dua
        // langkah, mengikuti semangat ringkas `design/time.webp`.
        TimeInput(
            t = t,
            hour = hour,
            minute = minute,
            onTime = { h, m -> hour = h; minute = m },
            onClear = {
                val now = Fmt.nowTime()
                hour = now.substringBefore(":").toIntOrNull() ?: 12
                minute = now.substringAfter(":").toIntOrNull() ?: 0
            }
        )

        if (initial?.goal?.isNotEmpty() == true) {
            Text(
                "Transaksi ini setoran nabung — menghapusnya ikut mengurangi saldo target.",
                Type.micro,
                t.muted2,
                Modifier.padding(vertical = 6.dp),
                maxLines = 3
            )
        }
    }
}

private fun pad2(v: Int): String = String.format(Locale.US, "%02d", v)

private fun dayPart(hour: Int): String = when {
    hour < 11 -> "pagi"
    hour < 15 -> "siang"
    hour < 19 -> "sore"
    else -> "malam"
}

/** Kategori yang masuk akal untuk jenis transaksi yang dipilih. */
private fun catsFor(type: String, cats: List<Cat>): List<Cat> = when (type) {
    "in" -> cats.filter { it.id == "gaji" || it.id == "lain" }.ifEmpty { cats }
    else -> cats.filter { it.id != "gaji" }.ifEmpty { cats }
}

@Composable
private fun AccountChips(
    t: Tok,
    accounts: List<Account>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        accounts.forEach { acc ->
            val on = acc.id == selected
            Row(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) t.accentWash else Color.Transparent)
                    .border(1.dp, if (on) t.accent else t.hair, RoundedCornerShape(10.dp))
                    .tap { onSelect(acc.id) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Glyph(glyphOf(acc.glyph), if (on) t.accent else t.muted, 15.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    acc.name,
                    Type.label.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                    if (on) t.accent else t.muted
                )
            }
        }
    }
}

@Composable
private fun CatChips(t: Tok, cats: List<Cat>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cats.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cat ->
                    val on = cat.id == selected
                    Row(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) t.accentWash else Color.Transparent)
                            .border(1.dp, if (on) t.accent else t.hair, RoundedCornerShape(10.dp))
                            .tap { onSelect(cat.id) }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Glyph(glyphOf(cat.glyph), if (on) t.accent else t.muted, 15.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            cat.name,
                            Type.label.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                            if (on) t.accent else t.muted,
                            Modifier.weight(1f)
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Kalender grid 7 kolom, Senin di kolom pertama. */
@Composable
fun CalendarBox(t: Tok, selected: String, onPick: (String) -> Unit) {
    val base = remember { Calendar.getInstance().apply { time = Fmt.parse(selected) } }
    var year by remember { mutableStateOf(base.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(base.get(Calendar.MONTH)) }

    val first = remember(year, month) {
        Calendar.getInstance().apply {
            set(year, month, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val startDow = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val todayIso = Fmt.iso(Date())

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, t.hair, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TouchTarget {
                Box(
                    Modifier.fillMaxSize().tap {
                        if (month == 0) {
                            month = 11
                            year -= 1
                        } else {
                            month -= 1
                        }
                    },
                    contentAlignment = Alignment.Center
                ) { Glyph(Ic.ChevL, t.muted2, 14.dp) }
            }
            Text(Fmt.monthLabel(first), Type.label, t.ink, Modifier.weight(1f), align = TextAlign.Center)
            TouchTarget {
                Box(
                    Modifier.fillMaxSize().tap {
                        if (month == 11) {
                            month = 0
                            year += 1
                        } else {
                            month += 1
                        }
                    },
                    contentAlignment = Alignment.Center
                ) { Glyph(Ic.ChevR, t.muted2, 14.dp) }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach {
                Text(it, Type.micro, t.muted2, Modifier.weight(1f), align = TextAlign.Center)
            }
        }

        val rows = (startDow + days + 6) / 7
        Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val day = r * 7 + c - startDow + 1
                        Box(Modifier.weight(1f).height(34.dp), contentAlignment = Alignment.Center) {
                            if (day in 1..days) {
                                val cal = remember(year, month, day) {
                                    Calendar.getInstance().apply {
                                        set(year, month, day, 12, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                }
                                val iso = Fmt.iso(cal.time)
                                val on = iso == selected
                                val isToday = iso == todayIso
                                Box(
                                    Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (on) t.accent else Color.Transparent)
                                        .then(
                                            if (isToday && !on) Modifier.border(1.dp, t.hair, CircleShape) else Modifier
                                        )
                                        .tap { onPick(iso) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        day.toString(),
                                        Type.label,
                                        if (on) (if (t.dark) t.bg else Color.White) else t.ink
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Hari ini",
                Type.label.copy(fontWeight = FontWeight.SemiBold),
                t.accent,
                Modifier.padding(vertical = 6.dp).tap { onPick(todayIso) }
            )
            Text(
                "Kemarin",
                Type.label,
                t.muted2,
                Modifier.padding(vertical = 6.dp).tap {
                    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    onPick(Fmt.iso(c.time))
                }
            )
        }
    }
}

/**
 * V1.6 #2 — Time Input: satu dial radial, dua langkah (jam -> menit), preset.
 *
 * Teori yang dipakai:
 * - Hick: hanya 12 pilihan tampil per langkah, bukan 36 chip sekaligus (V1.5).
 * - Fitts: target radial ~44dp, jarak antar pusat sel >= lebar sel, dan semua
 *   target berkumpul dekat ibu jari, bukan tersebar di pojok-pojok layar.
 * - Recognition over recall: posisi jam memakai model jam analog yang sudah
 *   dikenal; menit memakai kelipatan 5 pada posisi jarum yang sama.
 * - Progressive disclosure: ketuk jam -> lanjut ke menit; ketuk menit ->
 *   selesai dan popover tutup. Waktu penuh cukup dua ketuk.
 * - Fine tuning: +/- 1 pada segmen aktif menjaga jam ganjil dan menit tepat,
 *   jadi presisi V1.5 tidak hilang.
 * - Tidak ada keyboard/IME, tidak ada LazyColumn/scroll bersarang, tidak ada
 *   auto-scroll. Tinggi popover tetap saat segmen berganti.
 */
@Composable
private fun TimeInput(
    t: Tok,
    hour: Int,
    minute: Int,
    onTime: (Int, Int) -> Unit,
    onClear: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var use12 by remember { mutableStateOf(false) }
    // 0 = jam, 1 = menit. Popover selalu dibuka dari langkah jam.
    var step by remember { mutableIntStateOf(0) }

    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val isAm = hour < 12
    val shown = if (use12) {
        pad2(h12) + ":" + pad2(minute) + if (isAm) " AM" else " PM"
    } else {
        pad2(hour) + ":" + pad2(minute)
    }

    Text("Waktu", Type.label, t.muted2)
    Spacer(Modifier.height(6.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (expanded) t.accent.copy(alpha = 0.45f) else t.hair,
                RoundedCornerShape(10.dp)
            )
            .background(t.bg)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Area ketuk utama selebar mungkin; format dipisah supaya tidak ada
        // clickable bersarang yang bisa memicu dua aksi sekaligus.
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .pressable(scale = 0.99f, alpha = 0.96f) {
                    expanded = !expanded
                    if (expanded) step = 0
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Glyph(Ic.Clock, if (expanded) t.accent else t.muted2, 18.dp)
            Spacer(Modifier.width(11.dp))
            Text(
                shown,
                Type.title.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                t.ink,
                Modifier.weight(1f)
            )
            Glyph(if (expanded) Ic.Up else Ic.Down, t.faint, 14.dp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (use12) "12j" else "24j",
            Type.micro.copy(fontWeight = FontWeight.SemiBold),
            t.accent,
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(t.accentWash)
                .tap { use12 = !use12 }
                .padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }

    if (!expanded) return

    Spacer(Modifier.height(8.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, t.hairSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FineButton(t, Ic.Minus) {
                if (step == 0) onTime((hour + 23) % 24, minute)
                else onTime(hour, (minute + 59) % 60)
            }
            Spacer(Modifier.width(4.dp))
            TimeSegment(
                t,
                if (use12) pad2(h12) else if (hour == 0) "24" else pad2(hour),
                step == 0
            ) { step = 0 }
            Text(":", Type.title.copy(fontSize = 20.sp), t.muted2)
            TimeSegment(t, pad2(minute), step == 1) { step = 1 }
            Spacer(Modifier.width(4.dp))
            FineButton(t, Ic.Plus) {
                if (step == 0) onTime((hour + 1) % 24, minute)
                else onTime(hour, (minute + 1) % 60)
            }
            Spacer(Modifier.weight(1f))
            if (!use12) {
                Text(
                    "Sekarang",
                    Type.micro.copy(fontWeight = FontWeight.SemiBold),
                    t.accent,
                    Modifier.padding(vertical = 8.dp, horizontal = 4.dp).tap {
                        onClear()
                        expanded = false
                    }
                )
            }
        }

        // V1.6.8 M3: pisahkan baris AM/PM + Sekarang agar tidak terpotong di lebar 360dp.
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (step == 0) (if (use12) "Jam (1\u201312)" else "Jam (1\u201324)") else "Menit",
                Type.micro,
                t.muted2
            )
            Spacer(Modifier.weight(1f))
            if (use12) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, t.hair, RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(true to "AM", false to "PM").forEach { (amFlag, txt) ->
                        val active = isAm == amFlag
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) t.accent else Color.Transparent)
                                .tap {
                                    if (isAm != amFlag) onTime((hour + 12) % 24, minute)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                txt,
                                Type.micro.copy(fontWeight = FontWeight.SemiBold),
                                if (active) (if (t.dark) t.bg else Color.White) else t.muted2
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Sekarang",
                    Type.micro.copy(fontWeight = FontWeight.SemiBold),
                    t.accent,
                    Modifier.padding(vertical = 8.dp, horizontal = 4.dp).tap {
                        onClear()
                        expanded = false
                    }
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        if (step == 0 && !use12) {
            // V1.6.8 #8: format 24 jam menampilkan angka 1-24 lengkap (2 cincin).
            // Cincin luar: 12, 1..11; Cincin dalam: 24 (00:xx), 13..23.
            val outerLabels = (0..11).map { if (it == 0) "12" else it.toString() }
            val innerLabels = (0..11).map { if (it == 0) "24" else (it + 12).toString() }
            val isInner = hour == 0 || hour >= 13
            val selIdx = if (isInner) {
                if (hour == 0) 0 else hour - 12
            } else {
                if (hour == 12) 0 else hour
            }
            TimeDial24(
                t = t,
                outerLabels = outerLabels,
                innerLabels = innerLabels,
                selectedIndex = selIdx,
                selectedInner = isInner
            ) { idx, inner ->
                val h = if (inner) {
                    if (idx == 0) 0 else idx + 12
                } else {
                    if (idx == 0) 12 else idx
                }
                onTime(h.coerceIn(0, 23), minute)
                step = 1
            }
        } else {
            val labels: List<String>
            val selected: Int
            if (step == 0) {
                labels = (0..11).map { if (it == 0) "12" else it.toString() }
                selected = h12 % 12
            } else {
                labels = (0..11).map { pad2(it * 5) }
                selected = (minute / 5).coerceIn(0, 11)
            }

            TimeDial(
                t = t,
                labels = labels,
                selected = selected
            ) { i ->
                if (step == 0) {
                    val base = if (i == 0) 12 else i
                    val half = if (base == 12) 0 else base
                    val h = if (isAm) half else half + 12
                    onTime(h.coerceIn(0, 23), minute)
                    step = 1
                } else {
                    onTime(hour, i * 5)
                    expanded = false
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(8, 12, 17, 20).forEach { h ->
                TimePreset(t, dayPart(h), Modifier.weight(1f)) {
                    onTime(h, 0)
                    expanded = false
                }
            }
        }
    }
}

/** Segmen jam/menit: target 58x44dp, segmen aktif disorot aksen. */
@Composable
private fun TimeSegment(t: Tok, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .width(58.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) t.accentWash else Color.Transparent)
            .tap(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            Type.title.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
            if (active) t.accent else t.ink
        )
    }
}

/** Koreksi halus +/- 1 untuk segmen yang sedang aktif. */
@Composable
private fun FineButton(t: Tok, ic: Ic, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(t.accentWash)
            .tap(onClick),
        contentAlignment = Alignment.Center
    ) {
        Glyph(ic, t.accent, 16.dp)
    }
}

/** Preset satu ketuk: jalur tercepat untuk waktu yang paling sering dipakai. */
@Composable
private fun TimePreset(
    t: Tok,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, t.hairSoft, RoundedCornerShape(10.dp))
            .tap(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, Type.label.copy(fontWeight = FontWeight.Medium), t.ink)
    }
}

/**
 * Dial radial 12 posisi. Jari-jari dihitung dari lebar nyata, sehingga 12 sel
 * tidak saling tumpang tindih: jarak pusat = 2*r*sin(15deg) >= lebar sel.
 * Semua angka digambar sekali, tanpa LazyColumn dan tanpa state animasi per sel.
 */
@Composable
private fun TimeDial(
    t: Tok,
    labels: List<String>,
    selected: Int,
    onPick: (Int) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val dial = minOf(maxWidth, 236.dp)
        val density = LocalDensity.current
        val dialPx = with(density) { dial.toPx() }
        val cell = dial * (42f / 236f)
        val cellPx = with(density) { cell.toPx() }
        val rPx = dialPx / 2f - cellPx / 2f - with(density) { 6.dp.toPx() }
        val c = dialPx / 2f
        val selAngle = (selected * 30f - 90f) * PI.toFloat() / 180f

        Box(Modifier.size(dial).align(Alignment.Center)) {
            Canvas(Modifier.size(dial)) {
                drawCircle(
                    color = t.hairSoft,
                    radius = dialPx / 2f - 1.dp.toPx(),
                    center = Offset(c, c),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawLine(
                    color = t.accent.copy(alpha = 0.45f),
                    start = Offset(c, c),
                    end = Offset(c + rPx * cos(selAngle), c + rPx * sin(selAngle)),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(t.accent, radius = 3.dp.toPx(), center = Offset(c, c))
            }

            labels.forEachIndexed { i, label ->
                val a = (i * 30f - 90f) * PI.toFloat() / 180f
                val x = c + rPx * cos(a)
                val y = c + rPx * sin(a)
                val on = i == selected
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (x - cellPx / 2f).roundToInt(),
                                (y - cellPx / 2f).roundToInt()
                            )
                        }
                        .size(cell)
                        .clip(CircleShape)
                        .background(if (on) t.accent else Color.Transparent)
                        .tap { onPick(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        Type.body.copy(
                            fontSize = if (dial >= 220.dp) 14.sp else 13.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        if (on) (if (t.dark) t.bg else Color.White) else t.ink
                    )
                }
            }
        }
    }
}

/**
 * V1.6.8 #8 — Dial radial 2 cincin untuk format 24 jam penuh (1..24).
 * Cincin luar = 1..12, cincin dalam = 13..24. Semua 24 angka bisa langsung
 * dipilih satu ketuk tanpa kelipatan dua.
 */
@Composable
private fun TimeDial24(
    t: Tok,
    outerLabels: List<String>,
    innerLabels: List<String>,
    selectedIndex: Int,
    selectedInner: Boolean,
    onPick: (index: Int, isInner: Boolean) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val dial = minOf(maxWidth, 256.dp)
        val density = LocalDensity.current
        val dialPx = with(density) { dial.toPx() }
        val outerCell = dial * (38f / 256f)
        val innerCell = dial * (30f / 256f)
        val outerCellPx = with(density) { outerCell.toPx() }
        val innerCellPx = with(density) { innerCell.toPx() }
        val rOuter = dialPx / 2f - outerCellPx / 2f - with(density) { 4.dp.toPx() }
        val rInner = rOuter - outerCellPx * 0.92f
        val c = dialPx / 2f
        val selAngle = (selectedIndex * 30f - 90f) * PI.toFloat() / 180f
        val selRadius = if (selectedInner) rInner else rOuter

        Box(Modifier.size(dial).align(Alignment.Center)) {
            Canvas(Modifier.size(dial)) {
                drawCircle(
                    color = t.hairSoft,
                    radius = dialPx / 2f - 1.dp.toPx(),
                    center = Offset(c, c),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = t.hairSoft.copy(alpha = 0.6f),
                    radius = rInner + innerCellPx * 0.56f,
                    center = Offset(c, c),
                    style = Stroke(width = 0.8.dp.toPx())
                )
                drawLine(
                    color = t.accent.copy(alpha = 0.45f),
                    start = Offset(c, c),
                    end = Offset(c + selRadius * cos(selAngle), c + selRadius * sin(selAngle)),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(t.accent, radius = 3.dp.toPx(), center = Offset(c, c))
            }

            // Cincin luar: 12, 1..11
            outerLabels.forEachIndexed { i, label ->
                val a = (i * 30f - 90f) * PI.toFloat() / 180f
                val x = c + rOuter * cos(a)
                val y = c + rOuter * sin(a)
                val on = !selectedInner && i == selectedIndex
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (x - outerCellPx / 2f).roundToInt(),
                                (y - outerCellPx / 2f).roundToInt()
                            )
                        }
                        .size(outerCell)
                        .clip(CircleShape)
                        .background(if (on) t.accent else Color.Transparent)
                        .tap { onPick(i, false) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        Type.body.copy(
                            fontSize = 13.5.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        if (on) (if (t.dark) t.bg else Color.White) else t.ink
                    )
                }
            }

            // Cincin dalam: 24, 13..23
            innerLabels.forEachIndexed { i, label ->
                val a = (i * 30f - 90f) * PI.toFloat() / 180f
                val x = c + rInner * cos(a)
                val y = c + rInner * sin(a)
                val on = selectedInner && i == selectedIndex
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (x - innerCellPx / 2f).roundToInt(),
                                (y - innerCellPx / 2f).roundToInt()
                            )
                        }
                        .size(innerCell)
                        .clip(CircleShape)
                        .background(if (on) t.accent else Color.Transparent)
                        .tap { onPick(i, true) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        Type.micro.copy(
                            fontSize = 11.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        if (on) (if (t.dark) t.bg else Color.White) else t.muted2
                    )
                }
            }
        }
    }
}
