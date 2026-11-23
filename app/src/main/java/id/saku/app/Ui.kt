package id.saku.app

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Skala durasi DESIGN.md § Motion; 0 kalau animasi sistem dimatikan. */
object Motion {
    const val PRESS = 120
    const val ENTER = 180
    const val SHEET = 240
    const val SCREEN = 320

    fun d(base: Int, scale: Float): Int = if (scale <= 0f) 0 else (base * scale).toInt().coerceAtLeast(1)
}

@Composable
fun motionScale(): Float {
    val ctx = LocalContext.current
    val scale = remember(ctx) {
        try {
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        } catch (e: Exception) {
            1f
        }
    }
    return if (scale <= 0f) 0f else scale.coerceAtMost(1.5f)
}

internal fun Modifier.tap(onClick: () -> Unit): Modifier = clickable(
    interactionSource = MutableInteractionSource(), indication = null, onClick = onClick
)

/**
 * Ketuk dengan umpan balik tekan: skala + redup, transform/opacity saja.
 * V1.5 #7: besaran bisa diatur per komponen (tombol lebih terasa daripada
 * baris menu). Nilai state dibaca di lambda `graphicsLayer`, jadi animasi
 * berjalan di fase gambar tanpa memicu recomposition induk.
 */
@Composable
fun Modifier.pressable(
    scale: Float = 0.97f,
    alpha: Float = 0.9f,
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = tween(Motion.PRESS),
        label = "tekanS"
    )
    val a by animateFloatAsState(
        targetValue = if (pressed) alpha else 1f,
        animationSpec = tween(Motion.PRESS),
        label = "tekanA"
    )
    return this
        .graphicsLayer {
            scaleX = s
            scaleY = s
            this.alpha = a
        }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/**
 * V1.3 #4 — baris transaksi yang baru muncul masuk dengan fade + naik 16px.
 * Stagger 26ms per baris (maksimal 8 baris pertama) supaya daftar terasa dicetak
 * berurutan, bukan muncul serentak. `motionScale() == 0` langsung snap ke 1.
 */
@Composable
fun Modifier.rowEnter(index: Int, resetKey: Any = Unit): Modifier {
    // V1.6.8 #4: baris di luar 8 pertama langsung tampil tanpa Animatable/LaunchedEffect
    // supaya pengguliran daftar panjang tidak membuat ratusan coroutine per detik.
    if (index >= 8) return this
    val scale = motionScale()
    if (scale <= 0f) return this
    val anim = remember(resetKey) { Animatable(0f) }
    LaunchedEffect(resetKey) {
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = Motion.d(Motion.ENTER, scale),
                delayMillis = Motion.d(index * 22, scale),
                easing = FastOutSlowInEasing
            )
        )
    }
    return this.graphicsLayer {
        alpha = anim.value
        translationY = (1f - anim.value) * 14f
    }
}

@Composable
fun Text(
    text: String,
    style: TextStyle,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    align: TextAlign? = null
) {
    var s = style
    if (color != Color.Unspecified) s = s.copy(color = color)
    if (align != null) s = s.copy(textAlign = align)
    BasicText(
        text = text,
        modifier = modifier,
        style = s,
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines
    )
}

/** Garis rambut horizontal — pengganti kartu dan bayangan. */
@Composable
fun Hair(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

internal fun Modifier.topHair(color: Color): Modifier = drawBehind {
    val w = 1.dp.toPx()
    drawLine(color, Offset(0f, w / 2f), Offset(size.width, w / 2f), w)
}

/** Batang progress / track nabung, radius 2dp. V1.5 #7: spring, tapi tetap dijepit. */
@Composable
fun Track(t: Tok, frac: Float, height: Dp = 4.dp, color: Color = t.pos) {
    val animated by animateFloatAsState(
        targetValue = frac.coerceIn(0f, 1f),
        // WAJIB clamp nilai animasi: spring bisa overshoot > 1 dan
        // fillMaxWidth(fraction) melempar exception kalau fraksi di luar 0..1.
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 320f),
        label = "progress"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(t.hair)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

enum class Ic {
    Home, Receipt, Wallet, Sliders,
    Plus, Minus, Close, ChevL, ChevR, Check, Up, Down, Trash, Pencil,
    Food, Car, Bag, Film, Heart, Money, Dot, Palette, Chart, Tag,
    // V1.4: Time Input, foto, backup, dan pin nabung
    Clock, Camera, Image, Backup, Restore, Pin,
    // V1.6.8: ikon mata saldo, notifikasi, pindah saldo, kartu/bank, onboarding
    Eye, EyeOff, Bell, Transfer, Card, Sparkle
}

fun glyphOf(name: String): Ic = when (name) {
    "food" -> Ic.Food
    "car" -> Ic.Car
    "home" -> Ic.Home
    "bag" -> Ic.Bag
    "film" -> Ic.Film
    "heart" -> Ic.Heart
    "money" -> Ic.Money
    "wallet" -> Ic.Wallet
    "image" -> Ic.Image
    "pin" -> Ic.Pin
    "card" -> Ic.Card
    "transfer" -> Ic.Transfer
    "bell" -> Ic.Bell
    else -> Ic.Dot
}

/** Satu set ikon buatan sendiri: grid 20, stroke 1.5, ujung membulat. */
@Composable
fun Glyph(ic: Ic, tint: Color, box: Dp = 20.dp, stroke: Dp = 1.5.dp) {
    Canvas(Modifier.size(box)) {
        val s = size.minDimension / 20f
        val w = stroke.toPx()
        val cap = StrokeCap.Round

        fun path(build: Path.() -> Unit) {
            drawPath(Path().apply(build), tint, style = Stroke(width = w, cap = cap, join = StrokeJoin.Round))
        }

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), w, cap)

        fun rect(x: Float, y: Float, ww: Float, hh: Float, r: Float) = drawRoundRect(
            color = tint,
            topLeft = Offset(x * s, y * s),
            size = Size(ww * s, hh * s),
            cornerRadius = CornerRadius(r * s),
            style = Stroke(width = w, cap = cap, join = StrokeJoin.Round)
        )

        fun circle(cx: Float, cy: Float, r: Float, filled: Boolean = false) {
            if (filled) drawCircle(tint, r * s, Offset(cx * s, cy * s))
            else drawCircle(tint, r * s, Offset(cx * s, cy * s), style = Stroke(width = w))
        }

        when (ic) {
            Ic.Home -> path {
                moveTo(3f * s, 8.6f * s); lineTo(10f * s, 3f * s); lineTo(17f * s, 8.6f * s)
                lineTo(17f * s, 16f * s); lineTo(13f * s, 16f * s); lineTo(13f * s, 11f * s)
                lineTo(7f * s, 11f * s); lineTo(7f * s, 16f * s); lineTo(3f * s, 16f * s)
                close()
            }

            Ic.Receipt -> {
                path {
                    moveTo(5f * s, 3f * s); lineTo(15f * s, 3f * s); lineTo(15f * s, 17f * s)
                    lineTo(13f * s, 15.6f * s); lineTo(11.3f * s, 17f * s); lineTo(10f * s, 15.6f * s)
                    lineTo(8.7f * s, 17f * s); lineTo(7f * s, 15.6f * s); lineTo(5f * s, 17f * s)
                    close()
                }
                line(7.6f, 7f, 12.4f, 7f); line(7.6f, 10.4f, 12.4f, 10.4f)
            }

            Ic.Wallet, Ic.Money -> {
                if (ic == Ic.Wallet) {
                    rect(2.6f, 6f, 14.8f, 9.4f, 2f)
                    line(2.6f, 9.4f, 17.4f, 9.4f)
                    path {
                        moveTo(6f * s, 6f * s); lineTo(6f * s, 4.6f * s); lineTo(14f * s, 4.6f * s)
                        lineTo(14f * s, 6f * s)
                    }
                } else {
                    rect(2.4f, 5.6f, 15.2f, 8.8f, 1.6f)
                    circle(10f, 10f, 2f)
                }
            }

            Ic.Sliders -> {
                line(3f, 6f, 5.3f, 6f); line(8.7f, 6f, 17f, 6f); circle(7f, 6f, 1.7f)
                line(3f, 10f, 11.3f, 10f); line(14.7f, 10f, 17f, 10f); circle(13f, 10f, 1.7f)
                line(3f, 14f, 6.9f, 14f); line(10.3f, 14f, 17f, 14f); circle(8.6f, 14f, 1.7f)
            }

            Ic.Plus -> { line(10f, 4.6f, 10f, 15.4f); line(4.6f, 10f, 15.4f, 10f) }
            Ic.Minus -> line(4.6f, 10f, 15.4f, 10f)
            Ic.Close -> { line(5.6f, 5.6f, 14.4f, 14.4f); line(14.4f, 5.6f, 5.6f, 14.4f) }

            Ic.ChevL -> path {
                moveTo(12f * s, 4f * s); lineTo(6f * s, 10f * s); lineTo(12f * s, 16f * s)
            }

            Ic.ChevR -> path {
                moveTo(8f * s, 4f * s); lineTo(14f * s, 10f * s); lineTo(8f * s, 16f * s)
            }

            Ic.Check -> path {
                moveTo(4f * s, 10.6f * s); lineTo(8.4f * s, 15f * s); lineTo(16f * s, 5.4f * s)
            }

            Ic.Up -> { line(10f, 16f, 10f, 4f); path { moveTo(5.4f * s, 8.6f * s); lineTo(10f * s, 4f * s); lineTo(14.6f * s, 8.6f * s) } }
            Ic.Down -> { line(10f, 4f, 10f, 16f); path { moveTo(5.4f * s, 11.4f * s); lineTo(10f * s, 16f * s); lineTo(14.6f * s, 11.4f * s) } }

            Ic.Trash -> {
                line(3.6f, 5.6f, 16.4f, 5.6f)
                rect(5.6f, 5.6f, 8.8f, 11.4f, 1.6f)
                path {
                    moveTo(8f * s, 5.6f * s); lineTo(8f * s, 3.6f * s); lineTo(12f * s, 3.6f * s)
                    lineTo(12f * s, 5.6f * s)
                }
            }

            Ic.Pencil -> {
                path {
                    moveTo(4f * s, 16f * s); lineTo(4.8f * s, 12.4f * s); lineTo(12.4f * s, 4.8f * s)
                    lineTo(15.2f * s, 7.6f * s); lineTo(7.6f * s, 15.2f * s); close()
                }
                line(11.2f, 6f, 14f, 8.8f)
            }

            Ic.Food -> {
                line(2.6f, 9.6f, 17.4f, 9.6f)
                path {
                    moveTo(3.4f * s, 9.6f * s)
                    cubicTo(3.4f * s, 14f * s, 6.4f * s, 16.6f * s, 10f * s, 16.6f * s)
                    cubicTo(13.6f * s, 16.6f * s, 16.6f * s, 14f * s, 16.6f * s, 9.6f * s)
                }
            }

            Ic.Car -> {
                rect(3f, 8.6f, 14f, 5.2f, 1.6f)
                path {
                    moveTo(5.6f * s, 8.6f * s); lineTo(6.8f * s, 5.6f * s); lineTo(13.2f * s, 5.6f * s)
                    lineTo(14.4f * s, 8.6f * s)
                }
                circle(6.6f, 15f, 1.4f); circle(13.4f, 15f, 1.4f)
            }

            Ic.Bag -> {
                path {
                    moveTo(5.4f * s, 7f * s); lineTo(14.6f * s, 7f * s); lineTo(15.2f * s, 15.8f * s)
                    lineTo(14f * s, 17f * s); lineTo(6f * s, 17f * s); lineTo(4.8f * s, 15.8f * s)
                    close()
                }
                path {
                    moveTo(7.4f * s, 7f * s); lineTo(7.4f * s, 5.6f * s)
                    cubicTo(7.4f * s, 4.4f * s, 8.6f * s, 3.8f * s, 10f * s, 3.8f * s)
                    cubicTo(11.4f * s, 3.8f * s, 12.6f * s, 4.4f * s, 12.6f * s, 5.6f * s)
                    lineTo(12.6f * s, 7f * s)
                }
            }

            Ic.Film -> {
                rect(3f, 5.4f, 14f, 9.2f, 1.6f)
                line(7.6f, 5.4f, 7.6f, 14.6f); line(12.4f, 5.4f, 12.4f, 14.6f)
            }

            Ic.Heart -> path {
                moveTo(10f * s, 16.4f * s)
                cubicTo(5f * s, 13f * s, 3.4f * s, 10.4f * s, 4.6f * s, 8.2f * s)
                cubicTo(5.6f * s, 6.4f * s, 8.4f * s, 6.4f * s, 10f * s, 8.4f * s)
                cubicTo(11.6f * s, 6.4f * s, 14.4f * s, 6.4f * s, 15.4f * s, 8.2f * s)
                cubicTo(16.6f * s, 10.4f * s, 15f * s, 13f * s, 10f * s, 16.4f * s)
                close()
            }

            Ic.Palette -> {
                circle(10f, 10f, 7.4f)
                circle(7.2f, 8f, 1.1f, filled = true)
                circle(12.8f, 8f, 1.1f, filled = true)
                circle(7.6f, 12.6f, 1.1f, filled = true)
                circle(12.6f, 12.4f, 1.1f, filled = true)
            }

            Ic.Chart -> {
                line(3f, 16f, 17f, 16f)
                path {
                    moveTo(4f * s, 13f * s); lineTo(8f * s, 8f * s); lineTo(11.4f * s, 11f * s)
                    lineTo(16f * s, 5f * s)
                }
            }

            Ic.Tag -> {
                path {
                    moveTo(3.4f * s, 10f * s); lineTo(10f * s, 3.4f * s); lineTo(16.6f * s, 3.4f * s)
                    lineTo(16.6f * s, 10f * s); lineTo(10f * s, 16.6f * s); close()
                }
                circle(13f, 7f, 1.2f)
            }

            Ic.Dot -> {
                circle(5f, 10f, 1.3f, filled = true)
                circle(10f, 10f, 1.3f, filled = true)
                circle(15f, 10f, 1.3f, filled = true)
            }

            Ic.Clock -> {
                circle(10f, 10f, 7.2f)
                line(10f, 5.4f, 10f, 10.2f)
                line(10f, 10.2f, 13.2f, 12.2f)
            }

            Ic.Camera -> {
                rect(2.6f, 6.2f, 14.8f, 10f, 2f)
                rect(7f, 4.4f, 4f, 1.8f, 0.8f)
                circle(10f, 11.2f, 3f)
                circle(15.2f, 8.4f, 0.8f, filled = true)
            }

            Ic.Image -> {
                rect(3f, 4.4f, 14f, 11.2f, 1.6f)
                circle(7.2f, 8.2f, 1.3f)
                path {
                    moveTo(3.6f * s, 14.4f * s); lineTo(8f * s, 10f * s)
                    lineTo(11f * s, 13f * s); lineTo(14.4f * s, 9.8f * s)
                    lineTo(16.4f * s, 11.6f * s)
                }
            }

            Ic.Backup -> {
                rect(3.4f, 12.6f, 13.2f, 3.8f, 1.4f)
                line(10f, 3.4f, 10f, 11.6f)
                path { moveTo(6.8f * s, 6.6f * s); lineTo(10f * s, 3.4f * s); lineTo(13.2f * s, 6.6f * s) }
            }

            Ic.Restore -> {
                rect(3.4f, 3.6f, 13.2f, 3.8f, 1.4f)
                line(10f, 7.8f, 10f, 16.4f)
                path { moveTo(6.8f * s, 13.2f * s); lineTo(10f * s, 16.4f * s); lineTo(13.2f * s, 13.2f * s) }
            }

            Ic.Pin -> {
                circle(10f, 7.6f, 3.2f)
                line(10f, 10.8f, 10f, 17f)
            }

            Ic.Eye -> {
                path {
                    moveTo(2.4f * s, 10f * s)
                    cubicTo(5.2f * s, 5.4f * s, 14.8f * s, 5.4f * s, 17.6f * s, 10f * s)
                    cubicTo(14.8f * s, 14.6f * s, 5.2f * s, 14.6f * s, 2.4f * s, 10f * s)
                    close()
                }
                circle(10f, 10f, 2.3f)
            }

            Ic.EyeOff -> {
                path {
                    moveTo(2.4f * s, 10f * s)
                    cubicTo(5.2f * s, 5.4f * s, 14.8f * s, 5.4f * s, 17.6f * s, 10f * s)
                    cubicTo(14.8f * s, 14.6f * s, 5.2f * s, 14.6f * s, 2.4f * s, 10f * s)
                }
                line(3.6f, 3.6f, 16.4f, 16.4f)
            }

            Ic.Bell -> {
                path {
                    moveTo(5.2f * s, 13.6f * s)
                    lineTo(5.2f * s, 9.2f * s)
                    cubicTo(5.2f * s, 6.1f * s, 7.3f * s, 4f * s, 10f * s, 4f * s)
                    cubicTo(12.7f * s, 4f * s, 14.8f * s, 6.1f * s, 14.8f * s, 9.2f * s)
                    lineTo(14.8f * s, 13.6f * s)
                    lineTo(16.4f * s, 15.2f * s)
                    lineTo(3.6f * s, 15.2f * s)
                    close()
                }
                line(8.4f, 17.2f, 11.6f, 17.2f)
            }

            Ic.Transfer -> {
                line(3.6f, 7.4f, 16.2f, 7.4f)
                path { moveTo(12.8f * s, 4.4f * s); lineTo(16.2f * s, 7.4f * s); lineTo(12.8f * s, 10.4f * s) }
                line(16.4f, 13f, 3.8f, 13f)
                path { moveTo(7.2f * s, 10f * s); lineTo(3.8f * s, 13f * s); lineTo(7.2f * s, 16f * s) }
            }

            Ic.Card -> {
                rect(2.4f, 5f, 15.2f, 10f, 1.8f)
                line(2.4f, 8.6f, 17.6f, 8.6f)
                line(5.2f, 12.2f, 9f, 12.2f)
            }

            Ic.Sparkle -> {
                path {
                    moveTo(10f * s, 3f * s)
                    lineTo(11.8f * s, 8.2f * s)
                    lineTo(17f * s, 10f * s)
                    lineTo(11.8f * s, 11.8f * s)
                    lineTo(10f * s, 17f * s)
                    lineTo(8.2f * s, 11.8f * s)
                    lineTo(3f * s, 10f * s)
                    lineTo(8.2f * s, 8.2f * s)
                    close()
                }
            }
        }
    }
}

/** V1.6.8 — sakelar on/off ringkas tanpa Material (target sentuh >= 44dp + Role.Switch). */
@Composable
fun ToggleSwitch(t: Tok, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val thumbX by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
        label = "toggle"
    )
    Box(
        Modifier
            .width(48.dp)
            .height(44.dp)
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
            }
            .tap { onToggle(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(46.dp)
                .height(26.dp)
                .clip(CircleShape)
                .background(if (checked) t.accent else t.hair)
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .graphicsLayer { translationX = thumbX * 20.dp.toPx() }
                    .clip(CircleShape)
                    .background(if (checked) (if (t.dark) t.bg else Color.White) else t.sheet)
            )
        }
    }
}

/** Kotak field: garis rambut, radius 10dp, tinggi tetap supaya seluruh kotak bisa diketuk. */
@Composable
fun FieldShell(
    t: Tok,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    height: Dp = 48.dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(t.bg)
            .border(1.dp, if (error) t.error else t.hair, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Field teks dengan placeholder. */
@Composable
fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    t: Tok,
    modifier: Modifier = Modifier,
    style: TextStyle = Type.body,
    keyboardType: KeyboardType = KeyboardType.Text,
    onFocus: ((Boolean) -> Unit)? = null
) {
    val focus = remember { FocusRequester() }
    Box(modifier.fillMaxWidth().tap { focus.requestFocus() }, contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(placeholder, style, t.muted2)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = style.copy(color = t.ink),
            cursorBrush = SolidColor(t.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .then(if (onFocus != null) Modifier.onFocusChanged { onFocus(it.isFocused) } else Modifier)
        )
    }
}

/** Pemisah ribuan saat mengetik: 18500 → 18.500, kursor tetap di tempat. */
object Grouping : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val src = text.text
        if (src.length <= 3) return TransformedText(text, OffsetMapping.Identity)

        val out = StringBuilder()
        val map = IntArray(src.length + 1)
        var t = 0
        for (i in src.indices) {
            if (i > 0 && (src.length - i) % 3 == 0) {
                out.append('.')
                t++
            }
            map[i] = t
            out.append(src[i])
            t++
        }
        map[src.length] = t

        val offset = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = map[offset.coerceIn(0, src.length)]
            override fun transformedToOriginal(offset: Int): Int {
                for (i in map.indices) if (map[i] >= offset) return i
                return src.length
            }
        }
        return TransformedText(AnnotatedString(out.toString()), offset)
    }
}

/** Field nominal: angka saja, tampil dengan pemisah ribuan. */
@Composable
fun AmountField(
    digits: String,
    onDigits: (String) -> Unit,
    t: Tok,
    modifier: Modifier = Modifier,
    style: TextStyle = Type.display,
    placeholder: String = "0",
    onFocus: ((Boolean) -> Unit)? = null
) {
    val focus = remember { FocusRequester() }
    Box(modifier.fillMaxWidth().tap { focus.requestFocus() }, contentAlignment = Alignment.CenterStart) {
        if (digits.isEmpty()) Text(placeholder, style, t.muted2)
        BasicTextField(
            value = digits,
            onValueChange = { v -> onDigits(v.filter { it.isDigit() }.trimStart('0').take(12)) },
            textStyle = style.copy(color = t.ink),
            cursorBrush = SolidColor(t.accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = Grouping,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .then(if (onFocus != null) Modifier.onFocusChanged { onFocus(it.isFocused) } else Modifier)
        )
    }
}

/** Tombol utama: isi aksen, teks putih, radius bulat penuh, tinggi 48dp. */
@Composable
fun PrimaryButton(
    label: String,
    t: Tok,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDisabledClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(if (enabled) t.accent else t.accent.copy(alpha = 0.38f))
            .semantics { contentDescription = label }
            .pressable(scale = 0.98f, alpha = 0.92f) {
                if (enabled) onClick() else onDisabledClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            Type.body.copy(fontWeight = FontWeight.SemiBold),
            if (enabled) (if (t.dark) t.bg else Color.White) else t.muted2,
            align = TextAlign.Center
        )
    }
}

/** Tombol sekunder: garis rambut 1dp, teks tinta. */
@Composable
fun SecondaryButton(
    label: String,
    t: Tok,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .border(1.dp, t.hair, CircleShape)
            .pressable(scale = 0.98f, alpha = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, Type.body.copy(fontWeight = FontWeight.SemiBold), t.ink, align = TextAlign.Center)
    }
}

/** Target sentuh minimum 44dp untuk ikon aksi. */
@Composable
fun TouchTarget(content: @Composable () -> Unit) {
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { content() }
}

/** Kontrol segmen dua/tiga pilihan. */
@Composable
fun Segmented(
    t: Tok,
    options: List<String>,
    selected: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, t.hair, RoundedCornerShape(10.dp))
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) t.accentWash else Color.Transparent)
                    .pressable(scale = 0.97f, alpha = 0.92f) { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    Type.label.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                    if (on) t.accent else t.muted
                )
            }
        }
    }
}

/** Baris menu folder di Atur. */
@Composable
fun NavRow(t: Tok, icon: Ic, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .semantics { contentDescription = title }
            .pressable(scale = 0.985f, alpha = 0.94f, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(t.accentWash),
            contentAlignment = Alignment.Center
        ) {
            Glyph(icon, t.accent, 16.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, Type.body, t.ink)
            Text(subtitle, Type.micro, t.muted2, Modifier.padding(top = 2.dp))
        }
        Glyph(Ic.ChevR, t.muted2, 16.dp)
    }
}

@Composable
fun ScreenHeader(t: Tok, title: String, back: Boolean, onBack: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (back) {
            TouchTarget {
                Box(
                    Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Kembali" }
                        .tap(onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Glyph(Ic.ChevL, t.ink, 18.dp)
                }
            }
            Spacer(Modifier.width(4.dp))
        }
        Text(title, Type.title, t.ink, Modifier.weight(1f))
    }
}

/** Kerangka sheet: scrim, slide 240ms, bisa digulir, menghormati IME. */
@Composable
fun SheetScaffold(
    t: Tok,
    onDismiss: () -> Unit,
    visible: Boolean = true,
    footer: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale = motionScale()
    val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    val d = Motion.d(Motion.SHEET, scale)
    // V1.4 #8: sheet bisa ikut memakai liquid glass kalau area "sheet" dipilih.
    // Aman karena SheetScaffold dirender di level aplikasi sebagai SAUDARA dari
    // Box sakuLayerBackdrop, bukan turunannya (lihat catatan crash di Glass.kt).
    val glass = LocalGlassUi.current
    val backdrop = glass.backdrop
    val chrome = if (glass.sheet && backdrop != null) {
        Modifier.glassPanel(t, backdrop, shape)
    } else {
        Modifier.background(t.sheet).border(1.dp, t.hairSoft, shape)
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = shown && visible,
            enter = fadeIn(tween(Motion.d(Motion.ENTER, scale))),
            exit = fadeOut(tween(d / 2))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (t.dark) Color(0x99000000) else Color(0x591B2721))
                    .tap(onDismiss)
            )
        }
        BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
            val maxSheet = maxHeight * 0.94f
            AnimatedVisibility(
                visible = shown && visible,
                enter = slideInVertically(
                    animationSpec = tween(d, easing = FastOutSlowInEasing)
                ) { it },
                exit = slideOutVertically(animationSpec = tween(d, easing = FastOutSlowInEasing)) { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxSheet)
                        .clip(shape)
                        .then(chrome)
                ) {
                    Column(
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                            .padding(top = 12.dp, bottom = 26.dp)
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(38.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(t.hair)
                        )
                        Spacer(Modifier.height(14.dp))
                        content()
                    }
                    if (footer != null) {
                        Hair(t.hairSoft)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 12.dp, bottom = 20.dp)
                        ) {
                            footer()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SheetTitle(t: Tok, title: String, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Type.title, t.ink, Modifier.weight(1f), maxLines = 2)
        TouchTarget {
            Box(
                Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Tutup" }
                    .tap(onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Glyph(Ic.Close, t.muted2, 18.dp)
            }
        }
    }
}

/** V1.3 #5: pemilih jam berpindah ke TimeField di Sheet.kt (stepper + preset). */
