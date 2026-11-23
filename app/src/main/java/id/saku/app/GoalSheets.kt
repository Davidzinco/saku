package id.saku.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

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
                        .height(44.dp)
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

/**
 * V1.5 #5 — pemilih gambar target. Berkas yang dipilih masih berupa mentah di
 * cacheDir sampai tombol simpan ditekan, jadi membatalkan sheet tidak pernah
 * menyisakan berkas yatim dan tidak pernah menghapus gambar yang masih dipakai.
 */
@Composable
fun GoalImagePicker(
    t: Tok,
    previewPath: String,
    onPick: (String) -> Unit,
    onClear: () -> Unit
) {
    val actions = rememberRawImageActions(onPick)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GoalImage(previewPath, t, 64.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Gambar", Type.label, t.muted2)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhotoNoteIcons(t, actions)
                if (previewPath.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text("Hapus", Type.micro, t.error, Modifier.tap(onClear))
                }
            }
        }
    }
}

/** Konversi berkas mentah jadi WebP target; kembalikan "" kalau tidak ada. */
private fun commitGoalImage(ctx: android.content.Context, raw: String?): String =
    if (raw.isNullOrEmpty()) "" else storeGoalImage(ctx, File(raw)) ?: ""

/**
 * V1.5 #5 — sheet detail target: gambar motivasi, progress, dan riwayat
 * kapan saja setoran/penarikan terjadi. Daftar riwayat sengaja memakai
 * `Column` biasa (bukan LazyColumn) karena SheetScaffold sudah menggulir
 * secara vertikal; nested scroll di sini berisiko error pengukuran.
 * Jumlah baris dibatasi, dengan opsi "Lihat semua" bila perlu.
 */
@Composable
fun GoalDetailSheet(
    t: Tok,
    saku: Saku,
    goal: Goal,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSetor: () -> Unit,
    onTarik: () -> Unit,
    onEdit: () -> Unit
) {
    val history = remember(saku.tx, goal.id) { saku.goalHistory(goal.id) }
    val saved = saku.savedOf(goal.id)
    val shownSaved = animatedLong(saved)
    val frac = if (goal.target > 0) saved.toFloat() / goal.target.toFloat() else 0f
    val pct = (frac * 100).toInt().coerceIn(0, 100)
    var showAll by remember { mutableStateOf(false) }
    val shown = if (showAll) history else history.take(8)

    SheetScaffold(
        t = t,
        onDismiss = onDismiss,
        visible = visible,
        footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(t.accent)
                        .pressable(scale = 0.96f, alpha = 0.9f, onClick = onSetor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Setor",
                        Type.body.copy(fontWeight = FontWeight.SemiBold),
                        if (t.dark) t.bg else androidx.compose.ui.graphics.Color.White
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, t.hair, CircleShape)
                        .pressable(scale = 0.96f, alpha = 0.9f) {
                            if (saved > 0) onTarik()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tarik",
                        Type.body.copy(fontWeight = FontWeight.SemiBold),
                        if (saved > 0) t.ink else t.muted2
                    )
                }
            }
        }
    ) {
        SheetTitle(t, goal.name, onDismiss)
        Spacer(Modifier.height(12.dp))

        if (goal.image.isNotEmpty()) {
            GoalImageWide(goal.image, t, 150.dp)
            Spacer(Modifier.height(12.dp))
        }

        Text("Tersimpan", Type.label, t.muted2)
        Text(Fmt.num(shownSaved), Type.display, t.ink, Modifier.padding(top = 3.dp))
        Text(
            "dari " + Fmt.rp(goal.target) + " \u00b7 " + pct + "%",
            Type.label,
            t.muted2,
            Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.height(10.dp))
        Track(t, frac, height = 6.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            if (saved >= goal.target) "Target tercapai" else "Sisa " + Fmt.rp(goal.target - saved),
            Type.micro,
            if (saved >= goal.target) t.pos else t.muted2
        )

        Spacer(Modifier.height(16.dp))
        Hair(t.hair)

        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Riwayat menabung", Type.section, t.ink, Modifier.weight(1f))
            Text(
                if (history.isEmpty()) "belum ada" else history.size.toString() + " catatan",
                Type.micro,
                t.muted2
            )
        }

        if (history.isEmpty()) {
            Text(
                "Belum ada setoran atau penarikan.",
                Type.label,
                t.muted2,
                Modifier.padding(vertical = 10.dp)
            )
        } else {
            shown.forEach { tx ->
                val isIn = tx.type == "in"
                Row(
                    Modifier.fillMaxWidth().height(46.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(t.hairSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Glyph(if (isIn) Ic.Plus else Ic.Minus, t.muted2, 12.dp, 1.4.dp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (isIn) "Tarik" else "Setor",
                        Type.bodySm,
                        t.ink,
                        Modifier.width(48.dp)
                    )
                    Text(
                        Fmt.dateTime(tx.date, tx.time),
                        Type.micro,
                        t.muted2,
                        Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        (if (isIn) "+" else "\u2212") + Fmt.num(tx.amount),
                        Type.amount.copy(fontWeight = FontWeight.SemiBold),
                        if (isIn) t.pos else t.error
                    )
                }
                Hair(t.hairSoft)
            }
            if (!showAll && history.size > shown.size) {
                Text(
                    "Lihat semua " + history.size + " catatan",
                    Type.label.copy(fontWeight = FontWeight.SemiBold),
                    t.accent,
                    Modifier.padding(vertical = 10.dp).tap { showAll = true }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            if (goal.image.isEmpty()) "Tambah gambar" else "Ubah target & gambar",
            Type.label.copy(fontWeight = FontWeight.SemiBold),
            t.accent,
            Modifier.padding(vertical = 8.dp).tap(onEdit)
        )
    }
}

/** Sheet ubah nama/target nabung + gambar + hapus (hanya kalau saldo target kosong). */
@Composable
fun GoalEditSheet(
    t: Tok,
    goal: Goal,
    saved: Long,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Long, String) -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(goal.name) }
    var target by remember { mutableStateOf(goal.target.toString()) }
    var clearImage by remember { mutableStateOf(false) }
    var rawImage by remember { mutableStateOf<String?>(null) }
    val targetValue = target.toLongOrNull() ?: 0L
    val canSave = name.isNotBlank() && targetValue > 0L
    var confirmDelete by remember { mutableStateOf(false) }

    val preview = rawImage ?: if (clearImage) "" else goal.image

    // Batalkan sheet -> buang berkas mentah yang belum disimpan. Tidak pernah
    // menyentuh goal.image yang masih dipakai data tersimpan.
    val pendingState = rememberUpdatedState(rawImage)
    DisposableEffect(Unit) {
        onDispose { discardRawImage(pendingState.value) }
    }

    SheetScaffold(
        t = t,
        onDismiss = onDismiss,
        visible = visible,
        footer = {
            PrimaryButton("Simpan perubahan", t, enabled = canSave) {
                val finalImage = when {
                    rawImage != null -> commitGoalImage(ctx, rawImage).ifEmpty { goal.image }
                    clearImage -> ""
                    else -> goal.image
                }
                discardRawImage(rawImage)
                rawImage = null
                onSave(name.trim(), targetValue, finalImage)
            }
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

        Spacer(Modifier.height(16.dp))
        Hair(t.hairSoft)
        Spacer(Modifier.height(12.dp))
        GoalImagePicker(
            t = t,
            previewPath = preview,
            onPick = { path ->
                if (rawImage != null && rawImage != path) discardRawImage(rawImage)
                invalidateGoalImageCache(path)
                rawImage = path
                clearImage = false
            },
            onClear = {
                discardRawImage(rawImage)
                rawImage = null
                clearImage = true
            }
        )

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
