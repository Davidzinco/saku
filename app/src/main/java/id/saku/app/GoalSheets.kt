package id.saku.app

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** Sheet nominal untuk setor / tarik nabung. */
@Composable
fun AmountSheet(
    t: Tok,
    title: String,
    subtitle: String,
    quick: List<Long>,
    max: Long?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var digits by remember { mutableStateOf("") }
    val amount = digits.toLongOrNull() ?: 0L
    val over = max != null && amount > max
    val valid = amount > 0L && !over

    SheetScaffold(
        t = t,
        onDismiss = onDismiss,
        visible = visible,
        footer = { PrimaryButton("Simpan", t, enabled = valid) { onConfirm(amount) } }
    ) {
        SheetTitle(t, title, onDismiss)
        Text(subtitle, Type.label, t.muted2, Modifier.padding(top = 6.dp), maxLines = 3)
        Spacer(Modifier.height(14.dp))

        Text("Nominal", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        FieldShell(t, height = 68.dp, error = over) {
            Text("Rp", Type.currency, t.muted2)
            Spacer(Modifier.width(8.dp))
            AmountField(
                digits = digits,
                onDigits = { digits = it },
                t = t,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        if (over) {
            Text("Melebihi saldo target (" + Fmt.rp(max) + ").", Type.label, t.error, Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quick.forEach { q ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, t.hair, RoundedCornerShape(10.dp))
                        .tap { digits = q.toString() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (q >= 1_000_000L) (q / 1_000_000L).toString() + "jt" else (q / 1000L).toString() + "rb",
                        Type.label,
                        t.ink
                    )
                }
            }
        }

    }
}

/** Sheet ubah nama/target nabung + hapus (hanya kalau saldo target kosong). */
@Composable
fun GoalEditSheet(
    t: Tok,
    goal: Goal,
    saved: Long,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(goal.name) }
    var target by remember { mutableStateOf(goal.target.toString()) }
    val targetValue = target.toLongOrNull() ?: 0L
    val canSave = name.isNotBlank() && targetValue > 0L
    var confirmDelete by remember { mutableStateOf(false) }

    SheetScaffold(
        t = t,
        onDismiss = onDismiss,
        visible = visible,
        footer = {
            PrimaryButton("Simpan perubahan", t, enabled = canSave) { onSave(name.trim(), targetValue) }
            Spacer(Modifier.height(8.dp))
            SecondaryButton(
                label = if (confirmDelete) "Yakin hapus target?" else "Hapus target",
                t = t
            ) {
                when {
                    saved != 0L -> Unit
                    confirmDelete -> onDelete()
                    else -> confirmDelete = true
                }
            }
        }
    ) {
        SheetTitle(t, "Ubah target", onDismiss)
        Spacer(Modifier.height(14.dp))

        Text("Nama target", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        FieldShell(t) {
            PlainField(
                value = name,
                onValueChange = { name = it.take(24) },
                placeholder = "Nama target",
                t = t,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("Target", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        FieldShell(t) {
            Text("Rp", Type.currency, t.muted2)
            Spacer(Modifier.width(8.dp))
            AmountField(
                digits = target,
                onDigits = { target = it },
                t = t,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                style = Type.body
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Sudah tersimpan " + Fmt.rp(saved) + " — dihitung dari transaksi setoran, bukan diisi manual.",
            Type.micro,
            t.muted2,
            maxLines = 3
        )

        if (saved != 0L) {
            Text(
                "Tarik dulu " + Fmt.rp(saved) + " sebelum menghapus target.",
                Type.label,
                t.muted2,
                Modifier.padding(top = 8.dp),
                maxLines = 2
            )
        }
    }
}
