package id.saku.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class AturPage { Root, Tema, Kategori, Grafik, UI, Home, Cadangan }

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
            AturPage.Tema -> TemaPage(saku, t, onChange, { page = AturPage.Root }) { page = AturPage.UI }
            AturPage.Kategori -> KategoriPage(saku, t, onChange) { page = AturPage.Root }
            AturPage.Grafik -> GrafikPage(saku, t, onChange) { page = AturPage.Root }
            AturPage.UI -> UIPage(saku, t, onChange) { page = AturPage.Tema }
            AturPage.Home -> HomePage(saku, t, onChange) { page = AturPage.Root }
            AturPage.Cadangan -> CadanganPage(saku, t, onChange) { page = AturPage.Root }
        }
    }
}

@Composable
private fun AturRoot(saku: Saku, t: Tok, go: (AturPage) -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Setting", back = false)
        Spacer(Modifier.height(16.dp))

        NavRow(
            t = t,
            icon = Ic.Palette,
            title = "Theme",
            subtitle = (if (saku.dark) "Dark" else "Light") + " \u00b7 " + saku.palette().name +
                " \u00b7 " + (if (saku.glass) "Kaca" else "Kertas"),
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
        NavRow(
            t = t,
            icon = Ic.Home,
            title = "Home",
            subtitle = if (saku.showNabungHome) "Nabung tampil" else "Nabung disembunyikan",
            onClick = { go(AturPage.Home) }
        )
        Hair(t.hairSoft)
        NavRow(
            t = t,
            icon = Ic.Backup,
            title = "Backup",
            subtitle = "Ekspor / pulihkan data + foto",
            onClick = { go(AturPage.Cadangan) }
        )
        Hair(t.hairSoft)

        Spacer(Modifier.height(26.dp))
        Text("Data", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        Hair(t.hair)
        InfoRow(t, "Transaksi", saku.tx.size.toString())
        InfoRow(t, "Target nabung", saku.goals.size.toString())
        InfoRow(t, "Tersimpan", Fmt.rp(saku.savedTotal()))
        InfoRow(t, "Saldo", Fmt.rp(saku.balance()))
        // V1.5 #8: versi dibaca dari PackageManager (versionName di Gradle),
        // jadi tidak pernah lagi tertinggal di "V1.0".
        InfoRow(t, "Versi", "V" + Fmt.appVersion(ctx))

        Spacer(Modifier.height(24.dp))
        Text("Saku \u00b7 V" + Fmt.appVersion(ctx), Type.label, t.ink)
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
private fun TemaPage(
    saku: Saku,
    t: Tok,
    onChange: (Saku) -> Unit,
    onBack: () -> Unit,
    onUI: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Theme", back = true, onBack = onBack)
        Spacer(Modifier.height(14.dp))

        // V1.4 #8: Theme > UI, pilih per bagian mana yang memakai liquid glass.
        NavRow(
            t = t,
            icon = Ic.Sliders,
            title = "UI",
            subtitle = "Pilih bagian yang memakai liquid glass",
            onClick = onUI
        )
        Hair(t.hairSoft)
        Spacer(Modifier.height(14.dp))

        Text("Mode", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Light", "Dark"), if (saku.dark) 1 else 0) { i ->
            onChange(saku.copy(dark = i == 1))
        }

        Spacer(Modifier.height(24.dp))
        Text("Mode dasar", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Kertas", "Kaca"), if (saku.glass) 1 else 0) { i ->
            onChange(saku.copy(glass = i == 1))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Sakelar utama untuk semua permukaan. Pilih Kaca lalu buka UI untuk menentukan " +
                "bagian mana saja yang ikut berubah. Efek penuh hanya di Android 13+; " +
                "di bawah itu otomatis tampil datar.",
            Type.micro,
            t.muted2,
            maxLines = 4
        )

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

        // V1.5 #6 — Custom dibuat lebih mudah: pratinjau langsung, aksen
        // 12 warna dalam grid 6 kolom, nada kertas jadi deretan chip yang
        // bisa digeser. Memilih salah satu langsung memakai palet custom.
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Custom", Type.label, t.muted2)
            Spacer(Modifier.weight(1f))
            Text(
                if (saku.paletteId == "custom") "dipakai" else "ketuk warna untuk memakai",
                Type.micro,
                if (saku.paletteId == "custom") t.accent else t.muted2
            )
        }
        Spacer(Modifier.height(8.dp))
        CustomPreview(saku, t)

        Spacer(Modifier.height(16.dp))
        Text("Aksen", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        AccentSwatches.chunked(6).forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.forEach { hex ->
                    val on = saku.paletteId == "custom" && saku.customAccent == hex
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(if (on) 2.dp else 1.dp, if (on) t.ink else t.hair, CircleShape)
                            .pressable(scale = 0.9f, alpha = 0.9f) {
                                onChange(saku.copy(paletteId = "custom", customAccent = hex))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (on) {
                            Glyph(Ic.Check, Color.White, 17.dp, 2.dp)
                        }
                    }
                }
                repeat(6 - group.size) { Spacer(Modifier.size(38.dp)) }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(6.dp))
        Text("Kertas", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tones.forEach { tone ->
                val on = saku.paletteId == "custom" && saku.customTone == tone.id
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, if (on) t.accent else t.hair, RoundedCornerShape(10.dp))
                        .background(if (on) t.accentWash else Color.Transparent)
                        .pressable(scale = 0.96f, alpha = 0.92f) {
                            onChange(saku.copy(paletteId = "custom", customTone = tone.id))
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(tone.lBg))
                            .border(1.dp, t.hair, RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        tone.name,
                        Type.label.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                        if (on) t.ink else t.muted
                    )
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

/** Pratinjau kombinasi custom yang sedang dipilih, sebelum dipakai penuh. */
@Composable
private fun CustomPreview(saku: Saku, t: Tok) {
    val preview = remember(saku.customAccent, saku.customTone, saku.dark) {
        buildTokens(Palette("custom", "Custom", toneOf(saku.customTone), saku.customAccent), saku.dark)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(preview.bg)
            .border(1.dp, t.hair, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Contoh tampilan", Type.micro, preview.muted2)
            Text("Rp 1.250.000", Type.title, preview.ink, Modifier.padding(top = 3.dp))
        }
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(preview.accent),
            contentAlignment = Alignment.Center
        ) {
            Glyph(Ic.Plus, if (preview.dark) preview.bg else Color.White, 18.dp)
        }
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

/**
 * V1.4 #8 — Setting > Theme > UI. Tiap bagian bisa dipilih sendiri antara
 * kertas dan liquid glass; mode dasar "Kertas" tetap mematikan semuanya.
 */
@Composable
private fun UIPage(saku: Saku, t: Tok, onChange: (Saku) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "UI", back = true, onBack = onBack)
        Spacer(Modifier.height(12.dp))
        Text(
            "Pilih permukaan overlay yang memakai liquid glass. Bagian lain tetap " +
                "kertas sesuai arah Buku Tabungan.",
            Type.micro,
            t.muted2,
            maxLines = 3
        )
        Spacer(Modifier.height(18.dp))

        glassAreas.forEach { (key, label) ->
            Text(label, Type.body, t.ink)
            Spacer(Modifier.height(6.dp))
            Segmented(
                t,
                listOf("Kertas", "Kaca"),
                if (saku.glassPref(key)) 1 else 0
            ) { i ->
                val m = saku.uiGlass.toMutableMap()
                m[key] = i == 1
                onChange(saku.copy(uiGlass = m, glass = true))
            }
            Spacer(Modifier.height(6.dp))
            if (!saku.glass) {
                Text("Mode dasar masih Kertas — nyalakan di Theme.", Type.micro, t.muted2)
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            "Semua permukaan memakai warna dari token tema. Tidak ada gradien, glow, " +
                "atau perubahan tata letak — hanya material permukaannya.",
            Type.micro,
            t.muted2,
            maxLines = 3
        )
        Spacer(Modifier.height(10.dp))
        // V1.4 crash fix: jelaskan kenapa area di dalam halaman tidak ditawarkan.
        Text(
            "Halaman (termasuk Ringkasan Home) selalu kertas: kaca hanya bisa " +
                "dipasang di atas lapisan yang direkam, bukan di dalamnya.",
            Type.micro,
            t.muted2,
            maxLines = 3
        )
        Spacer(Modifier.height(110.dp))
    }
}

/** V1.4 #10 — opsi khusus untuk ringkasan Nabung yang tampil di Home. */
@Composable
private fun HomePage(saku: Saku, t: Tok, onChange: (Saku) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Home", back = true, onBack = onBack)
        Spacer(Modifier.height(14.dp))

        Text("Ringkasan nabung", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Sembunyikan", "Tampilkan"), if (saku.showNabungHome) 1 else 0) { i ->
            onChange(saku.copy(showNabungHome = i == 1))
        }

        Spacer(Modifier.height(24.dp))
        Text("Bergulir dengan target lain", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Tidak", "Ya"), if (saku.nabungScroll) 1 else 0) { i ->
            onChange(saku.copy(nabungScroll = i == 1))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Ya: semua target tampil berdampingan dan bisa digeser. Tidak: hanya satu target yang di-pin.",
            Type.micro,
            t.muted2,
            maxLines = 3
        )

        Spacer(Modifier.height(24.dp))
        Text("Pin di Home", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        PickRow(t, "Otomatis (terbanyak)", saku.nabungPinned.isEmpty()) {
            onChange(saku.copy(nabungPinned = ""))
        }
        saku.goals.forEach { g ->
            PickRow(t, g.name, saku.nabungPinned == g.id) {
                onChange(saku.copy(nabungPinned = g.id))
            }
        }
        if (saku.goals.isEmpty()) {
            Text("Belum ada target nabung.", Type.micro, t.muted2, Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun PickRow(t: Tok, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(46.dp).tap(onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Type.body, if (selected) t.ink else t.muted2, Modifier.weight(1f))
        if (selected) Glyph(Ic.Check, t.accent, 17.dp)
    }
    Hair(t.hairSoft)
}

/** V1.4 #3 — ekspor / impor satu arsip ZIP lewat SAF, tanpa permission. */
@Composable
private fun CadanganPage(saku: Saku, t: Tok, onChange: (Saku) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var status by remember { mutableStateOf("") }

    val export = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) {
            status = "Ekspor dibatalkan."
        } else {
            status = Backup.export(ctx, uri, saku).fold(
                onSuccess = { n -> "Backup tersimpan: ${saku.tx.size} transaksi, $n foto." },
                onFailure = { "Gagal membuat backup: ${it.message}" }
            )
        }
    }

    val impor = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            status = "Pulihkan dibatalkan."
        } else {
            status = Backup.import(ctx, uri).fold(
                onSuccess = { (data, n) ->
                    onChange(data)
                    "Pulihkan berhasil: ${data.tx.size} transaksi, $n foto."
                },
                onFailure = { "Arsip ditolak: ${it.message}" }
            )
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Backup", back = true, onBack = onBack)
        Spacer(Modifier.height(14.dp))
        Text(
            "Satu berkas ZIP berisi seluruh transaksi, kategori, target, pengaturan, " +
                "dan foto. Simpan di Drive atau pindahkan ke HP lain, lalu pulihkan di sini.",
            Type.micro,
            t.muted2,
            maxLines = 4
        )

        Spacer(Modifier.height(22.dp))
        PrimaryButton("Buat backup", t) { export.launch(Backup.suggestedName()) }
        Spacer(Modifier.height(8.dp))
        SecondaryButton("Pulihkan dari backup", t) {
            impor.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
        }

        if (status.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(status, Type.label, t.ink, maxLines = 3)
        }

        Spacer(Modifier.height(26.dp))
        Text("Isi cadangan", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        Hair(t.hair)
        InfoRow(t, "Transaksi", saku.tx.size.toString())
        InfoRow(t, "Kategori", saku.cats.size.toString())
        InfoRow(t, "Target nabung", saku.goals.size.toString())
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
