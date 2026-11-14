package id.saku.app

import androidx.compose.ui.graphics.Color

data class Tok(
    val bg: Color,
    val panel: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val dim: Color,
    val lime: Color,
    val pos: Color,
    val neg: Color,
    val track: Color,
    val bar: Color,
    val glass: Color,
    val glassRim: Color
)

val Dark = Tok(
    bg = Color(0xFF05080A),
    panel = Color(0xFF0B1013),
    line = Color(0x24FCFCFE),
    ink = Color(0xFFFCFCFE),
    muted = Color(0xFF8B9499),
    dim = Color(0xFF5F6A70),
    lime = Color(0xFFC8FF00),
    pos = Color(0xFF7BE495),
    neg = Color(0xFFFF7A6B),
    track = Color(0x14FCFCFE),
    bar = Color(0x38FCFCFE),
    glass = Color(0x8A121212),
    glassRim = Color(0x3DFFFFFF)
)

val Light = Tok(
    bg = Color(0xFFEBEAE4),
    panel = Color(0xFFF7F6F1),
    line = Color(0x24161A1C),
    ink = Color(0xFF16191B),
    muted = Color(0xFF575E62),
    dim = Color(0xFF8B9296),
    lime = Color(0xFF6F8E00),
    pos = Color(0xFF1F7A4E),
    neg = Color(0xFFAC4630),
    track = Color(0x17161A1C),
    bar = Color(0x2E161A1C),
    glass = Color(0xA6FAFAFA),
    glassRim = Color(0xAAFFFFFF)
)
