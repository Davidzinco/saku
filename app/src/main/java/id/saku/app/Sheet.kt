package id.saku.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

/** Sheet catat transaksi: label di atas field, validasi saat blur, satu aksi utama. */
@Composable
fun TxSheet(
    t: Tok,
    cats: List<Cat>,
    lastCat: String?,
    onDismiss: () -> Unit,
    onSave: (Tx) -> Unit
) {
    var type by remember { mutableStateOf("out") }
    var amount by remember { mutableStateOf("") }
    var cat by remember {
        val initial = catsFor("out", cats)
        mutableStateOf(initial.firstOrNull { it.id == lastCat }?.id ?: initial.first().id)
    }
    var note by remember { mutableStateOf("") }
    var dateIso by remember { mutableStateOf(Fmt.iso(Date())) }
    var calOpen by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    LaunchedEffect(Unit) { shown = true }

    val nominal = amount.toLongOrNull() ?: 0L
    val invalid = nominal <= 0L
    val visibleCats = catsFor(type, cats)

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = shown, enter = fadeIn(tween(180))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (t.dark) Color(0x99000000) else Color(0x591B2721))
                    .tap(onDismiss)
            )
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxSheet = maxHeight * 0.94f
            AnimatedVisibility(
                visible = shown,
                enter = slideInVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxSheet)
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .background(t.sheet)
                        .border(1.dp, t.hairSoft, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .imePadding()
                ) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                            .padding(top = 12.dp, bottom = 26.dp)
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(38.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(t.hair)
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Catat transaksi", Type.title, t.ink, Modifier.weight(1f))
                            TouchTarget {
                                Box(Modifier.fillMaxSize().tap(onDismiss), contentAlignment = Alignment.Center) {
                                    Glyph(Ic.Close, t.muted2, 18.dp)
                                }
                            }
                        }
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
                        Spacer(Modifier.height(20.dp))

                        // Nominal
                        Text("Nominal", Type.label, t.muted2)
                        Spacer(Modifier.height(6.dp))
                        FieldShell(t, error = touched && invalid, height = 68.dp) {
                            Text("Rp", Type.currency, t.muted2)
                            Spacer(Modifier.width(8.dp))
                            PlainField(
                                value = amount,
                                onValueChange = { v -> amount = v.filter { it.isDigit() }.trimStart('0').take(12) },
                                placeholder = "0",
                                t = t,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                style = Type.display,
                                keyboardType = KeyboardType.Number,
                                onFocus = { focused ->
                                    if (wasFocused && !focused) touched = true
                                    wasFocused = focused
                                }
                            )
                        }
                        if (touched && invalid) {
                            Text(
                                "Nominal harus lebih dari nol.",
                                Type.label,
                                t.error,
                                Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(18.dp))

                        // Kategori
                        Text("Kategori", Type.label, t.muted2)
                        Spacer(Modifier.height(8.dp))
                        CatChips(t, visibleCats, cat) {
                            focus.clearFocus()
                            cat = it
                        }
                        Spacer(Modifier.height(18.dp))

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
                        }
                        Spacer(Modifier.height(18.dp))

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
                                Text(
                                    Fmt.dayLabel(dateIso) + " \u00b7 " + Fmt.tgl(dateIso),
                                    Type.body,
                                    t.ink
                                )
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

                        Spacer(Modifier.height(22.dp))
                        PrimaryButton(
                            label = "Simpan transaksi",
                            t = t,
                            enabled = !invalid,
                            onDisabledClick = { touched = true }
                        ) {
                            onSave(
                                Tx(
                                    id = System.nanoTime().toString(36),
                                    date = dateIso,
                                    time = Fmt.nowTime(),
                                    type = type,
                                    amount = nominal,
                                    cat = cat,
                                    note = note.trim()
                                )
                            )
                        }
                    }
                }
            }
        }
    }
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
            Text(
                Fmt.monthLabel(first),
                Type.label,
                t.ink,
                Modifier.weight(1f),
                align = TextAlign.Center
            )
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
                                            if (isToday && !on) {
                                                Modifier.border(1.dp, t.hair, CircleShape)
                                            } else {
                                                Modifier
                                            }
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
