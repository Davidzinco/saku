package id.saku.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

/** Daftar transaksi satu bulan — grammar yang sama dengan Home: judul, tabel, garis rambut. */
@Composable
fun Transaksi(
    saku: Saku,
    t: Tok,
    cursor: Calendar,
    flashId: String?,
    onCursor: (Calendar) -> Unit,
    onCatat: () -> Unit,
    onOpen: (Tx) -> Unit
) {
    val rows = saku.monthTxs(cursor)
    val running = saku.runningBalances(cursor, rows)
    val income = saku.income(cursor)
    val outcome = saku.outcome(cursor)
    val diff = income - outcome

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Transaksi", back = false)
        Spacer(Modifier.height(12.dp))
        MonthNav(t, cursor, onCursor)
        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            Stat(t, "Masuk", Fmt.sign(animatedLong(income, 360)), if (income > 0) t.pos else t.ink)
            Stat(t, "Keluar", Fmt.out(animatedLong(outcome, 360)), if (outcome > 0) t.error else t.ink)
            Stat(t, "Selisih", Fmt.sign(animatedLong(diff, 360)), if (diff > 0) t.pos else if (diff < 0) t.error else t.ink)
        }

        Spacer(Modifier.height(16.dp))
        TableHead(t)

        val listState = rememberLazyListState()
        val monthKey = cursor.get(Calendar.YEAR) * 100 + cursor.get(Calendar.MONTH)
        LaunchedEffect(monthKey) { listState.scrollToItem(0) }

        // V1.4 #1: daftar dibuat sekali; animasi masuk per baris saat bulan berganti.
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
