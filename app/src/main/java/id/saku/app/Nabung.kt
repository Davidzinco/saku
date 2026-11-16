package id.saku.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Locale

@Composable
fun Nabung(saku: Saku, t: Tok, onChange: (Saku) -> Unit, onSheet: (GoalSheet) -> Unit) {
    val savedTotal = saku.savedTotal()
    val targetTotal = saku.goals.sumOf { it.target }
    val pct = if (targetTotal > 0) (savedTotal * 100 / targetTotal).toInt() else 0
    val shownSaved = animatedLong(savedTotal)

    val now = Calendar.getInstance()
    val prefix = String.format(Locale.US, "%04d-%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    val thisMonth = saku.tx.filter { it.goal.isNotEmpty() && it.date.startsWith(prefix) }.sumOf { -it.signed }

    var newName by remember { mutableStateOf("") }
    var newTarget by remember { mutableStateOf("") }
    val targetValue = newTarget.toLongOrNull() ?: 0L
    val canSave = newName.isNotBlank() && targetValue > 0L

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Nabung", back = false)
        Spacer(Modifier.height(14.dp))

        Text("Tersimpan", Type.label, t.muted2)
        Text(Fmt.num(shownSaved), Type.display, t.ink, Modifier.padding(top = 3.dp))
        Text(
            if (targetTotal > 0) "dari " + Fmt.rp(targetTotal) + " \u00b7 " + pct + "%" else "Belum ada target",
            Type.label,
            t.muted2,
            Modifier.padding(top = 2.dp)
        )

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            Stat(t, "Saldo akhir", Fmt.rp(saku.balance()), t.ink)
            Stat(t, "Setoran bulan ini", Fmt.sign(thisMonth), if (thisMonth > 0) t.pos else t.ink)
        }

        Spacer(Modifier.height(14.dp))
        Hair(t.hair)

        if (saku.goals.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp)) {
                Text("Belum ada target nabung.", Type.body, t.ink)
                Text(
                    "Setoran dicatat sebagai transaksi, jadi saldo selalu sinkron.",
                    Type.label,
                    t.muted2,
                    Modifier.padding(top = 6.dp),
                    maxLines = 3
                )
            }
        }

        saku.goals.forEach { goal ->
            val saved = saku.savedOf(goal.id)
            val frac = if (goal.target > 0) saved.toFloat() / goal.target.toFloat() else 0f
            val goalPct = (frac * 100).toInt().coerceIn(0, 100)

            Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        goal.name,
                        Type.body.copy(fontWeight = FontWeight.SemiBold),
                        t.ink,
                        Modifier.weight(1f),
                        maxLines = 2
                    )
                    Text(goalPct.toString() + "%", Type.figure, t.muted2)
                }
                Spacer(Modifier.height(8.dp))
                Track(t, frac)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Fmt.num(saved) + " / " + Fmt.num(goal.target),
                        Type.figure,
                        t.muted2,
                        Modifier.weight(1f)
                    )
                    Text(
                        "Setor",
                        Type.label.copy(fontWeight = FontWeight.SemiBold),
                        t.accent,
                        Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                            .tap { onSheet(GoalSheet.Setor(goal)) }
                    )
                    Text(
                        "Tarik",
                        Type.label.copy(fontWeight = FontWeight.SemiBold),
                        if (saved > 0) t.muted else t.faint,
                        Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                            .tap { if (saved > 0) onSheet(GoalSheet.Tarik(goal)) }
                    )
                    TouchTarget {
                        Box(
                            Modifier.fillMaxSize().tap { onSheet(GoalSheet.Ubah(goal)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Glyph(Ic.Pencil, t.muted2, 15.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Hair(t.hairSoft)
        }

        Spacer(Modifier.height(22.dp))
        Text("Target baru", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))

        FieldShell(t) {
            PlainField(
                value = newName,
                onValueChange = { newName = it.take(24) },
                placeholder = "Nama target",
                t = t,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        Spacer(Modifier.height(8.dp))
        FieldShell(t) {
            Text("Rp", Type.currency, t.muted2)
            Spacer(Modifier.width(8.dp))
            AmountField(
                digits = newTarget,
                onDigits = { newTarget = it },
                t = t,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                style = Type.body
            )
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Simpan target", t, enabled = canSave) {
            onChange(
                saku.copy(
                    goals = saku.goals + Goal(
                        id = "g" + System.nanoTime().toString(36),
                        name = newName.trim(),
                        target = targetValue
                    )
                )
            )
            newName = ""
            newTarget = ""
        }
        Spacer(Modifier.height(110.dp))
    }
}
