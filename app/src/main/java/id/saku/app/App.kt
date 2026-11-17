package id.saku.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import com.kyant.backdrop.Backdrop
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

enum class Tab(val label: String, val icon: Ic) {
    Home("Home", Ic.Home),
    Transaksi("Transaksi", Ic.Receipt),
    Nabung("Nabung", Ic.Wallet),
    Setting("Setting", Ic.Sliders)
}

@Composable
fun SakuApp() {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    var saku by remember { mutableStateOf(store.load()) }
    var tab by remember { mutableStateOf(Tab.Home) }
    var cursor by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    var sheetOpen by remember { mutableStateOf(false) }
    var sheetVisible by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Tx?>(null) }
    var goalSheet by remember { mutableStateOf<GoalSheet?>(null) }
    var goalVisible by remember { mutableStateOf(false) }
    var detailTx by remember { mutableStateOf<Tx?>(null) }
    var detailVisible by remember { mutableStateOf(false) }
    var flashId by remember { mutableStateOf<String?>(null) }
    // V1.3 #1: saat jari sedang scrub grafik, pager dimatikan supaya geseran
    // horizontal tidak pernah dibaca sebagai "pindah ke halaman Transaksi".
    var chartScrubbing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val t = saku.tokens()
    // Seluruh layar direkam ke satu layer; dock/FAB kaca memantulkan layer ini.
    val backdrop = rememberSakuBackdrop(t.bg)
    val pager = rememberPagerState(pageCount = { Tab.entries.size })
    var pageJob by remember { mutableStateOf<Job?>(null) }
    val view = LocalView.current
    val activity = remember(ctx) { ctx.findActivity() }

    /**
     * V1.3 #3: satu jalur untuk berpindah tab. Klik cepat berulang membatalkan animasi
     * lama, bukan menumpuk animateScrollToPage yang saling menunggu (dulu bikin freeze).
     */
    fun selectTab(next: Tab) {
        if (tab == next) return
        tab = next
        pageJob?.cancel()
        pageJob = scope.launch {
            try {
                pager.animateScrollToPage(next.ordinal)
            } catch (e: CancellationException) {
                // animasi lama dibatalkan klik berikutnya — wajar
            }
        }
    }

    // Geser pager oleh pengguna tetap menyorot tab yang benar; animasi programatik
    // tidak boleh menimpa tab yang baru dipilih.
    LaunchedEffect(pager.settledPage) {
        val target = Tab.entries[pager.settledPage]
        if (tab != target && pageJob?.isActive != true) tab = target
    }

    LaunchedEffect(saku.dark, saku.paletteId, saku.customAccent, saku.customTone) {
        val win = activity?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(win, view).apply {
            isAppearanceLightStatusBars = !saku.dark
            isAppearanceLightNavigationBars = !saku.dark
        }
    }

    LaunchedEffect(flashId) {
        if (flashId != null) {
            delay(1400)
            flashId = null
        }
    }

    fun update(next: Saku) {
        saku = next
        store.save(next)
    }

    fun openEditor(tx: Tx?) {
        editing = tx
        sheetOpen = true
        sheetVisible = true
    }

    fun closeTxSheet() {
        sheetVisible = false
        scope.launch {
            delay(200)
            sheetOpen = false
        }
    }

    fun closeGoalSheet() {
        goalVisible = false
        scope.launch {
            delay(200)
            goalSheet = null
        }
    }

    // V1.3 #6: klik baris membuka detail dulu, bukan langsung editor.
    fun openDetail(tx: Tx) {
        detailTx = tx
        detailVisible = true
    }

    fun closeDetailSheet() {
        detailVisible = false
        scope.launch {
            delay(200)
            detailTx = null
        }
    }

    // back: prioritas paling rendah (Atur punya handler sendiri, sheet lebih tinggi)
    BackHandler(enabled = tab != Tab.Home && sheetOpen.not() && detailTx == null) {
        selectTab(Tab.Home)
    }

    Box(Modifier.fillMaxSize().background(t.bg)) {
        Box(Modifier.fillMaxSize().sakuLayerBackdrop(backdrop)) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !chartScrubbing,
                beyondViewportPageCount = 1
            ) { page ->
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    when (Tab.entries[page]) {
                        Tab.Home -> Beranda(
                            saku = saku,
                            t = t,
                            cursor = cursor,
                            flashId = flashId,
                            onCursor = { cursor = it },
                            onCatat = { openEditor(null) },
                            onOpen = { openDetail(it) },
                            onNabung = { selectTab(Tab.Nabung) },
                            onScrub = { chartScrubbing = it }
                        )

                        Tab.Transaksi -> Transaksi(
                            saku = saku,
                            t = t,
                            cursor = cursor,
                            flashId = flashId,
                            onCursor = { cursor = it },
                            onCatat = { openEditor(null) },
                            onOpen = { openDetail(it) }
                        )

                        Tab.Nabung -> Nabung(
                            saku = saku,
                            t = t,
                            onChange = { update(it) },
                            onSheet = {
                                goalSheet = it
                                goalVisible = true
                            }
                        )

                        Tab.Setting -> Atur(saku, t, onChange = { update(it) })
                    }
                }
            }
        }

        Dock(
            t = t,
            current = tab,
            onTab = { selectTab(it) },
            backdrop = backdrop,
            glass = saku.glass,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp)
        )

        // FAB tinggal di parent layar penuh supaya tetap bisa ditekan walau menonjol di atas dock.
        val fabChrome = if (saku.glass) {
            Modifier.glassFab(t, backdrop)
        } else {
            Modifier.background(t.accent)
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 51.dp)
                .size(54.dp)
                .shadow(
                    elevation = if (saku.glass) 0.dp else 10.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = t.accent.copy(alpha = 0.40f),
                    spotColor = t.accent.copy(alpha = 0.50f)
                )
                .clip(CircleShape)
                .then(fabChrome)
                .semantics { contentDescription = "Catat transaksi" }
                .tap { openEditor(null) },
            contentAlignment = Alignment.Center
        ) {
            Glyph(Ic.Plus, if (t.dark) t.bg else Color.White, 24.dp, 1.8.dp)
        }

        if (sheetOpen) {
            BackHandler { closeTxSheet() }
            TxSheet(
                t = t,
                cats = saku.cats,
                initial = editing,
                visible = sheetVisible,
                onDismiss = { closeTxSheet() },
                onSave = { tx ->
                    // foto lama dibuang kalau diganti/dihapus
                    val oldPhoto = editing?.photo ?: ""
                    if (oldPhoto.isNotEmpty() && oldPhoto != tx.photo) deletePhoto(oldPhoto)
                    update(saku.withTx(tx))
                    flashId = tx.id
                    cursor = Calendar.getInstance().apply {
                        time = Fmt.parse(tx.date)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    closeTxSheet()
                },
                onDelete = { id ->
                    saku.tx.firstOrNull { it.id == id }?.let { deletePhoto(it.photo) }
                    update(saku.withoutTx(id))
                    closeTxSheet()
                }
            )
        }

        // V1.3 #6 — detail transaksi, dirender setelah TxSheet supaya BackHandler-nya menang.
        when (val d = detailTx) {
            null -> Unit
            else -> {
                BackHandler { closeDetailSheet() }
                TxDetailSheet(
                    t = t,
                    saku = saku,
                    tx = d,
                    visible = detailVisible,
                    onDismiss = { closeDetailSheet() },
                    onEdit = { tx ->
                        closeDetailSheet()
                        scope.launch {
                            delay(220)
                            openEditor(tx)
                        }
                    },
                    onDelete = { id ->
                        deletePhoto(d.photo)
                        update(saku.withoutTx(id))
                        closeDetailSheet()
                    }
                )
            }
        }

        // sheet nabung dirender di level aplikasi supaya menutup dock, bukan di bawahnya
        when (val g = goalSheet) {
            is GoalSheet.Setor -> {
                BackHandler { closeGoalSheet() }
                AmountSheet(
                    t = t,
                    title = "Setor ke " + g.goal.name,
                    subtitle = "Tercatat sebagai pengeluaran, saldo akhir ikut berkurang.",
                    quick = listOf(100_000L, 250_000L, 500_000L, 1_000_000L),
                    max = null,
                    visible = goalVisible,
                    onDismiss = { closeGoalSheet() }
                ) { amount ->
                    update(saku.deposit(g.goal, amount))
                    closeGoalSheet()
                }
            }

            is GoalSheet.Tarik -> {
                BackHandler { closeGoalSheet() }
                AmountSheet(
                    t = t,
                    title = "Tarik dari " + g.goal.name,
                    subtitle = "Tersedia " + Fmt.rp(saku.savedOf(g.goal.id)) + ". Tercatat sebagai pemasukan.",
                    quick = listOf(100_000L, 250_000L, 500_000L),
                    max = saku.savedOf(g.goal.id),
                    visible = goalVisible,
                    onDismiss = { closeGoalSheet() }
                ) { amount ->
                    update(saku.withdraw(g.goal, amount))
                    closeGoalSheet()
                }
            }

            is GoalSheet.Ubah -> {
                BackHandler { closeGoalSheet() }
                GoalEditSheet(
                    t = t,
                    goal = g.goal,
                    saved = saku.savedOf(g.goal.id),
                    visible = goalVisible,
                    onDismiss = { closeGoalSheet() },
                    onSave = { name, target ->
                        update(
                            saku.copy(
                                goals = saku.goals.map {
                                    if (it.id == g.goal.id) it.copy(name = name, target = target) else it
                                }
                            )
                        )
                        closeGoalSheet()
                    },
                    onDelete = {
                        update(saku.copy(goals = saku.goals.filter { it.id != g.goal.id }))
                        closeGoalSheet()
                    }
                )
            }

            null -> Unit
        }
    }
}

/**
 * Dock datar: satu lembar surface, garis rambut, satu wash aksen untuk tab aktif.
 * V1.3: sistem geser dipertahankan, tapi halaman baru dibuka SETELAH jari dilepas.
 * Saat `glass` aktif, permukaannya memakai resep liquid glass dari
 * Kyant0/AndroidLiquidGlass (vibrancy + blur + lens), dengan warna tetap dari [Tok].
 */
@Composable
fun Dock(
    t: Tok,
    current: Tab,
    onTab: (Tab) -> Unit,
    backdrop: Backdrop,
    glass: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var snapNext by remember { mutableStateOf(false) }
    // Posisi wash saat jari menahan, disimpan sebagai state biasa. AwaitPointerEventScope
    // adalah restricted suspension scope, jadi Animatable.snapTo() tidak boleh dipanggil
    // langsung dari dalam pointerInput.
    var dragX by remember { mutableStateOf(0f) }
    val shape = dockShape()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(66.dp)
            .then(
                if (glass) {
                    Modifier.glassDock(t, backdrop, shape)
                } else {
                    Modifier
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
                }
            )
    ) {
        val slot = (maxWidth - 58.dp) / 4
        val slotPx = with(density) { slot.toPx() }
        val centerPx = with(density) { 58.dp.toPx() }

        fun startOf(i: Int) = slotPx * i + if (i >= 2) centerPx else 0f
        fun indexAt(x: Float) = (0..3).minByOrNull { abs(startOf(it) + slotPx / 2f - x) } ?: 0

        val wash = remember { Animatable(startOf(current.ordinal)) }
        val shownX = if (dragging) dragX else wash.value

        // Satu-satunya penggerak wash di luar drag. `snapNext` memakai spring pasca-geser.
        LaunchedEffect(current, dragging) {
            if (!dragging) {
                val spec = if (snapNext) {
                    spring<Float>(dampingRatio = 0.72f, stiffness = 900f)
                } else {
                    spring<Float>(dampingRatio = 0.82f, stiffness = 650f)
                }
                snapNext = false
                wash.animateTo(startOf(current.ordinal), spec)
            }
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Box(
                Modifier
                    .offset { IntOffset(shownX.roundToInt(), 0) }
                    .padding(horizontal = 8.dp)
                    .width(slot - 16.dp)
                    .height(44.dp)
                    .then(
                        if (glass) {
                            Modifier.glassWash(t, backdrop, washShape())
                        } else {
                            Modifier
                                .clip(washShape())
                                .background(t.accentWash)
                        }
                    )
            )
        }

        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Tab.entries.forEachIndexed { i, item ->
                if (i == 2) Spacer(Modifier.width(58.dp))
                val active = item == current
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
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

        // satu handler untuk ketuk dan geser
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(slotPx, centerPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var moved = false
                        var acc = 0f
                        var lastX = down.position.x
                        dragX = wash.value
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val dx = change.position.x - lastX
                            lastX = change.position.x
                            acc += dx
                            if (!moved && abs(acc) > viewConfiguration.touchSlop) {
                                moved = true
                                dragging = true
                            }
                            if (moved) {
                                change.consume()
                                // Wash mengikuti jari lewat state; halaman TIDAK dibuka
                                // selama jari masih menahan (V1.3 #2).
                                dragX = (dragX + dx).coerceIn(0f, startOf(3))
                            }
                        }
                        if (moved) {
                            val released = dragX
                            val target = Tab.entries[indexAt(lastX)]
                            // snapTo butuh suspend biasa, jadi dijalankan di coroutine
                            // sebelum state dilepas; LaunchedEffect lalu menypring ke slot.
                            scope.launch {
                                wash.snapTo(released)
                                snapNext = true
                                dragging = false
                                onTab(target)
                            }
                        } else {
                            onTab(Tab.entries[indexAt(down.position.x)])
                        }
                    }
                }
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
