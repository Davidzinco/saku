package id.saku.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Date

private fun Modifier.tap(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = MutableInteractionSource(), indication = null, onClick = onClick
)

@Composable
fun Scrim(onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xCC000000)).tap(onDismiss)
    )
}

@Composable
fun TxSheet(
    t: Tok,
    cats: List<Cat>,
    onDismiss: () -> Unit,
    onSave: (Tx) -> Unit
) {
    var type by remember { mutableStateOf("out") }
    var amount by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(cats.firstOrNull { it.id != "gaji" }?.id ?: "makan") }
    var note by remember { mutableStateOf("") }
    var dateIso by remember { mutableStateOf(Fmt.iso(Date())) }
    var calOpen by remember { mutableStateOf(false) }

    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible.value = true }

    Box(Modifier.fillMaxSize()) {
        Scrim(onDismiss)
        AnimatedVisibility(
            visible = visible.value,
            enter = slideInVertically(initialOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(t.panel)
                    .border(1.dp, t.line, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp, bottom = 26.dp)
                    .imePadding()
            ) {
                // pegangan
                Box(Modifier.align(Alignment.CenterHorizontally).width(38.dp).height(4.dp).clip(CircleShape).background(t.line))
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Catat transaksi", color = t.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Rounded.Close, "Tutup", tint = t.muted,
                        modifier = Modifier.size(22.dp).tap(onDismiss))
                }
                Spacer(Modifier.height(18.dp))

                // jenis
                Row(
                    Modifier.fillMaxWidth().clip(CircleShape).background(t.track).padding(3.dp)
                ) {
                    listOf("out" to "Keluar", "in" to "Masuk").forEach { (v, name) ->
                        val on = type == v
                        Box(
                            Modifier.weight(1f).clip(CircleShape)
                                .background(if (on) t.panel else Color.Transparent)
                                .tap { type = v; if (v == "in") cat = "gaji" }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name, color = if (on) t.ink else t.muted, fontSize = 13.sp,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                // nominal
                Text("NOMINAL", color = t.dim, fontSize = 10.sp, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rp", color = t.muted, fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (amount.isEmpty()) Text("0", color = t.dim, fontSize = 32.sp, fontWeight = FontWeight.Medium)
                        BasicTextField(
                            value = amount,
                            onValueChange = { v -> amount = v.filter { it.isDigit() }.take(12) },
                            textStyle = TextStyle(color = t.ink, fontSize = 32.sp, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(t.lime),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(t.line))
                Spacer(Modifier.height(18.dp))

                // kategori
                Text("KATEGORI", color = t.dim, fontSize = 10.sp, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(10.dp))
                val shown = cats.filter { if (type == "in") it.id == "gaji" || it.id == "lain" else it.id != "gaji" }
                ChipGrid(shown.size, 3, 8) { i ->
                    val c = shown[i]
                    run {
                        val on = cat == c.id
                        Row(
                            Modifier.clip(CircleShape)
                                .background(if (on) Color(c.color).copy(alpha = 0.18f) else t.track)
                                .border(1.dp, if (on) Color(c.color).copy(alpha = 0.55f) else Color.Transparent, CircleShape)
                                .tap { cat = c.id }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(catIcon(c.icon), null, tint = Color(c.color), modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(c.name, color = if (on) t.ink else t.muted, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))

                // catatan
                Text("CATATAN", color = t.dim, fontSize = 10.sp, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(t.track)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (note.isEmpty()) Text("mis. Makan siang", color = t.dim, fontSize = 14.sp)
                    BasicTextField(
                        value = note,
                        onValueChange = { note = it.take(60) },
                        textStyle = TextStyle(color = t.ink, fontSize = 14.sp),
                        cursorBrush = SolidColor(t.lime),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(18.dp))

                // tanggal
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(t.track)
                        .tap { calOpen = !calOpen }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tanggal", color = t.muted, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Fmt.dayLabel(dateIso) + " \u00b7 " + Fmt.human(dateIso), color = t.ink, fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(if (calOpen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null,
                            tint = t.muted, modifier = Modifier.size(18.dp))
                    }
                }

                if (calOpen) {
                    Spacer(Modifier.height(10.dp))
                    CalendarBox(t, dateIso) { dateIso = it; calOpen = false }
                }

                Spacer(Modifier.height(22.dp))

                val nominal = amount.toLongOrNull() ?: 0L
                val ok = nominal > 0
                Box(
                    Modifier.fillMaxWidth().height(52.dp).clip(CircleShape)
                        .background(if (ok) t.lime else t.track)
                        .tap { if (ok) onSave(Tx(System.nanoTime().toString(36), dateIso, Fmt.nowTime(), type, nominal, cat, note)) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Check, null, tint = if (ok) Color(0xFF0A0F12) else t.dim, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan", color = if (ok) Color(0xFF0A0F12) else t.dim, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** grid chip sederhana: perRow kolom, item dipotong sesuai jumlah */
@Composable
fun ChipGrid(count: Int, perRow: Int, spacing: Int, content: @Composable (Int) -> Unit) {
    val rows = (count + perRow - 1) / perRow
    Column(verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.dp), modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until perRow) {
                    val i = r * perRow + c
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (i < count) content(i)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarBox(t: Tok, selected: String, onPick: (String) -> Unit) {
    val cursor = remember {
        Calendar.getInstance().apply { time = Fmt.parse(selected) }
    }
    var ym by remember { mutableStateOf(cursor.get(Calendar.YEAR) to cursor.get(Calendar.MONTH)) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(t.track).padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.KeyboardArrowLeft, null, tint = t.muted, modifier = Modifier.size(20.dp)
                .tap { ym = if (ym.second == 0) ym.first - 1 to 11 else ym.first to ym.second - 1 })
            Text(Fmt.monthLabel(ym.first, ym.second).uppercase(), color = t.ink, fontSize = 12.sp,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Icon(Icons.Rounded.KeyboardArrowRight, null, tint = t.muted, modifier = Modifier.size(20.dp)
                .tap { ym = if (ym.second == 11) ym.first + 1 to 0 else ym.first to ym.second + 1 })
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "S", "R", "K", "J", "S", "M").forEach {
                Text(it, color = t.dim, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))

        val first = Calendar.getInstance().apply {
            set(ym.first, ym.second, 1)
        }
        val startDow = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7  // Senin = 0, Minggu = 6
        val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cells = startDow + days
        val rows = (cells + 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val dayNum = r * 7 + c - startDow + 1
                        Box(
                            Modifier.weight(1f).height(34.dp).clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum in 1..days) {
                                val cal = Calendar.getInstance().apply { set(ym.first, ym.second, dayNum) }
                                val iso = Fmt.iso(cal.time)
                                val on = iso == selected
                                Box(
                                    Modifier.size(30.dp).clip(CircleShape)
                                        .background(if (on) t.lime else Color.Transparent)
                                        .tap { onPick(iso) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$dayNum", color = if (on) Color(0xFF0A0F12) else t.ink, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Hari ini", color = t.lime, fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 4.dp).tap { onPick(Fmt.iso(Date())) })
            Text("Kemarin", color = t.muted, fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 4.dp).tap {
                    val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -1); onPick(Fmt.iso(c.time))
                })
        }
    }
}
