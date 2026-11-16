package id.saku.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Token desain — lihat DESIGN.md § Tokens. Nama field mengikuti peran, bukan warna.
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

val Light = Tok(
    bg = Color(0xFFECEFE7),
    sheet = Color(0xFFF7F8F3),
    ink = Color(0xFF1B2721),
    muted = Color(0xFF5C6A62),
    muted2 = Color(0xFF5F6B62),
    faint = Color(0xFF8E998F),
    hair = Color(0x1A1B2721),
    hairSoft = Color(0x0F1B2721),
    accent = Color(0xFF23418F),
    accentWash = Color(0x1223418F),
    pos = Color(0xFF39704F),
    warn = Color(0xFF7A5E12),
    error = Color(0xFFA33A2B),
    bar = Color(0xFFCDD6CA),
    tint = listOf(Color(0xFF24332B), Color(0xFF71806F), Color(0xFFA9B4A7), Color(0xFFD3DAD0)),
    dark = false
)

val Dark = Tok(
    bg = Color(0xFF121A16),
    sheet = Color(0xFF1A241F),
    ink = Color(0xFFE7EDE5),
    muted = Color(0xFFA3AFA5),
    muted2 = Color(0xFF8B978D),
    faint = Color(0xFF6B7770),
    hair = Color(0x1FE7EDE5),
    hairSoft = Color(0x12E7EDE5),
    accent = Color(0xFF8FA5F2),
    accentWash = Color(0x1F8FA5F2),
    pos = Color(0xFF7FBE99),
    warn = Color(0xFFD8B45C),
    error = Color(0xFFE38A7A),
    bar = Color(0xFF2B3A32),
    tint = listOf(Color(0xFFE7EDE5), Color(0xFF9DAE9F), Color(0xFF5A6B5E), Color(0xFF33443A)),
    dark = true
)

/**
 * Skala tipografi DESIGN.md. Archivo untuk semua teks, Newsreader hanya untuk nominal ≥ 28sp.
 * Semua gaya membawa `tnum` supaya kolom angka rata.
 */
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
