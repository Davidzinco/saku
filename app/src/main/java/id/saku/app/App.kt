package id.saku.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import java.util.Calendar
import kotlin.math.roundToInt

enum class Tab(val label: String, val icon: ImageVector) {
    Beranda("Beranda", Icons.Rounded.Home),
    Transaksi("Transaksi", Icons.Rounded.ReceiptLong),
    Nabung("Nabung", Icons.Rounded.Savings),
    Atur("Atur", Icons.Rounded.Tune)
}

fun catIcon(name: String): ImageVector = when (name) {
    "restaurant" -> Icons.Rounded.Restaurant
    "directions_car" -> Icons.Rounded.DirectionsCar
    "home" -> Icons.Rounded.Home
    "shopping_bag" -> Icons.Rounded.ShoppingBag
    "movie" -> Icons.Rounded.Movie
    "favorite" -> Icons.Rounded.Favorite
    "payments" -> Icons.Rounded.Payments
    else -> Icons.Rounded.MoreHoriz
}

private fun Modifier.tap(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = MutableInteractionSource(), indication = null, onClick = onClick
)

@Composable
fun SakuApp() {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    var saku by remember { mutableStateOf(store.load()) }
    var tab by remember { mutableStateOf(Tab.Beranda) }
    var cursor by remember { mutableStateOf(Calendar.getInstance()) }
    var sheetOpen by remember { mutableStateOf(false) }
    val t = if (saku.dark) Dark else Light
    val backdrop = rememberLayerBackdrop()

    fun update(next: Saku) { saku = next; store.save(next) }

    Box(Modifier.fillMaxSize().background(t.bg)) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop).statusBarsPadding()) {
            when (tab) {
                Tab.Beranda -> Beranda(saku, t, cursor, onCursor = { cursor = it }, onOpen = { sheetOpen = true })
                Tab.Transaksi -> Transaksi(saku, t, cursor, onCursor = { cursor = it })
                Tab.Nabung -> Nabung(saku, t, onAdd = { g, amount ->
                    update(saku.copy(goals = saku.goals.map { if (it.id == g.id) it.copy(saved = it.saved + amount) else it }))
                })
                Tab.Atur -> Atur(saku, t) { update(it) }
            }
        }

        Dock(
            t = t,
            current = tab,
            backdrop = backdrop,
            onTab = { tab = it },
            dark = saku.dark,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 26.dp)
        )

        // tombol catat: overlay layar penuh supaya bisa ditekan walau menonjol
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(t.lime)
                .tap { sheetOpen = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, "Catat transaksi", tint = Color(0xFF0A0F12), modifier = Modifier.size(24.dp))
        }

        if (sheetOpen) {
            TxSheet(t, saku.cats, onDismiss = { sheetOpen = false }, onSave = { tx ->
                update(saku.copy(tx = (saku.tx + tx).sortedByDescending { it.date + it.time }))
                sheetOpen = false
            })
        }
    }
}

@Composable
fun Dock(
    t: Tok,
    current: Tab,
    backdrop: Backdrop,
    onTab: (Tab) -> Unit,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val inset = 7.dp
    val gap = 60.dp
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier.fillMaxWidth().height(82.dp)) {
        val slotW = (maxWidth - gap) / 4
        val slotPx = with(density) { slotW.toPx() }
        val insetPx = with(density) { inset.toPx() }
        val dockPx = with(density) { maxWidth.toPx() }
        val pillWpx = slotPx - insetPx * 2
        val maxPillX = dockPx - pillWpx - insetPx

        fun slotX(i: Int): Float {
            val base = slotPx * i + (if (i >= 2) with(density) { gap.toPx() } else 0f)
            return (base + insetPx).coerceIn(insetPx, maxPillX)
        }

        val pill = remember { Animatable(slotX(current.ordinal)) }
        var dragging by remember { mutableStateOf(false) }

        LaunchedEffect(current) {
            if (!dragging) pill.animateTo(slotX(current.ordinal), spring(dampingRatio = 0.82f, stiffness = 650f))
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(66.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(33.dp) },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(24.dp.toPx(), 24.dp.toPx(), chromaticAberration = true)
                    },
                    onDrawSurface = { drawRect(t.glass) }
                )
                .border(1.dp, t.glassRim, RoundedCornerShape(33.dp))
                .pointerInput(slotPx) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            val idx = (((pill.value - insetPx) / slotPx).roundToInt()).coerceIn(0, 3)
                            scope.launch { pill.animateTo(slotX(idx), spring(dampingRatio = 0.72f, stiffness = 900f)) }
                            if (Tab.entries[idx] != current) onTab(Tab.entries[idx])
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch { pill.animateTo(slotX(current.ordinal), spring()) }
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            scope.launch { pill.snapTo((pill.value + drag.x).coerceIn(insetPx, maxPillX)) }
                        }
                    )
                }
        ) {
            // pil indikator
            Box(
                Modifier
                    .offset { IntOffset(pill.value.roundToInt(), 0) }
                    .padding(vertical = inset)
                    .width(slotW - inset * 2)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = if (dark) 0.12f else 0.40f))
            )

            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Tab.entries.forEachIndexed { i, item ->
                    if (i == 2) Spacer(Modifier.width(gap))
                    val active = item == current
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight().tap { onTab(item) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(item.icon, null, tint = if (active) t.ink else t.muted,
                            modifier = Modifier.size(if (active) 22.dp else 21.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(item.label.uppercase(), color = if (active) t.ink else t.muted,
                            fontSize = 8.5.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }

    }
}

@Composable
fun MonthRow(t: Tok, cursor: Calendar, onCursor: (Calendar) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.KeyboardArrowLeft, "Bulan sebelumnya", tint = t.muted,
            modifier = Modifier.size(22.dp).tap {
                val c = cursor.clone() as Calendar; c.add(Calendar.MONTH, -1); onCursor(c)
            })
        Text(
            Fmt.monthLabel(cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH)).uppercase(),
            color = t.muted, fontSize = 12.sp, letterSpacing = 1.sp,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
        )
        Icon(Icons.Rounded.KeyboardArrowRight, "Bulan berikutnya", tint = t.muted,
            modifier = Modifier.size(22.dp).tap {
                val c = cursor.clone() as Calendar; c.add(Calendar.MONTH, 1); onCursor(c)
            })
    }
}

@Composable
fun TrendBars(saku: Saku, t: Tok, cursor: Calendar, onCursor: (Calendar) -> Unit) {
    val data = saku.trend(6)
    val max = (data.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    Row(
        Modifier.fillMaxWidth().height(84.dp).padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (cal, total) ->
            val active = cal.get(Calendar.YEAR) == cursor.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == cursor.get(Calendar.MONTH)
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                val frac = (total.toFloat() / max).coerceIn(0.05f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((58 * frac).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) t.ink else t.bar)
                        .tap { onCursor(cal) }
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    Fmt.monthLabel(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)).take(3),
                    color = if (active) t.ink else t.dim, fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun Beranda(
    saku: Saku,
    t: Tok,
    cursor: Calendar,
    onCursor: (Calendar) -> Unit,
    onOpen: () -> Unit
) {
    val income = saku.income(cursor)
    val outcome = saku.outcome(cursor)
    val saldo = saku.balance()

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("SAKU.", color = t.ink, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
            Text(Fmt.monthLabel(cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH)).uppercase(),
                color = t.dim, fontSize = 10.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(18.dp))

        Text("SALDO", color = t.dim, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Text(Fmt.rp(saldo), color = t.ink, fontSize = 38.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("+" + Fmt.short(income), t.pos, t)
            Pill("\u2212" + Fmt.short(outcome), t.neg, t)
            Text("bulan ini", color = t.muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(20.dp))

        MonthRow(t, cursor, onCursor)
        Spacer(Modifier.height(6.dp))
        TrendBars(saku, t, cursor, onCursor)
        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TRANSAKSI", color = t.dim, fontSize = 10.sp, letterSpacing = 1.4.sp)
            Text("catat baru", color = t.lime, fontSize = 11.sp,
                modifier = Modifier.padding(4.dp).tap(onOpen))
        }
        Spacer(Modifier.height(4.dp))

        val list = saku.monthOf(cursor).sortedByDescending { it.date + it.time }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 106.dp)) {
            if (list.isEmpty()) {
                item {
                    Text("Belum ada transaksi bulan ini.", color = t.dim, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 24.dp))
                }
            }
            items(list) { TxRow(it, saku, t) }
        }
    }
}

@Composable
fun Pill(text: String, color: Color, t: Tok) {
    Box(Modifier.border(1.dp, color.copy(alpha = 0.35f), CircleShape).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text(text, color = color, fontSize = 11.sp)
    }
}

@Composable
fun TxRow(tx: Tx, saku: Saku, t: Tok) {
    val cat = saku.catOf(tx.cat)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color(cat?.color ?: 0xFF8B9499).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(catIcon(cat?.icon ?: "more_horiz"), null,
                    tint = Color(cat?.color ?: 0xFF8B9499), modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.note.ifBlank { cat?.name ?: "Transaksi" }, color = t.ink, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${cat?.name ?: "-"} \u00b7 ${Fmt.dayLabel(tx.date)} \u00b7 ${tx.time}",
                    color = t.dim, fontSize = 11.sp)
            }
            Text((if (tx.type == "in") "+" else "\u2212") + Fmt.rp(tx.amount),
                color = if (tx.type == "in") t.pos else t.ink, fontSize = 13.sp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(t.line))
    }
}

@Composable
fun Transaksi(saku: Saku, t: Tok, cursor: Calendar, onCursor: (Calendar) -> Unit) {
    val list = saku.monthOf(cursor).sortedByDescending { it.date + it.time }
    val income = saku.income(cursor)
    val outcome = saku.outcome(cursor)
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(10.dp))
        Text("TRANSAKSI", color = t.ink, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        MonthRow(t, cursor, onCursor)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("+" + Fmt.short(income), t.pos, t)
            Pill("\u2212" + Fmt.short(outcome), t.neg, t)
            Text("${list.size} transaksi", color = t.dim, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 106.dp)) {
            items(list) { TxRow(it, saku, t) }
        }
    }
}

@Composable
fun Nabung(saku: Saku, t: Tok, onAdd: (Goal, Long) -> Unit) {
    val saved = saku.goals.sumOf { it.saved }
    val target = saku.goals.sumOf { it.target }
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(10.dp))
        Text("NABUNG", color = t.ink, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text("TERSIMPAN", color = t.dim, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Text(Fmt.rp(saved), color = t.ink, fontSize = 32.sp, fontWeight = FontWeight.Medium)
        Text("dari ${Fmt.rp(target)} \u00b7 ${if (target > 0) saved * 100 / target else 0}%",
            color = t.muted, fontSize = 12.sp)
        Spacer(Modifier.height(22.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 106.dp)) {
            items(saku.goals) { g ->
                val pct = if (g.target > 0) (g.saved * 100 / g.target).coerceAtMost(100) else 0
                Column(Modifier.padding(vertical = 14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(g.name, color = t.ink, fontSize = 14.sp)
                        Text("$pct%", color = t.muted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(t.track)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(pct / 100f).background(t.pos))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${Fmt.rp(g.saved)} / ${Fmt.rp(g.target)}", color = t.dim, fontSize = 11.sp)
                        Text("+ Rp 100rb", color = t.lime, fontSize = 12.sp,
                            modifier = Modifier.padding(4.dp).tap { onAdd(g, 100_000L) })
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(t.line))
            }
        }
    }
}
