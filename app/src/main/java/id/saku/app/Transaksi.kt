package id.saku.app

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

/** Daftar transaksi satu bulan — grammar yang sama dengan Beranda: judul, tabel, garis rambut. */
@Composable
fun Transaksi(
    saku: Saku,
    t: Tok,
    cursor: Calendar,
    onCursor: (Calendar) -> Unit,
    onCatat: () -> Unit
) {
    val rows = saku.monthTxs(cursor)
    val running = saku.runningBalances(cursor, rows)
    val income = saku.income(cursor)
    val outcome = saku.outcome(cursor)
    val diff = income - outcome

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Text("Transaksi", Type.title, t.ink, Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(14.dp))
        MonthNav(t, cursor, onCursor)
        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            Stat(t, "Masuk", Fmt.sign(income), if (income > 0) t.pos else t.ink)
            Stat(t, "Keluar", Fmt.out(outcome), t.ink)
            Stat(t, "Selisih", Fmt.sign(diff), if (diff > 0) t.pos else t.ink)
        }

        Spacer(Modifier.height(16.dp))
        TableHead(t)

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 104.dp)) {
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
            itemsIndexed(rows) { i, tx ->
                TxRow(t, saku, tx, running.getOrElse(i) { 0L }, newest = i == 0)
            }
        }
    }
}
