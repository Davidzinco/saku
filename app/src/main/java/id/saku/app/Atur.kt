package id.saku.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class AturPage { Root, Tema, Kategori, Grafik }

@Composable
fun Atur(saku: Saku, t: Tok, onChange: (Saku) -> Unit) {
    var page by remember { mutableStateOf(AturPage.Root) }
    val scale = motionScale()

    // back: dari sub-halaman kembali ke daftar Setting, bukan keluar aplikasi
    BackHandler(enabled = page != AturPage.Root) { page = AturPage.Root }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val back = targetState.ordinal < initialState.ordinal
            val d = Motion.d(Motion.SCREEN, scale)
            (slideInHorizontally(tween(d, easing = FastOutSlowInEasing)) { if (back) -it / 6 else it / 6 } +
                fadeIn(tween(d))) togetherWith
                (slideOutHorizontally(tween(d, easing = FastOutSlowInEasing)) { if (back) it / 6 else -it / 6 } +
                    fadeOut(tween(d / 2)))
        },
        label = "atur"
    ) { current ->
        when (current) {
            AturPage.Root -> AturRoot(saku, t) { page = it }
            AturPage.Tema -> TemaPage(saku, t, onChange) { page = AturPage.Root }
            AturPage.Kategori -> KategoriPage(saku, t, onChange) { page = AturPage.Root }
            AturPage.Grafik -> GrafikPage(saku, t, onChange) { page = AturPage.Root }
        }
    }
}

@Composable
private fun AturRoot(saku: Saku, t: Tok, go: (AturPage) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Setting", back = false)
        Spacer(Modifier.height(16.dp))

        NavRow(
            t = t,
            icon = Ic.Palette,
            title = "Theme",
            subtitle = (if (saku.dark) "Dark" else "Light") + " \u00b7 " + saku.palette().name,
            onClick = { go(AturPage.Tema) }
        )
        Hair(t.hairSoft)
        NavRow(
            t = t,
            icon = Ic.Tag,
            title = "Category",
            subtitle = saku.cats.size.toString() + " kategori",
            onClick = { go(AturPage.Kategori) }
        )
        Hair(t.hairSoft)
        NavRow(
            t = t,
            icon = Ic.Chart,
            title = "Chart",
            subtitle = (if (saku.chart == "garis") "Garis" else "Batang") + " \u00b7 " + saku.months + " bulan",
            onClick = { go(AturPage.Grafik) }
        )
        Hair(t.hairSoft)

        Spacer(Modifier.height(26.dp))
        Text("Data", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        Hair(t.hair)
        InfoRow(t, "Transaksi", saku.tx.size.toString())
        InfoRow(t, "Target nabung", saku.goals.size.toString())
        InfoRow(t, "Tersimpan", Fmt.rp(saku.savedTotal()))
        InfoRow(t, "Saldo akhir", Fmt.rp(saku.balance()))

        Spacer(Modifier.height(24.dp))
        Text("Saku \u00b7 Compose", Type.label, t.ink)
        Text(
            "Data tersimpan di perangkat ini. Tanpa akun, tanpa server.",
            Type.micro,
            t.muted2,
            Modifier.padding(top = 4.dp),
            maxLines = 2
        )
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun TemaPage(saku: Saku, t: Tok, onChange: (Saku) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Theme", back = true, onBack = onBack)
        Spacer(Modifier.height(14.dp))

        Text("Mode", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Light", "Dark"), if (saku.dark) 1 else 0) { i ->
            onChange(saku.copy(dark = i == 1))
        }

        Spacer(Modifier.height(24.dp))
        Text("Palette", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Palettes.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { p ->
                    PaletteCard(
                        t = t,
                        palette = p,
                        selected = saku.paletteId == p.id,
                        modifier = Modifier.weight(1f)
                    ) { onChange(saku.copy(paletteId = p.id)) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Custom", Type.label, t.muted2)
            Spacer(Modifier.weight(1f))
            if (saku.paletteId == "custom") {
                Text("dipakai", Type.micro, t.accent)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentSwatches.chunked(4).forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    group.forEach { hex ->
                        val on = saku.paletteId == "custom" && saku.customAccent == hex
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(if (on) 2.dp else 1.dp, if (on) t.ink else t.hair, CircleShape)
                                .tap { onChange(saku.copy(paletteId = "custom", customAccent = hex)) }
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Tones.forEach { tone ->
                    val on = saku.paletteId == "custom" && saku.customTone == tone.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, if (on) t.ink else t.hair, RoundedCornerShape(10.dp))
                            .tap { onChange(saku.copy(paletteId = "custom", customTone = tone.id)) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier.size(16.dp).clip(RoundedCornerShape(5.dp))
                                .background(Color(tone.lBg))
                                .border(1.dp, t.hair, RoundedCornerShape(5.dp))
                        )
                        Text(
                            tone.name,
                            Type.label.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                            if (on) t.ink else t.muted
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Palet mengubah kertas, tinta, dan aksen sekaligus. Semua pasangan tetap lolos kontras teks.",
            Type.micro,
            t.muted2,
            maxLines = 3
        )
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun PaletteCard(t: Tok, palette: Palette, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) t.ink else t.hair, RoundedCornerShape(10.dp))
            .tap(onClick)
            .padding(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(
                Modifier.size(16.dp).clip(RoundedCornerShape(5.dp))
                    .background(Color(palette.tone.lBg))
                    .border(1.dp, t.hair, RoundedCornerShape(5.dp))
            )
            Box(Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(Color(palette.accent)))
            Box(
                Modifier.size(16.dp).clip(RoundedCornerShape(5.dp))
                    .background(Color(palette.tone.lInk))
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            palette.name,
            Type.label.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            if (selected) t.ink else t.muted
        )
    }
}

@Composable
private fun GrafikPage(saku: Saku, t: Tok, onChange: (Saku) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Chart", back = true, onBack = onBack)
        Spacer(Modifier.height(14.dp))

        Text("Bentuk", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Batang", "Garis"), if (saku.chart == "garis") 1 else 0) { i ->
            onChange(saku.copy(chart = if (i == 1) "garis" else "batang"))
        }

        Spacer(Modifier.height(24.dp))
        Text("Rentang", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("6 bulan", "12 bulan"), if (saku.months >= 12) 1 else 0) { i ->
            onChange(saku.copy(months = if (i == 1) 12 else 6))
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "6 bulan: Jan\u2013Jun atau Jul\u2013Des. 12 bulan: Jan\u2013Des. Jendela tidak bergeser saat pindah bulan; " +
                "batang yang tersorot mengikuti bulan yang sedang dilihat.",
            Type.micro,
            t.muted2,
            maxLines = 5
        )
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun KategoriPage(saku: Saku, t: Tok, onChange: (Saku) -> Unit, onBack: () -> Unit) {
    var newName by remember { mutableStateOf("") }
    var glyphIdx by remember { mutableStateOf(0) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Category", back = true, onBack = onBack)
        Spacer(Modifier.height(6.dp))
        Hair(t.hair)

        saku.cats.forEachIndexed { i, cat ->
            val used = saku.countFor(cat.id)
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Glyph(glyphOf(cat.glyph), t.ink, 16.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(cat.name, Type.body, t.ink)
                    if (used > 0) {
                        Text("$used transaksi", Type.micro, t.muted2, Modifier.padding(top = 1.dp))
                    }
                }
                if (i > 0) {
                    TouchTarget {
                        Box(
                            Modifier.fillMaxSize().tap {
                                val list = saku.cats.toMutableList()
                                val item = list.removeAt(i)
                                list.add(i - 1, item)
                                onChange(saku.copy(cats = list))
                            },
                            contentAlignment = Alignment.Center
                        ) { Glyph(Ic.Up, t.muted2, 16.dp) }
                    }
                }
                if (i < saku.cats.lastIndex) {
                    TouchTarget {
                        Box(
                            Modifier.fillMaxSize().tap {
                                val list = saku.cats.toMutableList()
                                val item = list.removeAt(i)
                                list.add(i + 1, item)
                                onChange(saku.copy(cats = list))
                            },
                            contentAlignment = Alignment.Center
                        ) { Glyph(Ic.Down, t.muted2, 16.dp) }
                    }
                }
                TouchTarget {
                    Box(
                        Modifier.fillMaxSize().tap {
                            if (used == 0) onChange(saku.copy(cats = saku.cats.filter { it.id != cat.id }))
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Glyph(Ic.Trash, if (used == 0) t.muted2 else t.hair, 16.dp)
                    }
                }
            }
            Hair(t.hairSoft)
        }

        Spacer(Modifier.height(16.dp))
        Text("Kategori baru", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                FieldShell(t) {
                    PlainField(
                        value = newName,
                        onValueChange = { newName = it.take(18) },
                        placeholder = "Nama kategori",
                        t = t,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    TouchTarget {
                        Box(
                            Modifier.fillMaxSize().tap { glyphIdx = (glyphIdx + 1) % glyphChoices.size },
                            contentAlignment = Alignment.Center
                        ) {
                            Glyph(glyphOf(glyphChoices[glyphIdx]), t.muted2, 16.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (newName.isBlank()) t.hair else t.accent)
                    .tap {
                        if (newName.isNotBlank()) {
                            onChange(
                                saku.copy(
                                    cats = saku.cats + Cat(
                                        id = "c" + System.nanoTime().toString(36),
                                        name = newName.trim(),
                                        glyph = glyphChoices[glyphIdx]
                                    )
                                )
                            )
                            newName = ""
                            focus.clearFocus()
                        }
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tambah",
                    Type.label.copy(fontWeight = FontWeight.SemiBold),
                    if (newName.isBlank()) t.faint else (if (t.dark) t.bg else Color.White)
                )
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun InfoRow(t: Tok, label: String, value: String) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Type.body, t.ink, Modifier.weight(1f))
        Text(value, Type.figure, t.muted2)
    }
    Hair(t.hairSoft)
}
