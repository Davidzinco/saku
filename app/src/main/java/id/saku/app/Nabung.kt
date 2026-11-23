package id.saku.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Calendar
import java.util.Locale

/**
 * V1.6.8 #12 — Redesign UI halaman Tabungan (Nabung):
 * - Kartu ringkasan total nabung modern dengan progress keseluruhan.
 * - Kartu target visual: foto motivasi dominan dengan overlay gradient halus,
 *   progress bar, nominal jelas, dan tombol cepat Setor/Tarik langsung di kartu.
 * - Memakai LazyColumn & perhitungan memoized untuk performa gulir lancar.
 */
@Composable
fun Nabung(
    saku: Saku,
    t: Tok,
    onChange: (Saku) -> Unit,
    onSheet: (GoalSheet) -> Unit,
    onToggleHide: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val savedByGoal = remember(saku.tx) { saku.savedMap() }
    val balance = remember(saku.tx) { saku.balance() }
    val savedTotal = remember(savedByGoal, saku.goals) { saku.goals.sumOf { savedByGoal[it.id] ?: 0L } }
    val targetTotal = remember(saku.goals) { saku.goals.sumOf { it.target } }
    val overallFrac = if (targetTotal > 0L) (savedTotal.toFloat() / targetTotal.toFloat()).coerceIn(0f, 1f) else 0f
    val pct = (overallFrac * 100).toInt()

    val thisMonth = remember(saku.tx, saku.goals) {
        val goalIds = saku.goals.mapTo(HashSet()) { it.id }
        val now = Calendar.getInstance()
        val prefix = String.format(Locale.US, "%04d-%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
        saku.tx.filter { it.goal.isNotEmpty() && it.goal in goalIds && it.date.startsWith(prefix) }.sumOf { -it.signed }
    }

    var formOpen by remember { mutableStateOf(saku.goals.isEmpty()) }
    var newName by remember { mutableStateOf("") }
    var newTarget by remember { mutableStateOf("") }
    var newRawImage by remember { mutableStateOf<String?>(null) }
    val targetValue = newTarget.toLongOrNull() ?: 0L
    val canSave = newName.isNotBlank() && targetValue > 0L

    val pendingState = rememberUpdatedState(newRawImage)
    DisposableEffect(Unit) {
        onDispose { discardRawImage(pendingState.value) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tabungan", Type.title, t.ink, Modifier.weight(1f))
                EyeToggleButton(t, saku.hideBalance, onToggleHide)
            }
        }

        // Kartu Ringkasan Total Tabungan — datar, rapi, tanpa gradien & nested cards (de-slop)
        item(key = "summary") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(t.sheet)
                    .border(1.dp, t.hair, RoundedCornerShape(10.dp))
                    .padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Total tersimpan", Type.label, t.muted)
                    Spacer(Modifier.weight(1f))
                    if (targetTotal > 0L) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(t.pos.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("$pct% tercapai", Type.micro.copy(fontWeight = FontWeight.SemiBold), t.pos)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                val shownSaved = animatedLong(savedTotal)
                Text(
                    Fmt.rpMasked(shownSaved, saku.hideBalance),
                    Type.display,
                    t.ink
                )
                if (targetTotal > 0L) {
                    Text(
                        "dari target " + Fmt.rpMasked(targetTotal, saku.hideBalance),
                        Type.micro,
                        t.muted2,
                        Modifier.padding(top = 2.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Track(t, overallFrac, height = 4.dp, color = t.pos)
                }
                Spacer(Modifier.height(12.dp))
                Hair(t.hairSoft)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Stat(t, "Saldo tersedia", Fmt.rpMasked(balance, saku.hideBalance), t.ink)
                    Stat(
                        t,
                        "Bulan ini",
                        if (saku.hideBalance) "\u2022\u2022\u2022\u2022" else Fmt.sign(thisMonth),
                        if (thisMonth > 0) t.pos else t.ink
                    )
                }
            }
        }

        if (saku.goals.isEmpty()) {
            item(key = "empty") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(t.sheet)
                        .border(1.dp, t.hairSoft, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text("Belum ada target nabung.", Type.body.copy(fontWeight = FontWeight.SemiBold), t.ink)
                        Text(
                            "Tambahkan gambar impianmu di bawah. Setiap setoran tercatat sebagai transaksi sehingga saldo selalu sinkron.",
                            Type.micro,
                            t.muted2,
                            Modifier.padding(top = 4.dp),
                            maxLines = 3
                        )
                    }
                }
            }
        } else {
            item(key = "goals_title") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Target impian", Type.section, t.ink)
                    Spacer(Modifier.weight(1f))
                    Text("${saku.goals.size} target", Type.micro, t.muted2)
                }
            }

            items(saku.goals, key = { it.id }) { goal ->
                val saved = savedByGoal[goal.id] ?: 0L
                VisualGoalCard(
                    t = t,
                    goal = goal,
                    saved = saved,
                    hideBalance = saku.hideBalance,
                    onDetail = { onSheet(GoalSheet.Detail(goal)) },
                    onSetor = { onSheet(GoalSheet.Setor(goal)) },
                    onTarik = { if (saved > 0) onSheet(GoalSheet.Tarik(goal)) },
                    onEdit = { onSheet(GoalSheet.Ubah(goal)) }
                )
            }
        }

        // Form Target Baru dalam kartu modern yang bisa dibuka-tutup
        item(key = "new_goal") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(t.sheet)
                    .border(1.dp, t.hairSoft, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().tap { formOpen = !formOpen },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(t.accentWash),
                        contentAlignment = Alignment.Center
                    ) {
                        Glyph(Ic.Plus, t.accent, 16.dp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Buat target nabung baru", Type.body.copy(fontWeight = FontWeight.SemiBold), t.ink, Modifier.weight(1f))
                    Glyph(if (formOpen) Ic.Up else Ic.Down, t.muted2, 15.dp)
                }

                AnimatedVisibility(visible = formOpen) {
                    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                        FieldShell(t) {
                            PlainField(
                                value = newName,
                                onValueChange = { newName = it.take(24) },
                                placeholder = "Nama target (mis. Dana Darurat, Laptop)",
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
                        GoalImagePicker(
                            t = t,
                            previewPath = newRawImage ?: "",
                            onPick = { path ->
                                if (newRawImage != null && newRawImage != path) discardRawImage(newRawImage)
                                invalidateGoalImageCache(path)
                                newRawImage = path
                            },
                            onClear = {
                                discardRawImage(newRawImage)
                                newRawImage = null
                            }
                        )
                        Spacer(Modifier.height(14.dp))
                        PrimaryButton("Simpan target", t, enabled = canSave) {
                            val image = newRawImage?.let { storeGoalImage(ctx, File(it)) ?: "" } ?: ""
                            discardRawImage(newRawImage)
                            newRawImage = null
                            onChange(
                                saku.copy(
                                    goals = saku.goals + Goal(
                                        id = "g" + System.nanoTime().toString(36),
                                        name = newName.trim(),
                                        target = targetValue,
                                        image = image
                                    )
                                )
                            )
                            newName = ""
                            newTarget = ""
                            formOpen = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisualGoalCard(
    t: Tok,
    goal: Goal,
    saved: Long,
    hideBalance: Boolean,
    onDetail: () -> Unit,
    onSetor: () -> Unit,
    onTarik: () -> Unit,
    onEdit: () -> Unit
) {
    val frac = if (goal.target > 0L) (saved.toFloat() / goal.target.toFloat()).coerceIn(0f, 1f) else 0f
    val goalPct = (frac * 100).toInt().coerceIn(0, 100)
    val remaining = (goal.target - saved).coerceAtLeast(0L)
    val hasImage = goal.image.isNotEmpty()

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(t.sheet)
            .border(1.dp, t.hair, RoundedCornerShape(10.dp))
            .pressable(scale = 0.99f, alpha = 0.96f, onClick = onDetail)
    ) {
        if (hasImage) {
            Box(Modifier.fillMaxWidth().height(136.dp)) {
                GoalImageWide(goal.image, t, 136.dp, Modifier.fillMaxSize())
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x14000000), Color(0xCC0D120F))
                            )
                        )
                )
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(44.dp)
                        .tap(onEdit),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x73000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Glyph(Ic.Pencil, Color.White, 14.dp)
                    }
                }
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        goal.name,
                        Type.title.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                        Color.White,
                        Modifier.weight(1f),
                        maxLines = 2
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(t.pos)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "$goalPct%",
                            Type.micro.copy(fontWeight = FontWeight.SemiBold),
                            if (t.dark) t.bg else Color.White
                        )
                    }
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(t.accentWash),
                    contentAlignment = Alignment.Center
                ) {
                    Glyph(Ic.Wallet, t.accent, 18.dp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(goal.name, Type.body.copy(fontWeight = FontWeight.SemiBold), t.ink, maxLines = 2)
                    Text(
                        if (remaining == 0L) "Target tercapai" else "Sisa " + Fmt.rpMasked(remaining, hideBalance),
                        Type.micro,
                        if (remaining == 0L) t.pos else t.muted2
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(t.pos.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("$goalPct%", Type.micro.copy(fontWeight = FontWeight.SemiBold), t.pos)
                }
                TouchTarget {
                    Box(Modifier.fillMaxSize().tap(onEdit), contentAlignment = Alignment.Center) {
                        Glyph(Ic.Pencil, t.muted2, 15.dp)
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Fmt.rpMasked(saved, hideBalance),
                    Type.body.copy(fontWeight = FontWeight.SemiBold),
                    t.ink
                )
                Text(
                    " / " + Fmt.rpMasked(goal.target, hideBalance),
                    Type.micro,
                    t.muted2,
                    Modifier.weight(1f)
                )
                if (hasImage) {
                    Text(
                        if (remaining == 0L) "Tercapai" else "Sisa " + Fmt.rpMasked(remaining, hideBalance),
                        Type.micro,
                        if (remaining == 0L) t.pos else t.muted2
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Track(t, frac, height = 4.dp, color = t.pos)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(t.accent)
                        .tap(onSetor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Setor",
                        Type.label.copy(fontWeight = FontWeight.SemiBold),
                        if (t.dark) t.bg else Color.White
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, if (saved > 0) t.hair else t.hairSoft, RoundedCornerShape(10.dp))
                        .tap(onTarik),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tarik",
                        Type.label.copy(fontWeight = FontWeight.SemiBold),
                        if (saved > 0) t.ink else t.muted2
                    )
                }
            }
        }
    }
}
