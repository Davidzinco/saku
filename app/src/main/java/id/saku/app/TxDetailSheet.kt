package id.saku.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
 * V1.4 #6 — ada navigasi transaksi sebelumnya/berikutnya, dan penghapusan tidak
 * menutup sheet mendadak: sheet tetap terbuka dengan konfirmasi + tombol Tutup.
 * V1.4 #7 — beberapa foto, ketuk untuk pratinjau penuh dengan pinch-zoom.
 * V1.5 #4 — hari, jumlah lampiran, dan asal setoran/penarikan nabung.
 */
@Composable
fun TxDetailSheet(
    t: Tok,
    saku: Saku,
    tx: Tx,
    visible: Boolean,
    index: Int,
    total: Int,
    deleted: Boolean,
    notice: String,
    onDismiss: () -> Unit,
    onEdit: (Tx) -> Unit,
    onDelete: (String) -> Unit,
    onNavigate: (Int) -> Unit,
    onDone: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var viewerAt by remember { mutableStateOf<Int?>(null) }
    val isIn = tx.type == "in"
    val isTransfer = tx.type == "transfer"
    val amountColor = when {
        deleted -> t.muted2
        isTransfer -> t.accent
        isIn -> t.pos
        else -> t.error
    }
    val catName = if (isTransfer) "Pindah saldo" else (saku.catOf(tx.cat)?.name ?: "Lainnya")
    val goalName = if (tx.goal.isEmpty()) null else saku.goalOf(tx.goal)?.name
    val fromAccName = saku.accountOf(tx.account)?.name ?: saku.accounts.firstOrNull()?.name ?: "Tunai"
    val toAccName = if (tx.toAccount.isNotEmpty()) saku.accountOf(tx.toAccount)?.name else null
    val photos = tx.allPhotos

    Box(Modifier.fillMaxSize()) {
        SheetScaffold(
            t = t,
            onDismiss = onDismiss,
            visible = visible,
            footer = {
                if (deleted) {
                    PrimaryButton("Tutup", t) { onDone() }
                } else {
                    PrimaryButton("Ubah transaksi", t) { onEdit(tx) }
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(
                        label = if (confirmDelete) "Yakin hapus transaksi ini?" else "Hapus transaksi",
                        t = t
                    ) {
                        if (confirmDelete) onDelete(tx.id) else confirmDelete = true
                    }
                }
            }
        ) {
            SheetTitle(t, "Detail transaksi", onDismiss)

            if (total > 1 && !deleted) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TouchTarget {
                        Box(
                            Modifier.fillMaxSize().tap { onNavigate(-1) },
                            contentAlignment = Alignment.Center
                        ) { Glyph(Ic.ChevL, t.muted2, 16.dp) }
                    }
                    Text("${(index + 1).coerceAtLeast(1)} / $total", Type.micro, t.muted2)
                    Spacer(Modifier.width(6.dp))
                    TouchTarget {
                        Box(
                            Modifier.fillMaxSize().tap { onNavigate(1) },
                            contentAlignment = Alignment.Center
                        ) { Glyph(Ic.ChevR, t.muted2, 16.dp) }
                    }
                    Spacer(Modifier.weight(1f))
                }
            } else {
                Spacer(Modifier.height(18.dp))
            }

            if (notice.isNotEmpty()) {
                Text(notice, Type.label, t.accent, Modifier.padding(vertical = 8.dp), maxLines = 2)
            }

            Text(
                when {
                    isTransfer -> "Pindah saldo"
                    isIn -> "Masuk"
                    else -> "Keluar"
                },
                Type.label,
                t.muted2
            )
            Text(
                (if (isTransfer) "" else if (isIn) "+" else "\u2212") + Fmt.num(tx.amount),
                Type.display,
                amountColor,
                Modifier.padding(top = 3.dp)
            )

            if (photos.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    photos.forEachIndexed { i, path ->
                        Box(Modifier.tap { viewerAt = i }) {
                            PhotoThumb(path, t, 72.dp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Hair(t.hair)

            DetailRow(t, "Kategori", catName, glyph = if (isTransfer) "transfer" else saku.catOf(tx.cat)?.glyph)
            if (saku.multiAccount || isTransfer) {
                DetailRow(
                    t,
                    if (isTransfer) "Alur saldo" else "Sumber dana",
                    if (isTransfer && toAccName != null) "$fromAccName \u2192 $toAccName" else fromAccName,
                    glyph = "wallet"
                )
            }
            DetailRow(t, "Catatan", tx.note.ifBlank { "—" })
            DetailRow(t, "Tanggal", Fmt.longDate(tx.date))
            // V1.5 #4: hari, jam beserta pembagian waktu, lampiran, dan asal
            // setoran nabung — semua tetap dalam grammar baris + hairline.
            DetailRow(t, "Hari", Fmt.weekday(tx.date))
            DetailRow(t, "Waktu", tx.time + " · " + dayPartLabel(tx.time))
            if (photos.isNotEmpty()) {
                DetailRow(t, "Lampiran", photos.size.toString() + " foto", glyph = "image")
            }
            DetailRow(t, "Saldo setelah", Fmt.rp(saku.saldoOf(tx)))
            if (goalName != null) {
                DetailRow(t, "Jenis", if (isIn) "Tarik dari" else "Setor ke", glyph = "wallet")
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

        if (viewerAt != null) {
            BackHandler { viewerAt = null }
            PhotoViewer(t, photos, viewerAt!!) { viewerAt = null }
        }
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
