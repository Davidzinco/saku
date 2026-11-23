package id.saku.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

internal fun nextCompactBarState(
    currentlyCompact: Boolean,
    firstVisibleIndex: Int,
    firstVisibleOffsetPx: Int,
    enterIndex: Int = 1,
    enterOffsetPx: Int = 140,
    exitOffsetPx: Int = 48
): Boolean {
    require(enterOffsetPx > exitOffsetPx) { "enterOffsetPx must exceed exitOffsetPx" }
    return if (currentlyCompact) {
        !(firstVisibleIndex == 0 && firstVisibleOffsetPx <= exitOffsetPx)
    } else {
        firstVisibleIndex >= enterIndex || (firstVisibleIndex == 0 && firstVisibleOffsetPx >= enterOffsetPx)
    }
}

/**
 * Halaman Transaksi (M1):
 * - Kartu saldo utama berada di dalam `LazyColumn` (`item("hero")`) dan ringkasan
 *   compact sticky dirender sebagai overlay di atas `LazyColumn`, sehingga tinggi
 *   viewport `LazyColumn` konstan (bebas scroll-bounce).
 * - Filter sumber dana (Semua / Tunai / E-Wallet / Custom) saat multi-sumber aktif.
 * - Grafik tren interaktif + ringkasan statistik + tabel 4 kolom Buku Tabungan.
 */
@Composable
fun Transaksi(
    saku: Saku,
    t: Tok,
    cursor: Calendar,
    flashId: String?,
    onCursor: (Calendar) -> Unit,
    onCatat: () -> Unit,
    onOpen: (Tx) -> Unit,
    onToggleHide: () -> Unit = {},
    onScrub: (Boolean) -> Unit = {}
) {
    var accountFilter by remember { mutableStateOf<String?>(null) }
    // Pastikan filter tetap valid kalau sumber dana dihapus di Pengaturan
    val activeFilter = if (saku.multiAccount && accountFilter != null && saku.accounts.any { it.id == accountFilter }) {
        accountFilter
    } else null

    val monthKey = cursor.get(Calendar.YEAR) * 100 + cursor.get(Calendar.MONTH)
    val snap = remember(saku.tx, saku.cats, monthKey, activeFilter) {
        saku.monthSnapshot(cursor, activeFilter)
    }
    val totalBalance = remember(saku.tx, saku.accounts, activeFilter) {
        if (activeFilter == null) saku.balance() else (saku.accountBalances()[activeFilter] ?: 0L)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(monthKey, activeFilter) { listState.scrollToItem(0) }

    val density = LocalDensity.current
    val fontScale = density.fontScale
    val scale = motionScale()
    val enterOffsetPx = remember(density) { with(density) { 88.dp.roundToPx() } }
    val exitOffsetPx = remember(density) { with(density) { 40.dp.roundToPx() } }
    var showCompactBar by remember { mutableStateOf(false) }

    LaunchedEffect(listState, enterOffsetPx, exitOffsetPx) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val next = nextCompactBarState(
                currentlyCompact = showCompactBar,
                firstVisibleIndex = index,
                firstVisibleOffsetPx = offset,
                enterIndex = 1,
                enterOffsetPx = enterOffsetPx,
                exitOffsetPx = exitOffsetPx
            )
            if (next != showCompactBar) showCompactBar = next
        }
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val compactProgress by animateFloatAsState(
        targetValue = if (showCompactBar) 1f else 0f,
        animationSpec = tween(
            durationMillis = Motion.d(if (showCompactBar) Motion.ENTER else 140, scale),
            easing = if (showCompactBar) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "compactSaldo"
    )

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(
            Modifier.fillMaxWidth().height(46.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transaksi", Type.title, t.ink, Modifier.weight(1f))
            EyeToggleButton(t, saku.hideBalance, onToggleHide)
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(top = 6.dp, bottom = 124.dp + navBottom)
            ) {
                item(key = "hero") {
                    val heroShape = RoundedCornerShape(10.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(heroShape)
                            .background(t.sheet)
                            .border(1.dp, t.hair, heroShape)
                            .padding(16.dp)
                    ) {
                        Text(
                            if (activeFilter == null) "Saldo total" else "Saldo ${saku.accountOf(activeFilter)?.name ?: ""}",
                            Type.micro,
                            t.muted2
                        )
                        AnimatedHeroAmount(t, totalBalance, saku.hideBalance, fontScale)
                        Spacer(Modifier.height(12.dp))
                        Hair(t.hairSoft)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            AnimatedStat(t, "Masuk", snap.income, "+", if (snap.income > 0) t.pos else t.ink, saku.hideBalance)
                            AnimatedStat(t, "Keluar", snap.outcome, "\u2212", if (snap.outcome > 0) t.error else t.ink, saku.hideBalance)
                            AnimatedStat(
                                t,
                                "Selisih",
                                snap.diff,
                                null,
                                if (snap.diff > 0) t.pos else if (snap.diff < 0) t.error else t.ink,
                                saku.hideBalance
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // Filter sumber dana bila multi-sumber aktif
                if (saku.multiAccount && saku.accounts.size > 1) {
                    item(key = "filters") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(t, "Semua", activeFilter == null) { accountFilter = null }
                            saku.accounts.forEach { acc ->
                                FilterChip(t, acc.name, activeFilter == acc.id) { accountFilter = acc.id }
                            }
                        }
                    }
                }

                // Grafik tren bulanan (terutama berguna saat Beranda memakai tampilan baru)
                item(key = "trend") {
                    val trendShape = RoundedCornerShape(10.dp)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(trendShape)
                            .background(t.sheet)
                            .border(1.dp, t.hairSoft, trendShape)
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        TrendSection(t, saku, cursor, onCursor, onScrub, showTopHair = false)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Statistik turunan
                item(key = "stats") {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        Stat(t, "Rata-rata keluar", Fmt.rpMasked(snap.avgOut, saku.hideBalance), t.ink)
                        Stat(t, "Hari aktif", "${snap.activeDays} hari", t.ink)
                        Stat(t, "Catatan", "${snap.rows.size} trx", t.ink)
                    }
                }

                item(key = "table_head") {
                    TableHead(t)
                }

                if (snap.rows.isEmpty()) {
                    item(key = "empty") {
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

                itemsIndexed(snap.rows, key = { _, tx -> tx.id }) { i, tx ->
                    Box(Modifier.rowEnter(i, monthKey)) {
                        TxRow(
                            t = t,
                            saku = saku,
                            tx = tx,
                            saldo = snap.running.getOrElse(i) { 0L },
                            newest = i == 0,
                            flash = tx.id == flashId,
                            hideBalance = saku.hideBalance
                        ) { onOpen(tx) }
                    }
                }
            }

            if (showCompactBar || compactProgress > 0.01f) {
                val compactShape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = compactProgress
                            translationY = (1f - compactProgress) * -12.dp.toPx()
                        }
                        .background(t.bg)
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(compactShape)
                            .background(t.sheet)
                            .border(1.dp, t.hair, compactShape)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (activeFilter == null) "Saldo total" else "Saldo ${saku.accountOf(activeFilter)?.name ?: ""}",
                                Type.micro,
                                t.muted2
                            )
                            Text(
                                Fmt.rpMasked(totalBalance, saku.hideBalance),
                                Type.title.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                                t.ink,
                                Modifier.padding(top = 1.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Fmt.monthShort(cursor) + " " + cursor.get(Calendar.YEAR), Type.micro, t.muted2)
                            Text(
                                if (saku.hideBalance) "\u2022\u2022\u2022\u2022" else Fmt.out(snap.outcome),
                                Type.figure.copy(fontWeight = FontWeight.SemiBold),
                                if (snap.outcome > 0 && !saku.hideBalance) t.error else t.ink,
                                Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(t: Tok, label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .tap(onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .clip(shape)
                .background(if (selected) t.ink else t.sheet)
                .border(1.dp, if (selected) t.ink else t.hair, shape)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                label,
                Type.micro.copy(fontWeight = FontWeight.SemiBold),
                if (selected) t.bg else t.muted
            )
        }
    }
}
