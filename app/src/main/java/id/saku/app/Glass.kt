package id.saku.app

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * Liquid Glass dari Kyant0/AndroidLiquidGlass (Backdrop 2.0.1), disesuaikan untuk Saku.
 *
 * Resepnya sama dengan aplikasi liquid glass sebelumnya: layerBackdrop merekam seluruh
 * layar, lalu drawBackdrop memantulkan ulang latar itu dengan vibrancy + blur + lens,
 * plus highlight tepi dan inner shadow. Efek hanya aktif di Android 12+ (RenderEffect)
 * dan penuh di Android 13+ (RuntimeShader/AGSL); di bawah itu library otomatis
 * memakai permukaan biasa, jadi aplikasi tetap jalan.
 *
 * Catatan: `onDraw` sengaja distabilkan. Kalau lambda baru dibuat setiap recomposition,
 * rememberLayerBackdrop akan mengganti LayerBackdrop terus-menerus dan kacanya berkedip.
 */
@Composable
fun rememberSakuBackdrop(bg: Color): LayerBackdrop {
    val bgState = rememberUpdatedState(bg)
    val onDraw: ContentDrawScope.() -> Unit = remember {
        { drawRect(bgState.value); drawContent() }
    }
    // graphicsLayer diserahkan ke default rememberLayerBackdrop(); onDraw tetap stabil
    // supaya LayerBackdrop tidak dibuat ulang setiap recomposition.
    return rememberLayerBackdrop(onDraw = onDraw)
}

fun Modifier.sakuLayerBackdrop(backdrop: LayerBackdrop): Modifier = this.layerBackdrop(backdrop)

/** Permukaan kaca untuk dock. Warna tetap mengikuti token Saku, bukan warna library. */
fun Modifier.glassDock(t: Tok, backdrop: Backdrop, shape: Shape): Modifier =
    this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(6f.dp.toPx())
            lens(12f.dp.toPx(), 12f.dp.toPx())
        },
        highlight = { Highlight.Default },
        shadow = { Shadow(alpha = 0.55f, color = t.ink.copy(alpha = 0.16f)) },
        innerShadow = { InnerShadow(radius = 8f.dp, alpha = 0.28f) },
        onDrawSurface = { drawRect(t.sheet.copy(alpha = 0.55f)) }
    )

/** Permukaan kaca untuk tombol catat (FAB). */
fun Modifier.glassFab(t: Tok, backdrop: Backdrop): Modifier =
    this.drawBackdrop(
        backdrop = backdrop,
        shape = { CircleShape },
        effects = {
            vibrancy()
            blur(4f.dp.toPx())
            lens(10f.dp.toPx(), 10f.dp.toPx(), chromaticAberration = true)
        },
        highlight = { Highlight.Default },
        shadow = { Shadow(alpha = 0.8f, color = t.accent.copy(alpha = 0.35f)) },
        onDrawSurface = { drawRect(t.accent.copy(alpha = 0.88f)) }
    )

/** Kapsul tab aktif ikut membiaskan latar, seperti indikator di komponen LiquidBottomTabs. */
fun Modifier.glassWash(t: Tok, backdrop: Backdrop, shape: Shape): Modifier =
    this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = { lens(6f.dp.toPx(), 10f.dp.toPx()) },
        highlight = { Highlight.Default },
        onDrawSurface = { drawRect(t.accentWash) }
    )

fun dockShape(): RoundedCornerShape = RoundedCornerShape(22.dp)
fun washShape(): RoundedCornerShape = RoundedCornerShape(14.dp)
