package id.saku.app

import android.content.Context
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Calendar
import kotlin.math.abs

fun shiftMonth(c: Calendar, delta: Int): Calendar = (c.clone() as Calendar).apply {
    set(Calendar.DAY_OF_MONTH, 1)
    add(Calendar.MONTH, delta)
}

@Composable
fun Beranda(
    saku: Saku,
    t: Tok,
    cursor: Calendar,
    flashId: String?,
    onCursor: (Calendar) -> Unit,
    onCatat: () -> Unit,
    onOpen: (Tx) -> Unit,
    onNabung: () -> Unit,
    onTransaksi: () -> Unit = {},
    onToggleHide: () -> Unit = {},
    onScrub: (Boolean) -> Unit = {}
) {
    val monthKey = cursor.get(Calendar.YEAR) * 100 + cursor.get(Calendar.MONTH)
    // V1.6.8 #4: satu lintasan data per bulan, bukan 5x scan saku.tx per frame.
    val snap = remember(saku.tx, saku.cats, monthKey) { saku.monthSnapshot(cursor) }
    val balance = remember(saku.tx) { saku.balance() }

    if (saku.homeStyle == "klasik") {
        BerandaKlasik(
            saku = saku,
            t = t,
            cursor = cursor,
            monthKey = monthKey,
            snap = snap,
            balance = balance,
            flashId = flashId,
            onCursor = onCursor,
            onCatat = onCatat,
            onOpen = onOpen,
            onNabung = onNabung,
            onToggleHide = onToggleHide,
            onScrub = onScrub
        )
    } else {
        BerandaBaru(
            saku = saku,
            t = t,
            cursor = cursor,
            monthKey = monthKey,
            snap = snap,
            balance = balance,
            flashId = flashId,
            onCursor = onCursor,
            onCatat = onCatat,
            onOpen = onOpen,
            onNabung = onNabung,
            onTransaksi = onTransaksi,
            onToggleHide = onToggleHide
        )
    }
}

/**
 * V1.6.8 #2 — Beranda Baru: kartu saldo 3D tilt-parallax bernuansa glass-inspired
 * (aman dari rekursi RenderNode), ikon mata privasi, ringkasan sumber dana,
 * kategori cepat, nabung, dan 5 transaksi terbaru.
 */
@Composable
private fun BerandaBaru(
    saku: Saku,
    t: Tok,
    cursor: Calendar,
    monthKey: Int,
    snap: MonthSnapshot,
    balance: Long,
    flashId: String?,
    onCursor: (Calendar) -> Unit,
    onCatat: () -> Unit,
    onOpen: (Tx) -> Unit,
    onNabung: () -> Unit,
    onTransaksi: () -> Unit,
    onToggleHide: () -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    val accBalances = remember(saku.tx, saku.accounts) { saku.accountBalances() }
    val recentRows = remember(snap.rows) { snap.rows.take(5) }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        state = listState,
        contentPadding = PaddingValues(top = 4.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: Wordmark + navigasi bulan ringkas + ikon mata
        item(key = "header") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SAKU", Type.wordmark, t.ink)
                Spacer(Modifier.weight(1f))
                CompactMonthPill(t, cursor, onCursor)
                Spacer(Modifier.width(8.dp))
                EyeToggleButton(t, saku.hideBalance, onToggleHide)
            }
        }

        // Kartu Rekening 3D Gyro-Parallax + Tap-to-Flip (M2)
        item(key = "hero_card") {
            val monthLabel = remember(cursor) {
                Fmt.monthShort(cursor) + " " + cursor.get(Calendar.YEAR)
            }
            TiltBalanceCard(
                t = t,
                balance = balance,
                income = snap.income,
                outcome = snap.outcome,
                diff = snap.diff,
                hideBalance = saku.hideBalance,
                fontScale = fontScale,
                onToggleHide = onToggleHide,
                accounts = saku.accounts,
                accountBalances = accBalances,
                avgOut = snap.avgOut,
                activeDays = snap.activeDays,
                txCount = snap.rows.size,
                monthLabel = monthLabel
            )
        }

        // Sumber Dana (Tunai / E-Wallet / Custom)
        if (saku.multiAccount && saku.splitBalance && saku.accounts.isNotEmpty()) {
            item(key = "accounts") {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sumber dana", Type.section, t.ink)
                        Spacer(Modifier.weight(1f))
                        Text("${saku.accounts.size} aktif", Type.micro, t.muted2)
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(saku.accounts, key = { it.id }) { acc ->
                            val bal = accBalances[acc.id] ?: 0L
                            AccountBalanceTile(
                                t = t,
                                account = acc,
                                balance = bal,
                                hide = saku.hideBalance,
                                onClick = onTransaksi
                            )
                        }
                    }
                }
            }
        }

        // Pengeluaran per kategori — langsung di lembar kertas dengan garis rambut (de-slop)
        item(key = "categories") {
            CatSection(t, snap.spendingByCat, onCatat, showTopHair = true)
        }

        // Ringkasan Nabung (menghormati opsi Pengaturan > Beranda)
        if (saku.showNabungHome) {
            item(key = "nabung") {
                NabungRow(t, saku, onNabung, showTopHair = true)
            }
        }

        // Transaksi Terbaru (ringkas, lengkapnya di halaman Transaksi)
        item(key = "recent_head") {
            Row(
                Modifier.fillMaxWidth().topHair(t.hairSoft).padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transaksi terbaru", Type.section, t.ink)
                Spacer(Modifier.weight(1f))
                Text(
                    if (snap.rows.isEmpty()) "Catat baru" else "Lihat semua (${snap.rows.size}) \u203a",
                    Type.label.copy(fontWeight = FontWeight.SemiBold),
                    t.accent,
                    Modifier.padding(vertical = 8.dp).tap { if (snap.rows.isEmpty()) onCatat() else onTransaksi() }
                )
            }
        }

        if (recentRows.isEmpty()) {
            item(key = "recent_empty") {
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text("Belum ada transaksi bulan ini.", Type.body, t.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Catat yang pertama.",
                        Type.label.copy(fontWeight = FontWeight.SemiBold),
                        t.accent,
                        Modifier.padding(vertical = 8.dp).tap(onCatat)
                    )
                }
            }
        } else {
            item(key = "recent_table_head") {
                TableHead(t)
            }
            itemsIndexed(recentRows, key = { _, tx -> tx.id }) { i, tx ->
                Box(Modifier.rowEnter(i, monthKey)) {
                    TxRow(
                        t = t,
                        saku = saku,
                        tx = tx,
                        saldo = snap.running.getOrElse(i) { 0L },
                        newest = i == 0,
                        flash = tx.id == flashId,
                        hideBalance = saku.hideBalance
                    ) { onOpen(tx) }
                }
            }
        }
    }
}

private const val CARD_MAX_TILT_Y = 8f
private const val CARD_MAX_TILT_X = 7f

internal data class CardTilt(
    val rotX: Float,
    val rotY: Float,
    val parallaxX: Float,
    val parallaxY: Float
)

internal fun normalizeSensorTilt(
    deltaPitchRad: Float,
    deltaRollRad: Float,
    maxAngleRad: Float = 0.45f
): Pair<Float, Float> {
    val safeMax = if (maxAngleRad <= 0f || maxAngleRad.isNaN()) 0.45f else maxAngleRad
    val nx = if (deltaRollRad.isNaN()) 0f else (deltaRollRad / safeMax).coerceIn(-1f, 1f)
    val ny = if (deltaPitchRad.isNaN()) 0f else (deltaPitchRad / safeMax).coerceIn(-1f, 1f)
    return nx to ny
}

internal fun computeCardTilt(
    normX: Float,
    normY: Float,
    maxRotDeg: Float = 8f,
    maxParallaxPx: Float = 14f
): CardTilt {
    val cx = if (normX.isNaN()) 0f else normX.coerceIn(-1f, 1f)
    val cy = if (normY.isNaN()) 0f else normY.coerceIn(-1f, 1f)
    return CardTilt(
        rotX = -cy * maxRotDeg,
        rotY = cx * maxRotDeg,
        parallaxX = cx * maxParallaxPx,
        parallaxY = cy * maxParallaxPx
    )
}

internal fun isCardBackVisible(flipAngleDeg: Float): Boolean {
    if (flipAngleDeg.isNaN()) return false
    val norm = ((flipAngleDeg % 360f) + 360f) % 360f
    return norm in 90f..270f
}

/**
 * AGSL RuntimeShader foil sheen untuk API 33+ (Android 13+).
 * Terpisah dalam objek @RequiresApi(33) agar verifikasi kelas pada API 26-32 aman.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object PassbookSheenShader {
    private const val AGSL = """
        uniform float2 uResolution;
        uniform float2 uTilt;
        uniform float4 uSheenColor;
        uniform float4 uInkTintColor;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / max(uResolution, float2(1.0));
            float2 lightCenter = float2(0.5 + uTilt.y * 0.36, 0.42 - uTilt.x * 0.34);
            float bandCoord = (uv.x * 0.75 + uv.y * 0.55) - (0.62 + uTilt.y * 0.30 - uTilt.x * 0.22);
            float band = exp(-bandCoord * bandCoord * 16.0);
            float dist = length(uv - lightCenter);
            float softSpot = exp(-dist * dist * 3.0);
            float edgeX = smoothstep(0.36, 0.5, abs(uv.x - 0.5));
            float edgeY = smoothstep(0.36, 0.5, abs(uv.y - 0.5));
            float rim = max(edgeX, edgeY) * 0.28;
            float mixFactor = clamp(band * 0.55 + softSpot * 0.45 + rim * 0.25, 0.0, 1.0);
            float4 col = mix(uInkTintColor, uSheenColor, band);
            float a = col.a * mixFactor;
            return half4(col.rgb * a, a);
        }
    """

    fun create(): RuntimeShader = RuntimeShader(AGSL)

    fun update(
        shader: RuntimeShader,
        width: Float,
        height: Float,
        normTiltX: Float,
        normTiltY: Float,
        sheen: Color,
        inkTint: Color,
        sheenAlpha: Float,
        inkTintAlpha: Float
    ) {
        shader.setFloatUniform("uResolution", width.coerceAtLeast(1f), height.coerceAtLeast(1f))
        shader.setFloatUniform("uTilt", normTiltX, normTiltY)
        shader.setFloatUniform("uSheenColor", sheen.red, sheen.green, sheen.blue, sheenAlpha)
        shader.setFloatUniform("uInkTintColor", inkTint.red, inkTint.green, inkTint.blue, inkTintAlpha)
    }
}

/**
 * Kartu Rekening 3D Gyro-Parallax + Tap-to-Flip (M2):
 * - Layer 0: Kertas buku tabungan (`t.sheet`, radius `22.dp`, hairline `1.dp` `t.hair`,
 *   mikro-garis buku tabungan `Canvas` + cap sudut `t.muted2`).
 * - Layer 1: Kilau kertas/tinta halus mengikuti sudut gyro (AGSL `RuntimeShader`
 *   pada API 33+, `linearGradient` halus pada API 26-32; tanpa neon/radial glow).
 * - Layer 2: Konten dengan parallax berlawanan via `Modifier.graphicsLayer`.
 * - Tap-to-Flip (`0° <-> 180°` `rotationY`) ke rincian sumber dana & arus kas.
 * - Murni `graphicsLayer` + `Canvas` — nol `drawBackdrop` / `LayerBackdrop`.
 */
@Composable
fun TiltBalanceCard(
    t: Tok,
    balance: Long,
    income: Long,
    outcome: Long,
    diff: Long,
    hideBalance: Boolean,
    fontScale: Float,
    @Suppress("UNUSED_PARAMETER") onToggleHide: () -> Unit = {},
    modifier: Modifier = Modifier,
    accounts: List<Account> = emptyList(),
    accountBalances: Map<String, Long> = emptyMap(),
    avgOut: Long = 0L,
    activeDays: Int = 0,
    txCount: Int = 0,
    monthLabel: String = ""
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scale = motionScale()
    val density = LocalDensity.current.density

    val tiltXState = remember { mutableFloatStateOf(0f) }
    val tiltYState = remember { mutableFloatStateOf(0f) }
    val parallaxXState = remember { mutableFloatStateOf(0f) }
    val parallaxYState = remember { mutableFloatStateOf(0f) }

    // Gyroscope / Rotation Vector dengan kalibrasi posisi nyaman awal & fallback 0f
    DisposableEffect(ctx, lifecycleOwner, scale) {
        if (scale <= 0f) {
            tiltXState.floatValue = 0f
            tiltYState.floatValue = 0f
            parallaxXState.floatValue = 0f
            parallaxYState.floatValue = 0f
            return@DisposableEffect onDispose {}
        }
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sm?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        if (sm == null || sensor == null) {
            tiltXState.floatValue = 0f
            tiltYState.floatValue = 0f
            parallaxXState.floatValue = 0f
            parallaxYState.floatValue = 0f
            return@DisposableEffect onDispose {}
        }

        val rotMatrix = FloatArray(9)
        val refMatrix = FloatArray(9)
        var hasRef = false
        var biasX = 0f
        var biasY = 0f
        var smoothX = 0f
        var smoothY = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                if (!hasRef) {
                    System.arraycopy(rotMatrix, 0, refMatrix, 0, 9)
                    hasRef = true
                    biasX = 0f
                    biasY = 0f
                    smoothX = 0f
                    smoothY = 0f
                    return
                }
                // Kolom ke-3 dari R_rel = R_ref^T * R_curr (row-major 3x3)
                val nx = refMatrix[0] * rotMatrix[2] + refMatrix[3] * rotMatrix[5] + refMatrix[6] * rotMatrix[8]
                val ny = refMatrix[1] * rotMatrix[2] + refMatrix[4] * rotMatrix[5] + refMatrix[7] * rotMatrix[8]

                // Adaptasi lambat agar sudut pegang alami (~45°) tetap terpusat
                biasX += (nx - biasX) * 0.012f
                biasY += (ny - biasY) * 0.012f

                // Low-pass filter responsif untuk meredam getaran tangan
                smoothX += ((nx - biasX) - smoothX) * 0.18f
                smoothY += ((ny - biasY) - smoothY) * 0.18f

                val (normX, normY) = normalizeSensorTilt(
                    deltaPitchRad = smoothY,
                    deltaRollRad = smoothX,
                    maxAngleRad = 0.31f
                )
                val cardTilt = computeCardTilt(
                    normX = normX,
                    normY = normY,
                    maxRotDeg = CARD_MAX_TILT_Y,
                    maxParallaxPx = 8f
                )
                tiltXState.floatValue = cardTilt.rotX.coerceIn(-CARD_MAX_TILT_X, CARD_MAX_TILT_X)
                tiltYState.floatValue = cardTilt.rotY
                parallaxXState.floatValue = cardTilt.parallaxX
                parallaxYState.floatValue = cardTilt.parallaxY
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun register() {
            hasRef = false
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        fun unregister() {
            sm.unregisterListener(listener)
            hasRef = false
            tiltXState.floatValue = 0f
            tiltYState.floatValue = 0f
            parallaxXState.floatValue = 0f
            parallaxYState.floatValue = 0f
        }

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            register()
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE -> unregister()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregister()
        }
    }

    var flipped by remember { mutableStateOf(false) }
    val flipAngle by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = if (scale <= 0f) snap() else tween(Motion.d(380, scale), easing = FastOutSlowInEasing),
        label = "cardFlip"
    )
    val showBack by remember { derivedStateOf { isCardBackVisible(flipAngle) } }

    val agslShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) PassbookSheenShader.create() else null
    }
    val agslBrush = remember(agslShader) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && agslShader != null) {
            ShaderBrush(agslShader)
        } else null
    }

    val shape = RoundedCornerShape(22.dp)
    val cardDescription = if (showBack) {
        "Rincian rekening $monthLabel. Rata-rata keluar ${Fmt.rpMasked(avgOut, hideBalance)}, " +
            "$activeDays hari aktif, $txCount transaksi. Ketuk untuk kembali."
    } else {
        "Kartu saldo. Total saldo ${Fmt.rpMasked(balance, hideBalance)}. " +
            "Masuk ${if (hideBalance) "disembunyikan" else Fmt.rp(income)}, " +
            "keluar ${if (hideBalance) "disembunyikan" else Fmt.rp(outcome)}. " +
            "Ketuk untuk rincian rekening."
    }

    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 172.dp)
            .graphicsLayer {
                rotationX = tiltXState.floatValue
                rotationY = flipAngle + tiltYState.floatValue
                cameraDistance = 14f * density
            }
            .clip(shape)
            .background(t.sheet)
            .border(1.dp, t.hair, shape)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = cardDescription
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = if (showBack) "Tampilkan ringkasan saldo" else "Tampilkan rincian rekening"
            ) { flipped = !flipped }
    ) {
        // Layer 0 + Layer 1: Pola mikro garis buku tabungan + kilau foil kertas/tinta halus
        Canvas(Modifier.matchParentSize()) {
            val stepY = 26.dp.toPx()
            val insetX = 20.dp.toPx()
            var y = 46.dp.toPx()
            while (y < size.height - 16.dp.toPx()) {
                drawLine(
                    color = t.hairSoft.copy(alpha = if (t.dark) 0.55f else 0.7f),
                    start = Offset(insetX, y),
                    end = Offset(size.width - insetX, y),
                    strokeWidth = 0.6.dp.toPx()
                )
                y += stepY
            }
            // Garis sudut buku tabungan (t.muted2 agar cobalt t.accent tetap hanya di 3 peran DESIGN.md)
            val markY = 14.dp.toPx()
            drawLine(
                color = t.muted2.copy(alpha = 0.45f),
                start = Offset(insetX, markY),
                end = Offset(insetX + 18.dp.toPx(), markY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            val ntx = (tiltXState.floatValue / CARD_MAX_TILT_X).coerceIn(-1f, 1f)
            val nty = (tiltYState.floatValue / CARD_MAX_TILT_Y).coerceIn(-1f, 1f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && agslShader != null && agslBrush != null) {
                PassbookSheenShader.update(
                    shader = agslShader,
                    width = size.width,
                    height = size.height,
                    normTiltX = ntx,
                    normTiltY = nty,
                    sheen = if (t.dark) t.ink else Color.White,
                    inkTint = t.ink,
                    sheenAlpha = if (t.dark) 0.06f else 0.22f,
                    inkTintAlpha = if (t.dark) 0.04f else 0.03f
                )
                drawRect(brush = agslBrush)
            } else {
                val shiftX = size.width * (0.35f + nty * 0.25f)
                val shiftY = size.height * (0.20f - ntx * 0.20f)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            (if (t.dark) t.ink else Color.White).copy(alpha = if (t.dark) 0.05f else 0.20f),
                            t.ink.copy(alpha = if (t.dark) 0.03f else 0.025f),
                            Color.Transparent
                        ),
                        start = Offset(shiftX - size.width * 0.45f, shiftY - size.height * 0.45f),
                        end = Offset(shiftX + size.width * 0.45f, shiftY + size.height * 0.45f)
                    )
                )
            }
        }

        // Layer 2: Konten depan/belakang dengan parallax berlawanan di graphicsLayer
        if (!showBack) {
            CardFrontContent(
                t = t,
                balance = balance,
                income = income,
                outcome = outcome,
                diff = diff,
                hideBalance = hideBalance,
                fontScale = fontScale,
                parallaxXState = parallaxXState,
                parallaxYState = parallaxYState
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { rotationY = 180f }
            ) {
                CardBackContent(
                    t = t,
                    income = income,
                    outcome = outcome,
                    hideBalance = hideBalance,
                    accounts = accounts,
                    accountBalances = accountBalances,
                    avgOut = avgOut,
                    activeDays = activeDays,
                    txCount = txCount,
                    monthLabel = monthLabel,
                    parallaxXState = parallaxXState,
                    parallaxYState = parallaxYState
                )
            }
        }
    }
}

@Composable
private fun CardFrontContent(
    t: Tok,
    balance: Long,
    income: Long,
    outcome: Long,
    diff: Long,
    hideBalance: Boolean,
    fontScale: Float,
    parallaxXState: FloatState,
    parallaxYState: FloatState
) {
    val shownIncome = animatedLong(income, 320)
    val shownOutcome = animatedLong(outcome, 320)
    val shownDiff = animatedLong(diff, 320)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = -parallaxXState.floatValue * 0.5f.dp.toPx()
                    translationY = -parallaxYState.floatValue * 0.4f.dp.toPx()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total saldo", Type.label, t.muted2, Modifier.weight(1f))
            Text("Ketuk untuk rincian", Type.micro, t.muted2)
        }

        Spacer(Modifier.height(10.dp))

        Box(
            Modifier.graphicsLayer {
                translationX = -parallaxXState.floatValue * 0.95f.dp.toPx()
                translationY = -parallaxYState.floatValue * 0.75f.dp.toPx()
            }
        ) {
            AnimatedHeroAmount(t, balance, hideBalance, fontScale)
        }

        Spacer(Modifier.height(14.dp))
        Hair(t.hairSoft)
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = -parallaxXState.floatValue * 0.7f.dp.toPx()
                    translationY = -parallaxYState.floatValue * 0.55f.dp.toPx()
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InlineLedgerStat(
                t = t,
                label = "Masuk",
                valueText = if (hideBalance) "\u2022\u2022\u2022\u2022" else Fmt.sign(shownIncome),
                color = if (income > 0) t.pos else t.ink,
                modifier = Modifier.weight(1f)
            )
            InlineLedgerStat(
                t = t,
                label = "Keluar",
                valueText = if (hideBalance) "\u2022\u2022\u2022\u2022" else Fmt.out(shownOutcome),
                color = if (outcome > 0) t.error else t.ink,
                modifier = Modifier.weight(1f)
            )
            InlineLedgerStat(
                t = t,
                label = "Selisih",
                valueText = if (hideBalance) "\u2022\u2022\u2022\u2022" else Fmt.sign(shownDiff),
                color = if (diff > 0) t.pos else if (diff < 0) t.error else t.ink,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InlineLedgerStat(
    t: Tok,
    label: String,
    valueText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(label, Type.micro, t.muted2)
        Text(
            valueText,
            Type.figure.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
            color,
            Modifier.padding(top = 2.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun CardBackContent(
    t: Tok,
    income: Long,
    outcome: Long,
    hideBalance: Boolean,
    accounts: List<Account>,
    accountBalances: Map<String, Long>,
    avgOut: Long,
    activeDays: Int,
    txCount: Int,
    monthLabel: String,
    parallaxXState: FloatState,
    parallaxYState: FloatState
) {
    val posTotal = accounts.sumOf { (accountBalances[it.id] ?: 0L).coerceAtLeast(0L) }
    val displayAccounts = remember(accounts, accountBalances) {
        val sorted = accounts.map { it to (accountBalances[it.id] ?: 0L) }
            .sortedByDescending { it.second }
        if (sorted.size <= 3) sorted
        else {
            val head = sorted.take(2)
            val restSum = sorted.drop(2).sumOf { it.second }
            head + (Account("_rest", "${sorted.size - 2} lainnya", "other", "wallet") to restSum)
        }
    }

    val flowTotal = (income + outcome).coerceAtLeast(0L)
    val inFrac = if (flowTotal > 0L) (income.toFloat() / flowTotal.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .graphicsLayer {
                // Parallax X dibalik karena pembungkus luar di-counter-rotate rotationY = 180f
                translationX = parallaxXState.floatValue * 0.7f.dp.toPx()
                translationY = -parallaxYState.floatValue * 0.55f.dp.toPx()
            }
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (monthLabel.isNotEmpty()) "Rincian \u00b7 $monthLabel" else "Rincian rekening",
                Type.label.copy(fontWeight = FontWeight.SemiBold),
                t.ink,
                Modifier.weight(1f)
            )
            Text("Ketuk untuk kembali", Type.micro, t.muted2)
        }

        Spacer(Modifier.height(10.dp))

        if (displayAccounts.isNotEmpty()) {
            // Batang proporsional sumber dana (monochrome tint ramp + radius 4.dp sesuai DESIGN.md:107)
            if (posTotal > 0L) {
                Row(
                    Modifier.fillMaxWidth().height(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    displayAccounts.forEachIndexed { i, (_, bal) ->
                        val share = (bal.coerceAtLeast(0L).toFloat() / posTotal.toFloat()).coerceIn(0f, 1f)
                        if (share > 0f) {
                            Box(
                                Modifier
                                    .weight(share.coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(t.tint[i % t.tint.size])
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                displayAccounts.forEachIndexed { i, (acc, bal) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(t.tint[i % t.tint.size])
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(acc.name, Type.micro, t.muted2, Modifier.weight(1f))
                        Text(
                            Fmt.rpMasked(bal, hideBalance),
                            Type.figure.copy(fontWeight = FontWeight.SemiBold),
                            if (bal < 0L) t.error else t.ink
                        )
                    }
                }
            }
        } else {
            Text("Belum ada sumber dana.", Type.micro, t.muted2)
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Arus kas", Type.micro, t.muted2, Modifier.weight(1f))
            Text(
                if (flowTotal > 0L) "${(inFrac * 100).toInt()}% masuk" else "Belum ada transaksi",
                Type.micro,
                t.muted2
            )
        }
        Spacer(Modifier.height(4.dp))
        if (flowTotal > 0L) {
            Row(Modifier.fillMaxWidth().height(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (inFrac > 0f) {
                    Box(
                        Modifier
                            .weight(inFrac.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(t.pos)
                    )
                }
                val outFrac = (1f - inFrac).coerceIn(0f, 1f)
                if (outFrac > 0f) {
                    Box(
                        Modifier
                            .weight(outFrac.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(t.error)
                    )
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(t.hair)
            )
        }

        Spacer(Modifier.height(10.dp))
        Hair(t.hairSoft)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InlineLedgerStat(
                t = t,
                label = "Rata-rata keluar",
                valueText = Fmt.rpMasked(avgOut, hideBalance),
                color = t.ink,
                modifier = Modifier.weight(1f)
            )
            InlineLedgerStat(
                t = t,
                label = "Hari aktif",
                valueText = "$activeDays hari",
                color = t.ink,
                modifier = Modifier.weight(1f)
            )
            InlineLedgerStat(
                t = t,
                label = "Catatan",
                valueText = "$txCount trx",
                color = t.ink,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AccountBalanceTile(
    t: Tok,
    account: Account,
    balance: Long,
    hide: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .width(152.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(t.sheet)
            .border(1.dp, t.hairSoft, RoundedCornerShape(16.dp))
            .pressable(scale = 0.97f, alpha = 0.92f, onClick = onClick)
            .padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(t.accentWash),
                contentAlignment = Alignment.Center
            ) {
                Glyph(glyphOf(account.glyph), t.accent, 15.dp)
            }
            Spacer(Modifier.width(8.dp))
            Text(account.name, Type.label.copy(fontWeight = FontWeight.SemiBold), t.muted, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            Fmt.rpMasked(balance, hide),
            Type.body.copy(fontWeight = FontWeight.SemiBold),
            if (balance < 0) t.error else t.ink
        )
    }
}

@Composable
fun EyeToggleButton(t: Tok, hidden: Boolean, onToggle: () -> Unit) {
    TouchTarget {
        Box(
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = if (hidden) "Tampilkan saldo" else "Sembunyikan saldo" }
                .tap(onToggle),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(t.hairSoft),
                contentAlignment = Alignment.Center
            ) {
                Glyph(if (hidden) Ic.EyeOff else Ic.Eye, t.muted, 15.dp)
            }
        }
    }
}

@Composable
private fun CompactMonthPill(t: Tok, cursor: Calendar, onCursor: (Calendar) -> Unit) {
    Row(
        Modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(t.sheet)
            .border(1.dp, t.hairSoft, CircleShape)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .semantics { contentDescription = "Bulan sebelumnya" }
                .tap { onCursor(shiftMonth(cursor, -1)) },
            contentAlignment = Alignment.Center
        ) { Glyph(Ic.ChevL, t.muted2, 13.dp) }
        Text(
            Fmt.monthShort(cursor) + " " + cursor.get(Calendar.YEAR),
            Type.micro.copy(fontWeight = FontWeight.SemiBold),
            t.ink,
            Modifier.padding(horizontal = 4.dp)
        )
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .semantics { contentDescription = "Bulan berikutnya" }
                .tap { onCursor(shiftMonth(cursor, 1)) },
            contentAlignment = Alignment.Center
        ) { Glyph(Ic.ChevR, t.muted2, 13.dp) }
    }
}

/**
 * Beranda Klasik (Buku Tabungan) — dipertahankan utuh sebagai opsi di Pengaturan,
 * kini memakai MonthSnapshot + tombol mata privasi saldo.
 */
@Composable
private fun BerandaKlasik(
    saku: Saku,
    t: Tok,
    cursor: Calendar,
    monthKey: Int,
    snap: MonthSnapshot,
    balance: Long,
    flashId: String?,
    onCursor: (Calendar) -> Unit,
    onCatat: () -> Unit,
    onOpen: (Tx) -> Unit,
    onNabung: () -> Unit,
    onToggleHide: () -> Unit,
    onScrub: (Boolean) -> Unit
) {
    val fontScale = LocalDensity.current.fontScale

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SAKU", Type.wordmark, t.ink)
            Spacer(Modifier.weight(1f))
            EyeToggleButton(t, saku.hideBalance, onToggleHide)
        }

        SaldoBlock(t, balance, snap.income, snap.outcome, saku.hideBalance, fontScale)
        TrendSection(t, saku, cursor, onCursor, onScrub)
        CatSection(t, snap.spendingByCat, onCatat)
        NabungRow(t, saku, onNabung)
        ListSection(
            t = t,
            saku = saku,
            monthKey = monthKey,
            rows = snap.rows,
            running = snap.running,
            flashId = flashId,
            onCatat = onCatat,
            onOpen = onOpen,
            modifier = Modifier.weight(1f).padding(top = 2.dp)
        )
    }
}

/** Angka hero beranimasi terisolasi agar induk tidak recompose setiap frame. */
@Composable
fun AnimatedHeroAmount(t: Tok, target: Long, hide: Boolean, fontScale: Float) {
    val shown = animatedLong(target)
    Row(Modifier.padding(top = 2.dp)) {
        Text("Rp", Type.currency, t.muted2, Modifier.alignByBaseline())
        Spacer(Modifier.width(9.dp))
        Text(
            Fmt.masked(shown, hide),
            Type.hero(fontScale),
            t.ink,
            Modifier.alignByBaseline()
        )
    }
}

/** Angka yang bergerak ke nilai baru tanpa kehilangan presisi Long. */
@Composable
fun animatedLong(target: Long, duration: Int = 380): Long {
    val scale = motionScale()
    var from by remember { mutableStateOf(target) }
    var value by remember { mutableStateOf(target) }
    val anim = remember { Animatable(0f) }

    LaunchedEffect(target) {
        if (target == value) return@LaunchedEffect
        from = value
        if (scale <= 0f) {
            value = target
            return@LaunchedEffect
        }
        val start = from
        anim.snapTo(0f)
        anim.animateTo(1f, tween(duration, easing = FastOutSlowInEasing)) {
            value = (start + ((target - start).toDouble() * this.value)).toLong()
        }
        value = target
    }
    return value
}

@Composable
private fun SaldoBlock(
    t: Tok,
    saldo: Long,
    income: Long,
    outcome: Long,
    hideBalance: Boolean,
    fontScale: Float
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 11.dp)
    ) {
        AnimatedHeroAmount(t, saldo, hideBalance, fontScale)
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            AnimatedStat(t, "Masuk", income, "+", if (income > 0) t.pos else t.ink, hideBalance)
            AnimatedStat(t, "Keluar", outcome, "\u2212", if (outcome > 0) t.error else t.ink, hideBalance)
            val diff = income - outcome
            AnimatedStat(
                t,
                "Selisih",
                diff,
                null,
                if (diff > 0) t.pos else if (diff < 0) t.error else t.ink,
                hideBalance
            )
        }
    }
}

@Composable
fun AnimatedStat(t: Tok, label: String, target: Long, mode: String?, color: Color, hide: Boolean = false) {
    val shown = animatedLong(target, 340)
    val text = if (hide) "\u2022\u2022\u2022\u2022" else when (mode) {
        "+" -> Fmt.sign(shown)
        "\u2212" -> Fmt.out(shown)
        else -> Fmt.sign(shown)
    }
    Stat(t, label, text, color)
}

@Composable
fun Stat(t: Tok, label: String, value: String, color: Color) {
    Column {
        Text(label, Type.label, t.muted2)
        Text(value, Type.stat, color, Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun NavCircle(t: Tok, icon: Ic, label: String, onClick: () -> Unit) {
    TouchTarget {
        Box(
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = label }
                .tap(onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(t.hairSoft),
                contentAlignment = Alignment.Center
            ) {
                Glyph(icon, t.muted2, 14.dp)
            }
        }
    }
}

@Composable
fun MonthNav(t: Tok, cursor: Calendar, onCursor: (Calendar) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NavCircle(t, Ic.ChevL, "Bulan sebelumnya") { onCursor(shiftMonth(cursor, -1)) }
        AnimatedContent(
            targetState = Fmt.monthLabel(cursor),
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            modifier = Modifier.weight(1f),
            label = "month"
        ) { label ->
            Text(label, Type.section, t.ink, Modifier.fillMaxWidth().padding(horizontal = 8.dp), align = TextAlign.Center)
        }
        NavCircle(t, Ic.ChevR, "Bulan berikutnya") { onCursor(shiftMonth(cursor, 1)) }
    }
}

internal enum class ScrubAxis { UNDECIDED, HORIZONTAL_SCRUB, VERTICAL_SCROLL }

internal fun classifyScrubDrag(dx: Float, dy: Float, touchSlop: Float): ScrubAxis {
    val ax = abs(dx)
    val ay = abs(dy)
    if (ax <= touchSlop && ay <= touchSlop) return ScrubAxis.UNDECIDED
    return if (ax > ay) ScrubAxis.HORIZONTAL_SCRUB else ScrubAxis.VERTICAL_SCROLL
}

internal fun scrubBarIndex(x: Float, widthPx: Float, count: Int): Int {
    if (count <= 0 || widthPx <= 0f) return -1
    return (x / widthPx * count).toInt().coerceIn(0, count - 1)
}

@Composable
fun TrendSection(
    t: Tok,
    saku: Saku,
    cursor: Calendar,
    onCursor: (Calendar) -> Unit,
    onScrub: (Boolean) -> Unit,
    showTopHair: Boolean = true
) {
    val scale = motionScale()
    val windowKey = cursor.get(Calendar.YEAR) * 100 +
        (if (saku.months >= 12) 0 else if (cursor.get(Calendar.MONTH) <= 5) 0 else 6)
    val data = remember(saku.tx, saku.months, windowKey) { saku.trendFast(cursor) }
    val max = (data.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    val activeIndex = data.indexOfFirst { sameMonth(it.first, cursor) }.coerceAtLeast(0)
    val many = data.size > 6
    val gap = if (many) 4.dp else 14.dp
    val barW = if (many) 14.dp else 24.dp

    val dataState = rememberUpdatedState(data)
    val pickState = rememberUpdatedState(onCursor)
    val scrubState = rememberUpdatedState(onScrub)
    val scrub = Modifier.pointerInput(saku.months) {
        val touchSlop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)

            var lastData: List<Pair<Calendar, Long>>? = null
            var lastIndex = -1
            fun apply(x: Float) {
                val d = dataState.value
                val i = scrubBarIndex(x, size.width.toFloat(), d.size)
                if (i < 0) return
                if (d !== lastData) {
                    lastData = d
                    lastIndex = -1
                }
                if (i == lastIndex) return
                lastIndex = i
                pickState.value(d[i].first)
            }

            var axis = ScrubAxis.UNDECIDED
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        if (axis == ScrubAxis.UNDECIDED && !change.isConsumed) {
                            apply(change.position.x)
                        }
                        break
                    }
                    when (axis) {
                        ScrubAxis.UNDECIDED -> {
                            val dx = change.position.x - down.position.x
                            val dy = change.position.y - down.position.y
                            when (classifyScrubDrag(dx, dy, touchSlop)) {
                                ScrubAxis.VERTICAL_SCROLL -> return@awaitEachGesture
                                ScrubAxis.HORIZONTAL_SCRUB -> {
                                    axis = ScrubAxis.HORIZONTAL_SCRUB
                                    scrubState.value(true)
                                    change.consume()
                                    apply(change.position.x)
                                }
                                ScrubAxis.UNDECIDED -> Unit
                            }
                        }
                        ScrubAxis.HORIZONTAL_SCRUB -> {
                            change.consume()
                            apply(change.position.x)
                        }
                        ScrubAxis.VERTICAL_SCROLL -> return@awaitEachGesture
                    }
                }
            } finally {
                scrubState.value(false)
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .then(if (showTopHair) Modifier.topHair(t.hairSoft) else Modifier)
            .padding(top = 7.dp, bottom = 10.dp)
    ) {
        MonthNav(t, cursor, onCursor)
        Spacer(Modifier.height(8.dp))

        val activeTotal = data.getOrNull(activeIndex)?.second ?: 0L
        val chartDesc = "Pengeluaran ${Fmt.monthLabel(cursor)} sebesar ${Fmt.rp(activeTotal)}"
        Box(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .semantics { contentDescription = chartDesc }
                .then(scrub)
        ) {
            if (saku.chart == "garis") {
                LineChart(
                    t = t,
                    data = data,
                    max = max,
                    activeIndex = activeIndex,
                    modifier = Modifier.fillMaxSize(),
                    scale = scale
                )
            } else {
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEachIndexed { i, (_, total) ->
                        val target = if (total <= 0L) 0.04f else (total.toFloat() / max.toFloat()).coerceIn(0.09f, 1f)
                        val frac by animateFloatAsState(
                            targetValue = target,
                            animationSpec = tween(
                                Motion.d(300, scale),
                                delayMillis = Motion.d(i * 14, scale),
                                easing = FastOutSlowInEasing
                            ),
                            label = "batang$i"
                        )
                        val active = i == activeIndex
                        Box(
                            Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                Modifier
                                    .width(barW)
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        scaleY = frac
                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                    }
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                    .background(if (active) t.accent else t.bar)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            data.forEachIndexed { i, (cal, _) ->
                val active = i == activeIndex
                Text(
                    Fmt.monthShort(cal),
                    Type.micro.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
                    if (active) t.ink else t.muted2,
                    Modifier.weight(1f),
                    align = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LineChart(
    t: Tok,
    data: List<Pair<Calendar, Long>>,
    max: Long,
    activeIndex: Int,
    modifier: Modifier,
    scale: Float
) {
    val reveal = remember(data) { Animatable(0f) }
    LaunchedEffect(data) {
        if (scale <= 0f) reveal.snapTo(1f) else reveal.animateTo(1f, tween(Motion.d(380, scale), easing = FastOutSlowInEasing))
    }
    val p = reveal.value
    val n = data.size

    Canvas(modifier) {
        val top = 4.dp.toPx()
        val bottom = size.height
        val usable = bottom - top
        val step = if (n <= 1) size.width else size.width / (n - 1)
        fun point(i: Int): Offset {
            val v = data[i].second.toFloat() / max.toFloat()
            return Offset(i * step, bottom - v.coerceIn(0f, 1f) * usable)
        }

        drawLine(t.hair, Offset(0f, bottom - 0.5f), Offset(size.width, bottom - 0.5f), 1.dp.toPx())

        for (i in 0 until n - 1) {
            val seg = (p * (n - 1) - i).coerceIn(0f, 1f)
            if (seg <= 0f) break
            val a = point(i)
            val b = point(i + 1)
            val end = Offset(a.x + (b.x - a.x) * seg, a.y + (b.y - a.y) * seg)
            drawLine(
                color = if (i == activeIndex) t.accent else t.muted2,
                start = a,
                end = end,
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        for (i in 0 until n) {
            val shown = p * (n - 1) >= i - 0.001f
            if (!shown) break
            val pt = point(i)
            if (i == activeIndex) {
                drawCircle(t.accent, 4.dp.toPx(), pt)
                drawCircle(t.sheet, 1.8.dp.toPx(), pt)
            } else {
                drawCircle(t.bg, 2.6.dp.toPx(), pt)
                drawCircle(t.muted2, 2.6.dp.toPx(), pt, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
            }
        }
    }
}

@Composable
private fun CatSection(
    t: Tok,
    spent: List<Pair<Cat, Long>>,
    onCatat: () -> Unit,
    showTopHair: Boolean = true
) {
    val scale = motionScale()

    Column(
        Modifier
            .fillMaxWidth()
            .then(if (showTopHair) Modifier.topHair(t.hairSoft) else Modifier)
            .padding(top = 10.dp, bottom = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Spending per kategori", Type.section, t.ink)
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    spent.isEmpty() -> "\u2014"
                    spent.size > 4 -> "4 dari ${spent.size} kategori"
                    else -> "${spent.size} kategori"
                },
                Type.micro,
                t.muted2
            )
        }

        if (spent.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Belum ada pengeluaran bulan ini.", Type.label, t.muted2)
                Spacer(Modifier.weight(1f))
                Text("Catat", Type.label.copy(fontWeight = FontWeight.SemiBold), t.accent, Modifier.tap(onCatat))
            }
            return@Column
        }

        val top = spent.take(4)
        val total = top.sumOf { it.second }.coerceAtLeast(1L)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            top.forEachIndexed { i, (_, amount) ->
                val frac by animateFloatAsState(
                    targetValue = amount.toFloat() / total.toFloat(),
                    animationSpec = tween(Motion.d(300, scale), delayMillis = Motion.d(i * 35, scale)),
                    label = "kategori$i"
                )
                Box(
                    Modifier
                        .weight(frac.coerceAtLeast(0.001f))
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
                                Modifier.size(6.dp).clip(RoundedCornerShape(2.dp))
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
private fun NabungRow(
    t: Tok,
    saku: Saku,
    onNabung: () -> Unit,
    showTopHair: Boolean = true
) {
    if (!saku.showNabungHome) return
    val savedByGoal = remember(saku.tx) { saku.savedMap() }
    val goals = saku.goals
    val pinned = saku.goalOf(saku.nabungPinned) ?: goals.maxByOrNull { savedByGoal[it.id] ?: 0L }

    Column(
        Modifier
            .fillMaxWidth()
            .then(if (showTopHair) Modifier.topHair(t.hairSoft) else Modifier)
            .padding(vertical = 10.dp)
    ) {
        if (saku.nabungScroll && goals.isNotEmpty()) {
            LazyRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(goals, key = { it.id }) { g ->
                    GoalChip(t, savedByGoal, g, onNabung)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().tap(onNabung), verticalAlignment = Alignment.CenterVertically) {
                if (pinned?.image?.isNotEmpty() == true) {
                    GoalImage(pinned.image, t, 26.dp)
                } else {
                    Box(
                        Modifier.size(26.dp).clip(RoundedCornerShape(9.dp)).background(t.accentWash),
                        contentAlignment = Alignment.Center
                    ) {
                        Glyph(Ic.Wallet, t.accent, 15.dp)
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    if (pinned == null) {
                        Text("Nabung", Type.bodySm.copy(fontWeight = FontWeight.SemiBold), t.ink)
                        Text(
                            "Belum ada target. Buat yang pertama.",
                            Type.micro,
                            t.muted2,
                            Modifier.padding(top = 2.dp)
                        )
                    } else {
                        val saved = savedByGoal[pinned.id] ?: 0L
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (saku.nabungPinned.isNotEmpty()) {
                                Glyph(Ic.Pin, t.accent, 12.dp, 1.4.dp)
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                pinned.name,
                                Type.bodySm.copy(fontWeight = FontWeight.SemiBold),
                                t.ink,
                                Modifier.weight(1f)
                            )
                            Text(Fmt.num(saved) + " / " + Fmt.num(pinned.target), Type.figure, t.muted2)
                        }
                        Spacer(Modifier.height(6.dp))
                        Track(t, if (pinned.target > 0) saved.toFloat() / pinned.target.toFloat() else 0f)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalChip(
    t: Tok,
    savedByGoal: Map<String, Long>,
    goal: Goal,
    onNabung: () -> Unit
) {
    val saved = savedByGoal[goal.id] ?: 0L
    Column(
        Modifier
            .width(186.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, t.hairSoft, RoundedCornerShape(12.dp))
            .tap(onNabung)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoalImage(goal.image, t, 30.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                goal.name,
                Type.bodySm.copy(fontWeight = FontWeight.SemiBold),
                t.ink,
                Modifier.weight(1f),
                maxLines = 1
            )
        }
        Spacer(Modifier.height(7.dp))
        Track(t, if (goal.target > 0) saved.toFloat() / goal.target.toFloat() else 0f)
        Spacer(Modifier.height(6.dp))
        Text(Fmt.num(saved) + " / " + Fmt.num(goal.target), Type.micro, t.muted2)
    }
}

@Composable
private fun ListSection(
    t: Tok,
    saku: Saku,
    monthKey: Int,
    rows: List<Tx>,
    running: List<Long>,
    flashId: String?,
    onCatat: () -> Unit,
    onOpen: (Tx) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().topHair(t.hairSoft).padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Transaksi", Type.section, t.ink)
            Spacer(Modifier.weight(1f))
            Text(if (rows.isEmpty()) "belum ada" else "${rows.size} transaksi", Type.micro, t.muted2)
        }
        Spacer(Modifier.height(6.dp))
        TableHead(t)

        val listState = rememberLazyListState()
        LaunchedEffect(monthKey) { listState.scrollToItem(0) }

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
                Box(Modifier.rowEnter(i, monthKey)) {
                    TxRow(
                        t = t,
                        saku = saku,
                        tx = tx,
                        saldo = running.getOrElse(i) { 0L },
                        newest = i == 0,
                        flash = tx.id == flashId,
                        hideBalance = saku.hideBalance
                    ) { onOpen(tx) }
                }
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
fun TxRow(
    t: Tok,
    saku: Saku,
    tx: Tx,
    saldo: Long,
    newest: Boolean,
    flash: Boolean,
    hideBalance: Boolean = false,
    onClick: () -> Unit
) {
    val isTransfer = tx.type == "transfer"
    val cat = if (isTransfer) null else saku.catOf(tx.cat)
    val catName = if (isTransfer) "Pindah" else (cat?.name ?: "Lainnya")
    val note = tx.note.ifBlank { catName }
    val photoCount = tx.allPhotos.size
    val hasGoal = tx.goal.isNotEmpty()
    val dayShort = remember(tx.date) { Fmt.weekdayShort(tx.date) }
    val accLabel = if (saku.multiAccount && !isTransfer) saku.accountOf(tx.account)?.name else null

    // V1.6.8 #4: hindari alokasi Animatable pada ratusan baris yang tidak sedang flash.
    val flashAlpha = if (flash) {
        val anim = remember(tx.id) { Animatable(1f) }
        LaunchedEffect(tx.id) {
            anim.animateTo(0f, tween(850, easing = FastOutSlowInEasing))
        }
        0.16f * anim.value
    } else 0f

    val kindWord = when {
        isTransfer -> "pindah saldo "
        tx.type == "in" -> "masuk "
        else -> "keluar "
    }
    val rowDesc = "${tx.date}, $note, $kindWord${Fmt.num(tx.amount)}, saldo ${Fmt.num(saldo)}" +
        (if (photoCount > 0) ", $photoCount foto" else "") +
        (if (hasGoal) ", target nabung" else "")

    Column(
        Modifier
            .fillMaxWidth()
            .then(if (flashAlpha > 0f) Modifier.background(t.accent.copy(alpha = flashAlpha)) else Modifier)
            .semantics(mergeDescendants = true) {
                contentDescription = rowDesc
            }
    ) {
        Row(
            Modifier.fillMaxWidth().height(50.dp).tap(onClick),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.width(40.dp)) {
                Text(Fmt.tgl(tx.date), Type.label, t.muted2)
                Text(dayShort, Type.micro, t.muted2, Modifier.padding(top = 1.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(note, Type.body, t.ink, maxLines = 1)
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Glyph(
                        if (isTransfer) Ic.Transfer else glyphOf(cat?.glyph ?: "dot"),
                        if (isTransfer) t.accent else t.muted2,
                        11.dp,
                        1.2.dp
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        catName,
                        Type.micro,
                        t.muted2,
                        Modifier.weight(1f, fill = false),
                        maxLines = 1
                    )
                    if (accLabel != null) {
                        Box(
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(2.5.dp)
                                .clip(CircleShape)
                                .background(t.muted2.copy(alpha = 0.55f))
                        )
                        Text(accLabel, Type.micro, t.muted2, maxLines = 1)
                    }
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(2.5.dp)
                            .clip(CircleShape)
                            .background(t.muted2.copy(alpha = 0.55f))
                    )
                    Text(tx.time, Type.micro, t.muted2)
                    if (photoCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Glyph(Ic.Image, t.accent, 10.dp, 1.2.dp)
                    }
                    if (hasGoal) {
                        Spacer(Modifier.width(3.dp))
                        Glyph(Ic.Pin, t.accent, 10.dp, 1.2.dp)
                    }
                }
            }
            Text(
                when {
                    isTransfer -> Fmt.num(tx.amount)
                    tx.type == "in" -> "+" + Fmt.num(tx.amount)
                    else -> "\u2212" + Fmt.num(tx.amount)
                },
                Type.amount,
                when {
                    isTransfer -> t.accent
                    tx.type == "in" -> t.pos
                    else -> t.error
                },
                Modifier.width(82.dp),
                align = TextAlign.End
            )
            Text(
                Fmt.masked(saldo, hideBalance),
                if (newest) Type.figure.copy(fontWeight = FontWeight.SemiBold) else Type.figure,
                if (newest) t.accent else t.muted2,
                Modifier.width(76.dp),
                align = TextAlign.End
            )
        }
        Hair(t.hairSoft)
    }
}
