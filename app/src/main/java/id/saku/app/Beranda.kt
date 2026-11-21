package id.saku.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Calendar

fun shiftMonth(c: Calendar, delta: Int): Calendar = (c.clone() as Calendar).apply {
    set(Calendar.DAY_OF_MONTH, 1)
    add(Calendar.MONTH, delta)
}

@Composable
fun Beranda(
    saku: Saku,
    t: Tok,
    cursor: Calendar,
    flashId: String?,
    onCursor: (Calendar) -> Unit,
    onCatat: () -> Unit,
    onOpen: (Tx) -> Unit,
    onNabung: () -> Unit,
    onScrub: (Boolean) -> Unit = {}
) {
    val fontScale = LocalDensity.current.fontScale
    val rows = saku.monthTxs(cursor)
    val running = saku.runningBalances(cursor, rows)

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        // V1.4 #9: label "Lembar [date]" dihapus. Wordmark saja sudah cukup.
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SAKU", Type.wordmark, t.ink)
        }

        SaldoBlock(t, saku.balance(), saku.income(cursor), saku.outcome(cursor), fontScale)
        TrendSection(t, saku, cursor, onCursor, onScrub)
        CatSection(t, saku, cursor, onCatat)
        NabungRow(t, saku, onNabung)
        ListSection(t, saku, cursor, rows, running, flashId, onCatat, onOpen, Modifier.weight(1f).padding(top = 2.dp))
    }
}

/** Angka yang bergerak ke nilai baru tanpa kehilangan presisi Long. */
@Composable
fun animatedLong(target: Long, duration: Int = 420): Long {
    val scale = motionScale()
    var from by remember { mutableStateOf(target) }
    var value by remember { mutableStateOf(target) }
    val anim = remember { Animatable(0f) }

    LaunchedEffect(target) {
        if (target == value) return@LaunchedEffect
        from = value
        if (scale <= 0f) {
            value = target
            return@LaunchedEffect
        }
        val start = from
        anim.snapTo(0f)
        anim.animateTo(1f, tween(duration, easing = FastOutSlowInEasing)) {
            value = (start + ((target - start).toDouble() * this.value)).toLong()
        }
        value = target
    }
    return value
}

@Composable
private fun SaldoBlock(t: Tok, saldo: Long, income: Long, outcome: Long, fontScale: Float) {
    val shownSaldo = animatedLong(saldo)
    // V1.4 #9: label "Saldo akhir" dihapus.
    // PERBAIKAN CRASH 2026-11-18: ringkasan ini sengaja TIDAK memakai kaca.
    // Ia berada di dalam subtree `sakuLayerBackdrop`; memasang `drawBackdrop`
    // di sini membuat display list mereferensi dirinya sendiri dan RenderThread
    // stack overflow (SIGSEGV). Kaca hanya untuk overlay: Dock / FAB / Sheet.
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 11.dp)
    ) {
        Row(Modifier.padding(top = 3.dp)) {
            Text("Rp", Type.currency, t.muted2, Modifier.alignByBaseline())
            Spacer(Modifier.width(9.dp))
            Text(Fmt.num(shownSaldo), Type.hero(fontScale), t.ink, Modifier.alignByBaseline())
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            Stat(t, "Masuk", Fmt.sign(animatedLong(income, 360)), if (income > 0) t.pos else t.ink)
            Stat(t, "Keluar", Fmt.out(animatedLong(outcome, 360)), if (outcome > 0) t.error else t.ink)
            val diff = income - outcome
            Stat(
                t,
                "Selisih",
                Fmt.sign(animatedLong(diff, 360)),
                if (diff > 0) t.pos else if (diff < 0) t.error else t.ink
            )
        }
    }
}

@Composable
fun Stat(t: Tok, label: String, value: String, color: Color) {
    Column {
        Text(label, Type.label, t.muted2)
        Text(value, Type.stat, color, Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun NavCircle(t: Tok, icon: Ic, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(t.hairSoft)
            .semantics { contentDescription = label }
            .tap(onClick),
        contentAlignment = Alignment.Center
    ) {
        Glyph(icon, t.muted2, 14.dp)
    }
}

@Composable
fun MonthNav(t: Tok, cursor: Calendar, onCursor: (Calendar) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NavCircle(t, Ic.ChevL, "Bulan sebelumnya") { onCursor(shiftMonth(cursor, -1)) }
        AnimatedContent(
            targetState = Fmt.monthLabel(cursor),
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            modifier = Modifier.weight(1f)
        ) { label ->
            Text(label, Type.section, t.ink, Modifier.fillMaxWidth().padding(horizontal = 8.dp), align = TextAlign.Center)
        }
        NavCircle(t, Ic.ChevR, "Bulan berikutnya") { onCursor(shiftMonth(cursor, 1)) }
    }
}

@Composable
private fun TrendSection(
    t: Tok,
    saku: Saku,
    cursor: Calendar,
    onCursor: (Calendar) -> Unit,
    onScrub: (Boolean) -> Unit
) {
    val scale = motionScale()
    // V1.4 #1: jendela grafik hanya berubah saat tahun/paruh tahun berganti, bukan
    // tiap kali jari bergerak. `trendFast()` juga cuma menyaring transaksi sekali.
    val windowKey = cursor.get(Calendar.YEAR) * 100 +
        (if (saku.months >= 12) 0 else if (cursor.get(Calendar.MONTH) <= 5) 0 else 6)
    val data = remember(saku, windowKey) { saku.trendFast(cursor) }
    val max = (data.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    val activeIndex = data.indexOfFirst { sameMonth(it.first, cursor) }.coerceAtLeast(0)
    val many = data.size > 6
    val gap = if (many) 4.dp else 14.dp
    val barW = if (many) 14.dp else 24.dp

    // V1.3 #1 — scrub posisional: TEKAN (tanpa harus menggeser) langsung memilih
    // bulan di bawah jari, dan data bulan itu langsung tampil. Down dikonsumsi di
    // sini supaya HorizontalPager tidak mengira ini geser pindah halaman.
    // State dibaca lewat rememberUpdatedState supaya handler tidak restart saat
    // cursor berubah di tengah gestur (dulu pointerInput(cursor) memutus drag).
    val dataState = rememberUpdatedState(data)
    val pickState = rememberUpdatedState(onCursor)
    val scrubState = rememberUpdatedState(onScrub)
    val scrub = Modifier.pointerInput(saku.months) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            scrubState.value(true)

            var lastData: List<Pair<Calendar, Long>>? = null
            var lastIndex = -1
            fun apply(x: Float) {
                val d = dataState.value
                if (d.isEmpty() || size.width <= 0) return
                if (d !== lastData) {
                    lastData = d
                    lastIndex = -1
                }
                val i = (x / size.width.toFloat() * d.size).toInt().coerceIn(0, d.lastIndex)
                if (i == lastIndex) return
                lastIndex = i
                pickState.value(d[i].first)
            }

            try {
                apply(down.position.x)
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    change.consume()
                    apply(change.position.x)
                }
            } finally {
                scrubState.value(false)
            }
        }
    }

    Column(Modifier.fillMaxWidth().topHair(t.hairSoft).padding(top = 7.dp, bottom = 10.dp)) {
        MonthNav(t, cursor, onCursor)
        Spacer(Modifier.height(8.dp))

        // Grafik dan strip di bawahnya berbagi handler yang sama: dua ruang gestur
        // khusus, tidak ada lagi "geser ke kanan malah pindah ke Transaksi".
        Box(Modifier.fillMaxWidth().height(46.dp).then(scrub)) {
            if (saku.chart == "garis") {
                LineChart(
                    t = t,
                    data = data,
                    max = max,
                    activeIndex = activeIndex,
                    modifier = Modifier.fillMaxSize(),
                    scale = scale
                )
            } else {
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEachIndexed { i, (_, total) ->
                        val target = if (total <= 0L) 0f else (total.toFloat() / max.toFloat()).coerceIn(0.02f, 1f)
                        val frac by animateFloatAsState(
                            targetValue = target,
                            animationSpec = tween(
                                Motion.d(320, scale),
                                delayMillis = Motion.d(i * 18, scale),
                                easing = FastOutSlowInEasing
                            ),
                            label = "batang$i"
                        )
                        val active = i == activeIndex
                        Box(
                            Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                Modifier
                                    .width(barW)
                                    .height(if (total <= 0L) 2.dp else (46f * frac).dp.coerceAtLeast(4.dp))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                    .background(if (active) t.accent else t.bar)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            data.forEachIndexed { i, (cal, _) ->
                val active = i == activeIndex
                Text(
                    Fmt.monthShort(cal),
                    Type.micro.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
                    if (active) t.ink else t.muted2,
                    Modifier.weight(1f),
                    align = TextAlign.Center
                )
            }
        }

        // V1.4 #1/#9: strip slider 26dp, caption "Tertinggi …", dan petunjuk
        // "tahan & geser" dihapus. Grafiknya sendiri adalah ruang gestur.
    }
}

@Composable
private fun LineChart(
    t: Tok,
    data: List<Pair<Calendar, Long>>,
    max: Long,
    activeIndex: Int,
    modifier: Modifier,
    scale: Float
) {
    val reveal = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        if (scale <= 0f) reveal.snapTo(1f) else reveal.animateTo(1f, tween(Motion.d(420, scale), easing = FastOutSlowInEasing))
    }
    val p = reveal.value
    val n = data.size

    // Tidak ada pointerInput di sini: ketuk dan geser ditangani Box induk,
    // sehingga grafik batang dan garis punya perilaku scrub yang identik.
    Canvas(modifier) {
        val top = 4.dp.toPx()
        val bottom = size.height
        val usable = bottom - top
        val step = if (n <= 1) size.width else size.width / (n - 1)
        fun point(i: Int): Offset {
            val v = data[i].second.toFloat() / max.toFloat()
            return Offset(i * step, bottom - v.coerceIn(0f, 1f) * usable)
        }

        drawLine(t.hair, Offset(0f, bottom - 0.5f), Offset(size.width, bottom - 0.5f), 1.dp.toPx())

        for (i in 0 until n - 1) {
            val seg = (p * (n - 1) - i).coerceIn(0f, 1f)
            if (seg <= 0f) break
            val a = point(i)
            val b = point(i + 1)
            val end = Offset(a.x + (b.x - a.x) * seg, a.y + (b.y - a.y) * seg)
            drawLine(
                color = if (i == activeIndex) t.accent else t.muted2,
                start = a,
                end = end,
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        for (i in 0 until n) {
            val shown = p * (n - 1) >= i - 0.001f
            if (!shown) break
            val pt = point(i)
            if (i == activeIndex) {
                drawCircle(t.accent, 4.dp.toPx(), pt)
                drawCircle(t.sheet, 1.8.dp.toPx(), pt)
            } else {
                drawCircle(t.bg, 2.6.dp.toPx(), pt)
                drawCircle(t.muted2, 2.6.dp.toPx(), pt, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
            }
        }
    }
}

@Composable
private fun CatSection(t: Tok, saku: Saku, cursor: Calendar, onCatat: () -> Unit) {
    val scale = motionScale()
    val spent = saku.spendingByCat(cursor)

    Column(Modifier.fillMaxWidth().topHair(t.hairSoft).padding(top = 10.dp, bottom = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Spending per kategori", Type.section, t.ink)
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    spent.isEmpty() -> "\u2014"
                    spent.size > 4 -> "4 dari ${spent.size} kategori"
                    else -> "${spent.size} kategori"
                },
                Type.micro,
                t.muted2
            )
        }

        if (spent.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Belum ada pengeluaran bulan ini.", Type.label, t.muted2)
                Spacer(Modifier.weight(1f))
                Text("Catat", Type.label.copy(fontWeight = FontWeight.SemiBold), t.accent, Modifier.tap(onCatat))
            }
            return@Column
        }

        val top = spent.take(4)
        val total = top.sumOf { it.second }.coerceAtLeast(1L)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            top.forEachIndexed { i, (_, amount) ->
                val frac by animateFloatAsState(
                    targetValue = amount.toFloat() / total.toFloat(),
                    animationSpec = tween(Motion.d(320, scale), delayMillis = Motion.d(i * 40, scale)),
                    label = "kategori$i"
                )
                Box(
                    Modifier
                        .weight(frac.coerceAtLeast(0.001f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(t.tint[i % t.tint.size])
                )
            }
        }

        Spacer(Modifier.height(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            top.chunked(2).forEachIndexed { rowIdx, pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    pair.forEachIndexed { colIdx, (cat, amount) ->
                        val i = rowIdx * 2 + colIdx
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(6.dp).clip(RoundedCornerShape(2.dp))
                                    .background(t.tint[i % t.tint.size])
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(cat.name, Type.figure, t.muted2, Modifier.weight(1f))
                            Text(Fmt.num(amount), Type.figure, t.ink)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * V1.4 #10 — tampilan Nabung di Home punya opsi sendiri:
 * tampil/sembunyi, ikut bergulir bersama target lain, dan target yang di-pin.
 */
@Composable
private fun NabungRow(t: Tok, saku: Saku, onNabung: () -> Unit) {
    if (!saku.showNabungHome) return
    val savedByGoal = remember(saku.tx) { saku.savedMap() }
    val goals = saku.goals
    val pinned = saku.goalOf(saku.nabungPinned) ?: goals.maxByOrNull { savedByGoal[it.id] ?: 0L }

    Column(Modifier.fillMaxWidth().topHair(t.hairSoft).padding(vertical = 10.dp)) {
        if (saku.nabungScroll && goals.isNotEmpty()) {
            LazyRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(goals, key = { it.id }) { g ->
                    GoalChip(t, savedByGoal, g, onNabung)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().tap(onNabung), verticalAlignment = Alignment.CenterVertically) {
                // V1.5 #5: kalau target punya gambar, tampilkan di sini.
                if (pinned?.image?.isNotEmpty() == true) {
                    GoalImage(pinned.image, t, 26.dp)
                } else {
                    Box(
                        Modifier.size(26.dp).clip(RoundedCornerShape(9.dp)).background(t.accentWash),
                        contentAlignment = Alignment.Center
                    ) {
                        Glyph(Ic.Wallet, t.accent, 15.dp)
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    if (pinned == null) {
                        Text("Nabung", Type.bodySm.copy(fontWeight = FontWeight.SemiBold), t.ink)
                        Text(
                            "Belum ada target. Buat yang pertama.",
                            Type.micro,
                            t.muted2,
                            Modifier.padding(top = 2.dp)
                        )
                    } else {
                        val saved = savedByGoal[pinned.id] ?: 0L
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (saku.nabungPinned.isNotEmpty()) {
                                Glyph(Ic.Pin, t.accent, 12.dp, 1.4.dp)
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                pinned.name,
                                Type.bodySm.copy(fontWeight = FontWeight.SemiBold),
                                t.ink,
                                Modifier.weight(1f)
                            )
                            Text(Fmt.num(saved) + " / " + Fmt.num(pinned.target), Type.figure, t.muted2)
                        }
                        Spacer(Modifier.height(6.dp))
                        Track(t, if (pinned.target > 0) saved.toFloat() / pinned.target.toFloat() else 0f)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalChip(
    t: Tok,
    savedByGoal: Map<String, Long>,
    goal: Goal,
    onNabung: () -> Unit
) {
    val saved = savedByGoal[goal.id] ?: 0L
    Column(
        Modifier
            .width(186.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, t.hairSoft, RoundedCornerShape(12.dp))
            .tap(onNabung)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoalImage(goal.image, t, 30.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                goal.name,
                Type.bodySm.copy(fontWeight = FontWeight.SemiBold),
                t.ink,
                Modifier.weight(1f),
                maxLines = 1
            )
        }
        Spacer(Modifier.height(7.dp))
        Track(t, if (goal.target > 0) saved.toFloat() / goal.target.toFloat() else 0f)
        Spacer(Modifier.height(6.dp))
        Text(Fmt.num(saved) + " / " + Fmt.num(goal.target), Type.micro, t.muted2)
    }
}

@Composable
private fun ListSection(
    t: Tok,
    saku: Saku,
    cursor: Calendar,
    rows: List<Tx>,
    running: List<Long>,
    flashId: String?,
    onCatat: () -> Unit,
    onOpen: (Tx) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().topHair(t.hairSoft).padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Transaksi", Type.section, t.ink)
            Spacer(Modifier.weight(1f))
            Text(if (rows.isEmpty()) "belum ada" else "${rows.size} transaksi", Type.micro, t.muted2)
        }
        Spacer(Modifier.height(6.dp))
        TableHead(t)

        val listState = rememberLazyListState()
        val monthKey = cursor.get(Calendar.YEAR) * 100 + cursor.get(Calendar.MONTH)
        LaunchedEffect(monthKey) { listState.scrollToItem(0) }

        // V1.4 #1: LazyColumn dibuat sekali saja. Dulu AnimatedContent yang berganti
        // kunci setiap bulan membuat seluruh daftar dibongkar — transaksi sempat
        // kosong dan gerakan scrub terasa ngelag. Animasi masuk kini per baris.
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            state = listState,
            contentPadding = PaddingValues(bottom = 104.dp)
        ) {
            if (rows.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 26.dp)) {
                        Text("Belum ada transaksi bulan ini.", Type.body, t.ink)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Catat yang pertama.",
                            Type.label.copy(fontWeight = FontWeight.SemiBold),
                            t.accent,
                            Modifier.tap(onCatat)
                        )
                    }
                }
            }
            itemsIndexed(rows, key = { _, tx -> tx.id }) { i, tx ->
                Box(Modifier.animateItem().rowEnter(i, monthKey)) {
                    TxRow(
                        t = t,
                        saku = saku,
                        tx = tx,
                        saldo = running.getOrElse(i) { 0L },
                        newest = i == 0,
                        flash = tx.id == flashId
                    ) { onOpen(tx) }
                }
            }
        }
    }
}

@Composable
fun TableHead(t: Tok) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Tgl", Type.colHead, t.muted2, Modifier.width(40.dp))
        Text("Keterangan", Type.colHead, t.muted2, Modifier.weight(1f))
        Text("Jumlah", Type.colHead, t.muted2, Modifier.width(82.dp), align = TextAlign.End)
        Text("Saldo", Type.colHead, t.muted2, Modifier.width(76.dp), align = TextAlign.End)
    }
    Hair(t.hair)
}

@Composable
fun TxRow(t: Tok, saku: Saku, tx: Tx, saldo: Long, newest: Boolean, flash: Boolean, onClick: () -> Unit) {
    val cat = saku.catOf(tx.cat)
    val catName = cat?.name ?: "Lainnya"
    val note = tx.note.ifBlank { catName }
    // V1.5 #4: glyph kategori + penanda lampiran/target, tanpa mengubah grid
    // 4 kolom (Tgl / Keterangan / Jumlah / Saldo) yang jadi ciri aplikasi.
    val photoCount = tx.allPhotos.size
    val hasGoal = tx.goal.isNotEmpty()
    val desc = Fmt.dayLabel(tx.date) + ", " + note + ", " +
        (if (tx.type == "in") "masuk " else "keluar ") + Fmt.num(tx.amount) +
        ", saldo " + Fmt.num(saldo) +
        (if (photoCount > 0) ", " + photoCount + " foto" else "") +
        (if (hasGoal) ", target nabung" else "")

    // kilatan singkat saat baris baru ditambah/diubah
    val flashAnim = remember { Animatable(0f) }
    LaunchedEffect(flash) {
        if (flash) {
            flashAnim.snapTo(1f)
            flashAnim.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(t.accent.copy(alpha = 0.16f * flashAnim.value))
            .semantics(mergeDescendants = true) { contentDescription = desc }
    ) {
        Row(
            Modifier.fillMaxWidth().height(49.dp).tap(onClick),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.width(40.dp)) {
                Text(Fmt.tgl(tx.date), Type.label, t.muted2)
                Text(
                    Fmt.weekday(tx.date).take(3),
                    Type.micro,
                    t.faint,
                    Modifier.padding(top = 1.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(note, Type.body, t.ink, maxLines = 1)
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Glyph(glyphOf(cat?.glyph ?: "dot"), t.muted2, 11.dp, 1.2.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(catName, Type.micro, t.muted2, Modifier.weight(1f, fill = false), maxLines = 1)
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(2.5.dp)
                            .clip(CircleShape)
                            .background(t.muted2.copy(alpha = 0.55f))
                    )
                    Text(tx.time, Type.micro, t.muted2)
                    if (photoCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Glyph(Ic.Image, t.accent, 10.dp, 1.2.dp)
                    }
                    if (hasGoal) {
                        Spacer(Modifier.width(3.dp))
                        Glyph(Ic.Pin, t.accent, 10.dp, 1.2.dp)
                    }
                }
            }
            Text(
                (if (tx.type == "in") "+" else "\u2212") + Fmt.num(tx.amount),
                Type.amount,
                if (tx.type == "in") t.pos else t.error,
                Modifier.width(82.dp),
                align = TextAlign.End
            )
            Text(
                Fmt.num(saldo),
                if (newest) Type.figure.copy(fontWeight = FontWeight.SemiBold) else Type.figure,
                if (newest) t.accent else t.muted2,
                Modifier.width(76.dp),
                align = TextAlign.End
            )
        }
        Hair(t.hairSoft)
    }
}
