package id.saku.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

enum class Tab(val label: String, val icon: Ic) {
    Beranda("Beranda", Ic.Home),
    Transaksi("Transaksi", Ic.Receipt),
    Nabung("Nabung", Ic.Wallet),
    Atur("Atur", Ic.Sliders)
}

@Composable
fun SakuApp() {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    var saku by remember { mutableStateOf(store.load()) }
    var tab by remember { mutableStateOf(Tab.Beranda) }
    var cursor by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    var sheet by remember { mutableStateOf(false) }

    val t = if (saku.dark) Dark else Light
    val view = LocalView.current
    val activity = remember(ctx) { ctx.findActivity() }

    LaunchedEffect(saku.dark) {
        val win = activity?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(win, view).apply {
            isAppearanceLightStatusBars = !saku.dark
            isAppearanceLightNavigationBars = !saku.dark
        }
    }

    fun update(next: Saku) {
        saku = next
        store.save(next)
    }

    Box(Modifier.fillMaxSize().background(t.bg)) {
        Crossfade(
            targetState = tab,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            label = "layar"
        ) { current ->
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                when (current) {
                    Tab.Beranda -> Beranda(
                        saku = saku,
                        t = t,
                        cursor = cursor,
                        onCursor = { cursor = it },
                        onCatat = { sheet = true },
                        onNabung = { tab = Tab.Nabung }
                    )

                    Tab.Transaksi -> Transaksi(
                        saku = saku,
                        t = t,
                        cursor = cursor,
                        onCursor = { cursor = it },
                        onCatat = { sheet = true }
                    )

                    Tab.Nabung -> Nabung(saku, t) { update(it) }
                    Tab.Atur -> Atur(saku, t) { update(it) }
                }
            }
        }

        Dock(
            t = t,
            current = tab,
            onTab = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp)
        )

        // FAB tinggal di parent layar penuh supaya tetap bisa ditekan walau menonjol di atas dock.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 51.dp)
                .size(54.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = t.accent.copy(alpha = 0.40f),
                    spotColor = t.accent.copy(alpha = 0.50f)
                )
                .clip(CircleShape)
                .background(t.accent)
                .semantics { contentDescription = "Catat transaksi" }
                .tap { sheet = true },
            contentAlignment = Alignment.Center
        ) {
            Glyph(Ic.Plus, if (t.dark) t.bg else Color.White, 24.dp, 1.8.dp)
        }

        if (sheet) {
            TxSheet(
                t = t,
                cats = saku.cats,
                lastCat = saku.tx.firstOrNull()?.cat,
                onDismiss = { sheet = false },
                onSave = { tx ->
                    val next = saku.copy(
                        tx = (saku.tx + tx).sortedWith(
                            compareByDescending<Tx> { it.date }.thenByDescending { it.time }
                        )
                    )
                    cursor = Calendar.getInstance().apply {
                        time = Fmt.parse(tx.date)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    update(next)
                    sheet = false
                }
            )
        }
    }
}

/**
 * Dock datar: satu lembar surface, garis rambut, satu wash aksen untuk tab aktif.
 * Bisa diketuk atau digeser (wash mengikuti jari lalu snap ke slot terdekat).
 */
@Composable
fun Dock(t: Tok, current: Tab, onTab: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(66.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = t.ink.copy(alpha = 0.18f),
                spotColor = t.ink.copy(alpha = 0.24f)
            )
            .clip(shape)
            .background(t.sheet)
            .border(1.dp, t.hairSoft, shape)
    ) {
        val slot = (maxWidth - 58.dp) / 4
        val slotPx = with(density) { slot.toPx() }
        val centerPx = with(density) { 58.dp.toPx() }

        fun startOf(i: Int) = slotPx * i + if (i >= 2) centerPx else 0f

        val wash = remember { Animatable(startOf(current.ordinal)) }

        LaunchedEffect(current, dragging) {
            if (!dragging) wash.animateTo(startOf(current.ordinal), spring(dampingRatio = 0.82f, stiffness = 650f))
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Box(
                Modifier
                    .offset { IntOffset(wash.value.roundToInt(), 0) }
                    .padding(horizontal = 8.dp)
                    .width(slot - 16.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(t.accentWash)
            )
        }

        Row(
            Modifier
                .fillMaxSize()
                .pointerInput(slotPx, centerPx) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            val center = wash.value + slotPx / 2f
                            val idx = (0..3).minByOrNull { abs(startOf(it) + slotPx / 2f - center) } ?: 0
                            scope.launch {
                                wash.animateTo(startOf(idx), spring(dampingRatio = 0.72f, stiffness = 900f))
                            }
                            if (Tab.entries[idx] != current) onTab(Tab.entries[idx])
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch {
                                wash.animateTo(startOf(current.ordinal), spring(dampingRatio = 0.82f, stiffness = 650f))
                            }
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            scope.launch {
                                wash.snapTo((wash.value + drag.x).coerceIn(0f, startOf(3)))
                            }
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tab.entries.forEachIndexed { i, item ->
                if (i == 2) Spacer(Modifier.width(58.dp))
                val active = item == current
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { contentDescription = item.label }
                        .tap { if (!dragging) onTab(item) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Glyph(item.icon, if (active) t.accent else t.faint, 20.dp)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        item.label,
                        Type.tab.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
                        if (active) t.accent else t.muted2
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
