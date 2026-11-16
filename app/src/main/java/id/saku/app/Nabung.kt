package id.saku.app

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun Nabung(saku: Saku, t: Tok, onChange: (Saku) -> Unit) {
    val saved = saku.goals.sumOf { it.saved }
    val target = saku.goals.sumOf { it.target }
    val pct = if (target > 0) (saved * 100 / target).toInt() else 0

    var newName by remember { mutableStateOf("") }
    var newTarget by remember { mutableStateOf("") }
    val targetValue = newTarget.toLongOrNull() ?: 0L
    val canSave = newName.isNotBlank() && targetValue > 0L
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Text("Nabung", Type.title, t.ink, Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(18.dp))

        Text("Tersimpan", Type.label, t.muted2)
        Text(Fmt.num(saved), Type.display, t.ink, Modifier.padding(top = 3.dp))
        Text(
            if (target > 0) "dari " + Fmt.rp(target) + " \u00b7 " + pct + "%" else "Belum ada target",
            Type.label,
            t.muted2,
            Modifier.padding(top = 2.dp)
        )

        Spacer(Modifier.height(14.dp))
        Hair(t.hair)

        if (saku.goals.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp)) {
                Text("Belum ada target nabung.", Type.body, t.ink)
                Text("Beri nama target pertama di bawah.", Type.label, t.muted2, Modifier.padding(top = 6.dp))
            }
        }

        saku.goals.forEach { goal ->
            val frac = if (goal.target > 0) goal.saved.toFloat() / goal.target.toFloat() else 0f
            val goalPct = if (goal.target > 0) (goal.saved * 100 / goal.target).coerceIn(0, 100).toInt() else 0
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
                        Fmt.num(goal.saved) + " / " + Fmt.num(goal.target),
                        Type.figure,
                        t.muted2,
                        Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .height(44.dp)
                            .padding(horizontal = 10.dp)
                            .tap {
                                onChange(
                                    saku.copy(
                                        goals = saku.goals.map {
                                            if (it.id == goal.id) it.copy(saved = it.saved + 100_000L) else it
                                        }
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+ 100rb",
                            Type.label.copy(fontWeight = FontWeight.SemiBold),
                            t.accent
                        )
                    }
                    TouchTarget {
                        Box(
                            Modifier.fillMaxSize().tap {
                                onChange(saku.copy(goals = saku.goals.filter { it.id != goal.id }))
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Glyph(Ic.Trash, t.muted2, 16.dp)
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
            PlainField(
                value = newTarget,
                onValueChange = { v -> newTarget = v.filter { it.isDigit() }.take(12) },
                placeholder = "0",
                t = t,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                keyboardType = KeyboardType.Number
            )
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Simpan target", t, enabled = canSave) {
            onChange(
                saku.copy(
                    goals = saku.goals + Goal(
                        id = "g" + System.nanoTime().toString(36),
                        name = newName.trim(),
                        target = targetValue,
                        saved = 0L
                    )
                )
            )
            newName = ""
            newTarget = ""
            focus.clearFocus()
        }
        Spacer(Modifier.height(110.dp))
    }
}
