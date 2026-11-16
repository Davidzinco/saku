package id.saku.app

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onCursor: (Calendar) -> Unit,
    onCatat: () -> Unit,
    onNabung: () -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    val rows = saku.monthTxs(cursor)
    val running = saku.runningBalances(cursor, rows)

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SAKU", Type.wordmark, t.ink)
            Spacer(Modifier.weight(1f))
            Text(
                "Lembar ${cursor.get(Calendar.MONTH) + 1} / ${cursor.get(Calendar.YEAR)}",
                Type.micro,
                t.muted2
            )
        }

        SaldoBlock(t, saku.balance(), saku.income(cursor), saku.outcome(cursor), fontScale)
        TrendSection(t, saku, cursor, onCursor)
        CatSection(t, saku, cursor, onCatat)
        NabungRow(t, saku, onNabung)
        ListSection(t, saku, rows, running, onCatat, Modifier.weight(1f).padding(top = 2.dp))
    }
}

@Composable
private fun SaldoBlock(t: Tok, saldo: Long, income: Long, outcome: Long, fontScale: Float) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 11.dp)) {
        Text("Saldo akhir", Type.label, t.muted2)
        Row(Modifier.padding(top = 3.dp)) {
            Text("Rp", Type.currency, t.muted2, Modifier.alignByBaseline())
            Spacer(Modifier.width(9.dp))
            Text(Fmt.num(saldo), Type.hero(fontScale), t.ink, Modifier.alignByBaseline())
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            Stat(t, "Masuk", Fmt.sign(income), if (income > 0) t.pos else t.ink)
            Stat(t, "Keluar", Fmt.out(outcome), t.ink)
            val diff = income - outcome
            Stat(t, "Selisih", Fmt.sign(diff), if (diff > 0) t.pos else t.ink)
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

/** Baris navigasi bulan yang sama dipakai Beranda dan Transaksi. */
@Composable
fun MonthNav(t: Tok, cursor: Calendar, onCursor: (Calendar) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NavCircle(t, Ic.ChevL, "Bulan sebelumnya") { onCursor(shiftMonth(cursor, -1)) }
        Text(
            Fmt.monthLabel(cursor),
            Type.section,
            t.ink,
            Modifier.weight(1f).padding(horizontal = 8.dp),
            align = TextAlign.Center
        )
        NavCircle(t, Ic.ChevR, "Bulan berikutnya") { onCursor(shiftMonth(cursor, 1)) }
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
private fun TrendSection(t: Tok, saku: Saku, cursor: Calendar, onCursor: (Calendar) -> Unit) {
    val data = saku.trend(6, cursor)
    val max = data.maxOfOrNull { it.second } ?: 0L
    val peak = data.maxByOrNull { it.second }

    Column(Modifier.fillMaxWidth().topHair(t.hairSoft).padding(top = 7.dp, bottom = 10.dp)) {
        MonthNav(t, cursor, onCursor)

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().height(46.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { (cal, total) ->
                val active = cal.get(Calendar.YEAR) == cursor.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == cursor.get(Calendar.MONTH)
                val frac = if (max <= 0L) 0f else total.toFloat() / max.toFloat()
                val h = (46f * frac).dp.coerceAtLeast(if (total > 0L) 4.dp else 2.dp)

                Column(
                    Modifier.weight(1f).fillMaxHeight().tap { onCursor(cal) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        Modifier
                            .width(24.dp)
                            .height(h)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(if (active) t.accent else t.bar)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            data.forEach { (cal, _) ->
                val active = cal.get(Calendar.YEAR) == cursor.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == cursor.get(Calendar.MONTH)
                Text(
                    Fmt.monthShort(cal),
                    Type.micro.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
                    if (active) t.ink else t.muted2,
                    Modifier.weight(1f),
                    align = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        val caption = if (peak != null && peak.second > 0L) {
            "Tertinggi " + Fmt.monthName(peak.first)
        } else {
            "Belum ada pengeluaran di 6 bulan ini"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(caption, Type.micro, t.muted2)
            if (peak != null && peak.second > 0L) {
                Text(", ", Type.micro, t.muted2)
                Text(Fmt.rp(peak.second), Type.micro.copy(fontWeight = FontWeight.SemiBold), t.muted2)
            }
        }
    }
}

@Composable
private fun CatSection(t: Tok, saku: Saku, cursor: Calendar, onCatat: () -> Unit) {
    val spent = saku.spendingByCat(cursor)

    Column(Modifier.fillMaxWidth().topHair(t.hairSoft).padding(top = 10.dp, bottom = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Pengeluaran per kategori", Type.section, t.ink)
            Spacer(Modifier.weight(1f))
            Text(if (spent.isEmpty()) "\u2014" else "${spent.size} kategori", Type.micro, t.muted2)
        }

        if (spent.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Belum ada pengeluaran bulan ini.", Type.label, t.muted2)
                Spacer(Modifier.weight(1f))
                Text(
                    "Catat",
                    Type.label.copy(fontWeight = FontWeight.SemiBold),
                    t.accent,
                    Modifier.tap(onCatat)
                )
            }
            return@Column
        }

        val top = spent.take(4)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            top.forEachIndexed { i, (_, amount) ->
                Box(
                    Modifier
                        .weight(amount.toFloat())
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
                                Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(2.dp))
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

@Composable
private fun NabungRow(t: Tok, saku: Saku, onNabung: () -> Unit) {
    val goal = saku.goals.maxByOrNull { it.saved }
    Column(Modifier.fillMaxWidth().topHair(t.hairSoft).padding(vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().tap(onNabung),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(26.dp).clip(RoundedCornerShape(9.dp)).background(t.accentWash),
                contentAlignment = Alignment.Center
            ) {
                Glyph(Ic.Wallet, t.accent, 15.dp)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                if (goal == null) {
                    Text("Nabung", Type.bodySm.copy(fontWeight = FontWeight.SemiBold), t.ink)
                    Text("Belum ada target. Buat yang pertama.", Type.micro, t.muted2, Modifier.padding(top = 2.dp))
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            goal.name,
                            Type.bodySm.copy(fontWeight = FontWeight.SemiBold),
                            t.ink,
                            Modifier.weight(1f)
                        )
                        Text(
                            Fmt.num(goal.saved) + " / " + Fmt.num(goal.target),
                            Type.figure,
                            t.muted2
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Track(t, if (goal.target > 0) goal.saved.toFloat() / goal.target.toFloat() else 0f)
                }
            }
        }
    }
}

@Composable
private fun ListSection(
    t: Tok,
    saku: Saku,
    rows: List<Tx>,
    running: List<Long>,
    onCatat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().topHair(t.hairSoft).padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Transaksi", Type.section, t.ink)
            Spacer(Modifier.weight(1f))
            Text(if (rows.isEmpty()) "belum ada" else "${rows.size} terakhir", Type.micro, t.muted2)
        }
        Spacer(Modifier.height(6.dp))
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
fun TxRow(t: Tok, saku: Saku, tx: Tx, saldo: Long, newest: Boolean) {
    val catName = saku.catOf(tx.cat)?.name ?: "Lainnya"
    val note = tx.note.ifBlank { catName }
    val desc = Fmt.dayLabel(tx.date) + ", " + note + ", " +
        (if (tx.type == "in") "masuk " else "keluar ") + Fmt.num(tx.amount) +
        ", saldo " + Fmt.num(saldo)

    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = desc }) {
        Row(
            Modifier.fillMaxWidth().height(49.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Fmt.tgl(tx.date), Type.label, t.muted2, Modifier.width(40.dp))
            Column(Modifier.weight(1f)) {
                Text(note, Type.body, t.ink)
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(catName, Type.micro, t.muted2)
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(2.5.dp)
                            .clip(CircleShape)
                            .background(t.muted2.copy(alpha = 0.55f))
                    )
                    Text(tx.time, Type.micro, t.muted2)
                }
            }
            Text(
                (if (tx.type == "in") "+" else "\u2212") + Fmt.num(tx.amount),
                Type.amount,
                if (tx.type == "in") t.pos else t.ink,
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
