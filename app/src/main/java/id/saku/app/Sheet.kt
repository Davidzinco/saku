package id.saku.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Sheet catat/ubah transaksi: nominal berformat, Date, Time (stepper + preset),
 * foto pada note, dan hapus dua ketuk. Urutan field mengikuti DESIGN.md.
 */
@Composable
fun TxSheet(
    t: Tok,
    cats: List<Cat>,
    initial: Tx?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (Tx) -> Unit,
    onDelete: (String) -> Unit
) {
    var type by remember { mutableStateOf(initial?.type ?: "out") }
    var digits by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var cat by remember {
        val initialList = catsFor(initial?.type ?: "out", cats)
        mutableStateOf(initialList.firstOrNull { it.id == initial?.cat }?.id ?: initialList.first().id)
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
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
    val invalid = amount <= 0L
    val visibleCats = catsFor(type, cats)
    val editing = initial != null

    SheetScaffold(
        t = t,
        onDismiss = onDismiss,
        visible = visible,
        footer = {
            PrimaryButton(
                label = if (editing) "Simpan perubahan" else "Simpan transaksi",
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
                        cat = cat,
                        note = note.trim(),
                        goal = initial?.goal ?: "",
                        photos = photos
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

        // Jenis
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("out" to "Keluar", "in" to "Masuk").forEach { (value, label) ->
                val on = type == value
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, if (on) t.ink else t.hair, RoundedCornerShape(10.dp))
                        .tap {
                            focus.clearFocus()
                            type = value
                            val list = catsFor(value, cats)
                            if (list.none { it.id == cat }) cat = list.first().id
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        Type.label.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                        if (on) t.ink else t.muted
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))

        // Nominal
        Text("Nominal", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        FieldShell(t, error = touched && invalid, height = 68.dp) {
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
        if (touched && invalid) {
            Text("Nominal harus lebih dari nol.", Type.label, t.error, Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(16.dp))

        // Kategori
        Text("Category", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        CatChips(t, visibleCats, cat) {
            focus.clearFocus()
            cat = it
        }
        Spacer(Modifier.height(16.dp))

        // Catatan
        Text("Note", Type.label, t.muted2)
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
        Text("Date", Type.label, t.muted2)
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

        // V1.4 #2 — Time Input sesuai design/time.webp: field ringkas berikon jam,
        // clear-time, dan popover tiga kolom (jam / menit / AM-PM) dengan interval.
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
private fun CatChips(t: Tok, cats: List<Cat>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cats.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cat ->
                    val on = cat.id == selected
                    Row(
                        Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, if (on) t.ink else t.hair, RoundedCornerShape(10.dp))
                            .tap { onSelect(cat.id) }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Glyph(glyphOf(cat.glyph), if (on) t.ink else t.muted, 15.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            cat.name,
                            Type.label.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                            if (on) t.ink else t.muted,
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
 * V1.4 #2 — Time Input mengikuti `design/time.webp`:
 * - satu field 48dp: contextual prefix (ikon jam), nilai `hh:mm` / `hh:mm aa`,
 *   clear-time (×) dan chevron pembuka popover;
 * - popover tiga kolom dengan configured intervals (jam, menit tiap 5 menit,
 *   AM/PM), selected segments disorot aksen;
 * - format 12/24 jam + preset pagi/siang/sore/malam.
 * Tidak ada keyboard, jadi tidak ada inset IME yang mengangkat sheet.
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
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val isAm = hour < 12
    val shown = if (use12) {
        pad2(h12) + ":" + pad2(minute) + " " + (if (isAm) "AM" else "PM")
    } else {
        pad2(hour) + ":" + pad2(minute)
    }

    Text("Time", Type.label, t.muted2)
    Spacer(Modifier.height(6.dp))
    FieldShell(t) {
        Glyph(Ic.Clock, t.muted2, 16.dp)
        Spacer(Modifier.width(9.dp))
        Box(
            Modifier.weight(1f).fillMaxHeight().tap { expanded = !expanded },
            contentAlignment = Alignment.CenterStart
        ) {
            Text(shown, Type.bodySm.copy(fontWeight = FontWeight.SemiBold), t.ink)
        }
        TouchTarget {
            Box(Modifier.fillMaxSize().tap(onClear), contentAlignment = Alignment.Center) {
                Glyph(Ic.Close, t.muted2, 14.dp)
            }
        }
        TouchTarget {
            Box(
                Modifier.fillMaxSize().tap { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) { Glyph(if (expanded) Ic.Up else Ic.Down, t.muted2, 16.dp) }
        }
    }

    if (!expanded) return

    val hourValues = if (use12) (1..12).toList() else (0..23).toList()
    val minuteValues = remember(minute) { (minuteIntervals + minute).distinct().sorted() }
    Spacer(Modifier.height(8.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, t.hair, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Format", Type.micro, t.muted2, Modifier.weight(1f))
            listOf("24 jam", "12 jam").forEachIndexed { i, label ->
                val on = (i == 1) == use12
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) t.accentWash else Color.Transparent)
                        .tap { use12 = i == 1 }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) { Text(label, Type.micro, if (on) t.accent else t.muted) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(148.dp)) {
            TimeColumn(
                label = "Jam",
                values = hourValues,
                selected = if (use12) h12 else hour,
                t = t,
                modifier = Modifier.weight(1f),
                display = { pad2(it) }
            ) { v ->
                val h = if (!use12) v else {
                    val base = if (v == 12) 0 else v
                    if (isAm) base else base + 12
                }
                onTime(h.coerceIn(0, 23), minute)
            }
            TimeColumn(
                label = "Menit",
                values = minuteValues,
                selected = minute,
                t = t,
                modifier = Modifier.weight(1f),
                display = { pad2(it) }
            ) { onTime(hour, it) }
            if (use12) {
                TimeColumn(
                    label = "",
                    values = listOf(0, 1),
                    selected = if (isAm) 0 else 1,
                    t = t,
                    modifier = Modifier.weight(1f),
                    display = { if (it == 0) "AM" else "PM" }
                ) { v ->
                    val h = if (v == 0) hour % 12 else (hour % 12) + 12
                    onTime(h.coerceIn(0, 23), minute)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(8, 12, 17, 20).forEach { h ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, t.hair, RoundedCornerShape(8.dp))
                        .tap { onTime(h, 0); expanded = false },
                    contentAlignment = Alignment.Center
                ) { Text(dayPart(h), Type.micro, t.muted) }
            }
        }
    }
}

private val minuteIntervals: List<Int> = (0..55 step 5).toList()

@Composable
private fun TimeColumn(
    label: String,
    values: List<Int>,
    selected: Int,
    t: Tok,
    modifier: Modifier,
    display: (Int) -> String,
    onPick: (Int) -> Unit
) {
    Column(
        modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (label.isNotEmpty()) {
            Text(label, Type.micro, t.muted2, Modifier.padding(vertical = 4.dp))
        } else {
            Spacer(Modifier.height(4.dp))
        }
        values.forEach { v ->
            val on = v == selected
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) t.accent else Color.Transparent)
                    .tap { onPick(v) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    display(v),
                    Type.bodySm.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                    if (on) (if (t.dark) t.bg else Color.White) else t.ink
                )
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
