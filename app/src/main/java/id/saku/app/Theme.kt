package id.saku.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Token desain — lihat DESIGN.md § Tokens. Nama field mengikuti peran, bukan warna.
 * Semua tema (bawaan maupun custom) dibangun dari [Palette]: satu nada kertas + satu aksen.
 */
data class Tok(
    val bg: Color,
    val sheet: Color,
    val ink: Color,
    val muted: Color,
    val muted2: Color,
    val faint: Color,
    val hair: Color,
    val hairSoft: Color,
    val accent: Color,
    val accentWash: Color,
    val pos: Color,
    val warn: Color,
    val error: Color,
    val bar: Color,
    val tint: List<Color>,
    val dark: Boolean
)

/** Nada kertas: menentukan bg, lembar, dan tinta di kedua mode. */
data class PaperTone(
    val id: String,
    val name: String,
    val lBg: Long,
    val lSheet: Long,
    val lInk: Long,
    val dBg: Long,
    val dSheet: Long,
    val dInk: Long
)

val Tones = listOf(
    PaperTone("hijau", "Hijau", 0xFFECEFE7, 0xFFF7F8F3, 0xFF1B2721, 0xFF121A16, 0xFF1A241F, 0xFFE7EDE5),
    PaperTone("krem", "Krem", 0xFFF1EBE1, 0xFFFAF6EF, 0xFF2B2119, 0xFF191513, 0xFF231D19, 0xFFF0E8DD),
    PaperTone("netral", "Netral", 0xFFEEEEEC, 0xFFF9F9F7, 0xFF1C1C1B, 0xFF131313, 0xFF1D1D1C, 0xFFEAEAE7),
    PaperTone("biru", "Biru", 0xFFE6ECF2, 0xFFF5F8FB, 0xFF182430, 0xFF111820, 0xFF1A232C, 0xFFE3EAF1)
)

data class Palette(val id: String, val name: String, val tone: PaperTone, val accent: Long)

val Palettes = listOf(
    Palette("kertas", "Kertas", Tones[0], 0xFF23418F),
    Palette("krem", "Krem & Terakota", Tones[1], 0xFFA8542F),
    Palette("baja", "Baja", Tones[3], 0xFF2C5D8A),
    Palette("hutan", "Hutan", Tones[0], 0xFF2F6B4F),
    Palette("ungu", "Ungu", Tones[2], 0xFF5B4BA8),
    Palette("mono", "Monokrom", Tones[2], 0xFF3A3A38)
)

/** Aksen untuk palet custom. */
val AccentSwatches = listOf(
    0xFF23418F, 0xFF2C5D8A, 0xFF0F6E6E, 0xFF2F6B4F,
    0xFF5B4BA8, 0xFFA8542F, 0xFFB03060, 0xFF3A3A38
)

fun toneOf(id: String): PaperTone = Tones.firstOrNull { it.id == id } ?: Tones[0]

fun buildTokens(p: Palette, dark: Boolean): Tok {
    val ink = Color(if (dark) p.tone.dInk else p.tone.lInk)
    val bg = Color(if (dark) p.tone.dBg else p.tone.lBg)
    val sheet = Color(if (dark) p.tone.dSheet else p.tone.lSheet)
    val accent = if (dark) lerp(Color(p.accent), Color.White, 0.42f) else Color(p.accent)

    val muted = lerp(ink, bg, 0.31f)
    val muted2 = lerp(ink, bg, if (dark) 0.43f else 0.34f)
    val faint = lerp(ink, bg, if (dark) 0.58f else 0.55f)
    val bar = if (dark) lerp(bg, ink, 0.12f) else lerp(bg, ink, 0.22f)

    val tint = if (dark) {
        listOf(ink, lerp(ink, bg, 0.35f), lerp(ink, bg, 0.62f), lerp(ink, bg, 0.80f))
    } else {
        listOf(ink, lerp(ink, bg, 0.45f), lerp(ink, bg, 0.72f), lerp(ink, bg, 0.88f))
    }

    return Tok(
        bg = bg,
        sheet = sheet,
        ink = ink,
        muted = muted,
        muted2 = muted2,
        faint = faint,
        hair = ink.copy(alpha = if (dark) 0.12f else 0.10f),
        hairSoft = ink.copy(alpha = if (dark) 0.07f else 0.06f),
        accent = accent,
        accentWash = accent.copy(alpha = if (dark) 0.14f else 0.07f),
        pos = Color(if (dark) 0xFF7FBE99 else 0xFF39704F),
        warn = Color(if (dark) 0xFFD8B45C else 0xFF7A5E12),
        error = Color(if (dark) 0xFFE38A7A else 0xFFA33A2B),
        bar = bar,
        tint = tint,
        dark = dark
    )
}

/** Skala tipografi DESIGN.md. Archivo untuk semua teks, Newsreader hanya untuk nominal ≥ 28sp. */
object Type {
    val sans = FontFamily(
        Font(R.font.archivo_regular, FontWeight.Normal),
        Font(R.font.archivo_medium, FontWeight.Medium),
        Font(R.font.archivo_semibold, FontWeight.SemiBold),
        Font(R.font.archivo_bold, FontWeight.Bold)
    )
    val serif = FontFamily(Font(R.font.newsreader_regular, FontWeight.Normal))

    private fun ui(size: TextUnit, lh: Float, weight: FontWeight, tracking: Float = 0f) = TextStyle(
        fontFamily = sans,
        fontSize = size,
        lineHeight = size * lh,
        fontWeight = weight,
        letterSpacing = tracking.em,
        fontFeatureSettings = "tnum"
    )

    private fun pr(size: TextUnit, lh: Float, tracking: Float = -0.018f) = TextStyle(
        fontFamily = serif,
        fontSize = size,
        lineHeight = size * lh,
        fontWeight = FontWeight.Normal,
        letterSpacing = tracking.em,
        fontFeatureSettings = "tnum"
    )

    /** hero 43sp, turun ke 34sp saat font scaling > 1.1 (DESIGN.md § Accessibility). */
    fun hero(fontScale: Float) = pr(if (fontScale > 1.1f) 34.sp else 43.sp, 1.06f)

    val display = pr(32.sp, 1.15f, -0.015f)
    val title = ui(19.sp, 1.3f, FontWeight.SemiBold)
    val body = ui(14.sp, 1.25f, FontWeight.Medium)
    val bodySm = ui(13.5.sp, 1.2f, FontWeight.Medium)
    val amount = ui(13.5.sp, 1.2f, FontWeight.Medium)
    val stat = ui(14.5.sp, 1.2f, FontWeight.Medium)
    val label = ui(11.5.sp, 1.2f, FontWeight.Medium)
    val micro = ui(10.5.sp, 1.2f, FontWeight.Medium)
    val figure = ui(11.sp, 1.2f, FontWeight.Medium)
    val tab = ui(10.sp, 1f, FontWeight.Medium)
    val section = ui(12.5.sp, 1.25f, FontWeight.SemiBold)
    val wordmark = ui(13.sp, 1f, FontWeight.Bold, 0.22f)
    val colHead = ui(9.sp, 1.2f, FontWeight.SemiBold, 0.08f)
    val currency = ui(16.sp, 1.2f, FontWeight.Medium)
}
