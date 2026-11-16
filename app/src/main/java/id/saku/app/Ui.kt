package id.saku.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun Modifier.tap(onClick: () -> Unit): Modifier = clickable(
    interactionSource = MutableInteractionSource(), indication = null, onClick = onClick
)

/** Teks tunggal aplikasi ini: Archivo/Newsreader lewat `style`, warna eksplisit. */
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

/** Garis rambut di tepi atas sebuah section. */
internal fun Modifier.topHair(color: Color): Modifier = drawBehind {
    val w = 1.dp.toPx()
    drawLine(color, Offset(0f, w / 2f), Offset(size.width, w / 2f), w)
}

/** Batang progress / track nabung, radius 2dp sesuai DESIGN.md § Spacing. */
@Composable
fun Track(t: Tok, frac: Float, height: Dp = 4.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(t.hair)
    ) {
        Box(
            Modifier
                .fillMaxWidth(frac.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(t.pos)
        )
    }
}

enum class Ic {
    Home, Receipt, Wallet, Sliders,
    Plus, Close, ChevL, ChevR, Check, Up, Down, Trash,
    Food, Car, Bag, Film, Heart, Money, Dot
}

fun glyphOf(name: String): Ic = when (name) {
    "food" -> Ic.Food
    "car" -> Ic.Car
    "home" -> Ic.Home
    "bag" -> Ic.Bag
    "film" -> Ic.Film
    "heart" -> Ic.Heart
    "money" -> Ic.Money
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

            Ic.Dot -> {
                circle(5f, 10f, 1.3f, filled = true)
                circle(10f, 10f, 1.3f, filled = true)
                circle(15f, 10f, 1.3f, filled = true)
            }
        }
    }
}

/** Kotak field dengan label di atasnya — dipakai sheet, Nabung, dan Atur. */
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

/** Field teks: seluruh kotak bisa diketuk untuk fokus, placeholder hilang saat mengetik. */
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
            .tap { if (enabled) onClick() else onDisabledClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            Type.body.copy(fontWeight = FontWeight.SemiBold),
            if (enabled) (if (t.dark) t.bg else Color.White) else t.faint,
            align = TextAlign.Center
        )
    }
}

/** Target sentuh minimum 44dp untuk ikon aksi. */
@Composable
fun TouchTarget(content: @Composable () -> Unit) {
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { content() }
}
