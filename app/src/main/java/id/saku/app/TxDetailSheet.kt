package id.saku.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * V1.3 #6 — klik baris transaksi membuka detail dulu.
 * Dari sini pengguna baru memilih "Ubah" (ke TxSheet) atau "Hapus" (dua ketuk).
 */
@Composable
fun TxDetailSheet(
    t: Tok,
    saku: Saku,
    tx: Tx,
    visible: Boolean,
    onDismiss: () -> Unit,
    onEdit: (Tx) -> Unit,
    onDelete: (String) -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val isIn = tx.type == "in"
    val amountColor = if (isIn) t.pos else t.error
    val catName = saku.catOf(tx.cat)?.name ?: "Lainnya"
    val goalName = if (tx.goal.isEmpty()) null else saku.goalOf(tx.goal)?.name

    SheetScaffold(
        t = t,
        onDismiss = onDismiss,
        visible = visible,
        footer = {
            PrimaryButton("Ubah transaksi", t) { onEdit(tx) }
            Spacer(Modifier.height(8.dp))
            SecondaryButton(
                label = if (confirmDelete) "Yakin hapus transaksi ini?" else "Hapus transaksi",
                t = t
            ) {
                if (confirmDelete) onDelete(tx.id) else confirmDelete = true
            }
        }
    ) {
        SheetTitle(t, "Detail transaksi", onDismiss)
        Spacer(Modifier.height(18.dp))

        Text(if (isIn) "Masuk" else "Keluar", Type.label, t.muted2)
        Text(
            (if (isIn) "+" else "\u2212") + Fmt.num(tx.amount),
            Type.display,
            amountColor,
            Modifier.padding(top = 3.dp)
        )

        if (tx.photo.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            PhotoPreview(tx.photo, t)
        }

        Spacer(Modifier.height(16.dp))
        Hair(t.hair)

        DetailRow(t, "Kategori", catName, glyph = saku.catOf(tx.cat)?.glyph)
        DetailRow(t, "Catatan", tx.note.ifBlank { "—" })
        DetailRow(t, "Tanggal", Fmt.longDate(tx.date))
        DetailRow(t, "Waktu", tx.time + " · " + dayPartLabel(tx.time))
        DetailRow(t, "Saldo setelah", Fmt.rp(saku.saldoOf(tx)))
        if (goalName != null) {
            DetailRow(t, "Target nabung", goalName)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Saldo setelah dihitung dari seluruh transaksi sampai bulan ini.",
            Type.micro,
            t.muted2,
            Modifier.padding(top = 4.dp),
            maxLines = 2
        )
    }
}

private fun dayPartLabel(time: String): String {
    val h = time.substringBefore(":").toIntOrNull() ?: return ""
    return when {
        h < 11 -> "pagi"
        h < 15 -> "siang"
        h < 19 -> "sore"
        else -> "malam"
    }
}

@Composable
private fun DetailRow(
    t: Tok,
    label: String,
    value: String,
    glyph: String? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (glyph != null) {
            Glyph(glyphOf(glyph), t.muted2, 16.dp)
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Text(label, Type.body, t.muted2, Modifier.weight(1f))
        Text(value, Type.bodySm.copy(fontWeight = FontWeight.SemiBold), t.ink)
    }
    Hair(t.hairSoft)
}
