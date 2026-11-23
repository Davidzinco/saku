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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AturPage {
    Root, Tema, UI, Home, Grafik, SumberDana, Kategori, NotifWidget, Cadangan, HapusData
}

@Composable
fun Atur(
    saku: Saku,
    t: Tok,
    onChange: (Saku) -> Unit,
    onReplace: (Saku) -> Unit = onChange,
    onOpenOnboarding: () -> Unit = {}
) {
    var page by remember { mutableStateOf(AturPage.Root) }
    val scale = motionScale()

    BackHandler(enabled = page != AturPage.Root) {
        page = if (page == AturPage.UI) AturPage.Tema else AturPage.Root
    }

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
            AturPage.Root -> AturRoot(saku, t, onOpenOnboarding) { page = it }
            AturPage.Tema -> TemaPage(saku, t, onChange, { page = AturPage.Root }) { page = AturPage.UI }
            AturPage.UI -> UIPage(saku, t, onChange) { page = AturPage.Tema }
            AturPage.Home -> HomePage(saku, t, onChange) { page = AturPage.Root }
            AturPage.Grafik -> GrafikPage(saku, t, onChange) { page = AturPage.Root }
            AturPage.SumberDana -> SumberDanaPage(saku, t, onChange) { page = AturPage.Root }
            AturPage.Kategori -> KategoriPage(saku, t, onChange) { page = AturPage.Root }
            AturPage.NotifWidget -> NotifWidgetPage(saku, t, onChange) { page = AturPage.Root }
            AturPage.Cadangan -> CadanganPage(saku, t, onReplace) { page = AturPage.Root }
            AturPage.HapusData -> HapusDataPage(saku, t, onChange, onReplace) { page = AturPage.Root }
        }
    }
}

@Composable
private fun SettingsGroupLabel(t: Tok, title: String) {
    Spacer(Modifier.height(18.dp))
    Text(title, Type.label.copy(fontWeight = FontWeight.SemiBold), t.muted2)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun AturRoot(
    saku: Saku,
    t: Tok,
    onOpenOnboarding: () -> Unit,
    go: (AturPage) -> Unit
) {
    val ctx = LocalContext.current
    val savedTotal = remember(saku.tx, saku.goals) { saku.savedTotal() }
    val balance = remember(saku.tx) { saku.balance() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Pengaturan", back = false)

        // Grup 1: Tampilan
        SettingsGroupLabel(t, "Tampilan")
        Hair(t.hair)
        NavRow(
            t = t,
            icon = Ic.Palette,
            title = "Tema & Kaca",
            subtitle = (if (saku.dark) "Gelap" else "Terang") + " \u00b7 " + saku.palette().name +
                " \u00b7 " + (if (saku.glass) "Kaca" else "Kertas"),
            onClick = { go(AturPage.Tema) }
        )
        Hair(t.hairSoft)
        NavRow(
            t = t,
            icon = Ic.Home,
            title = "Beranda",
            subtitle = (if (saku.homeStyle == "baru") "Tampilan Baru" else "Tampilan Klasik") +
                " \u00b7 " + (if (saku.showNabungHome) "Nabung tampil" else "Nabung tersembunyi"),
            onClick = { go(AturPage.Home) }
        )
        Hair(t.hairSoft)
        NavRow(
            t = t,
            icon = Ic.Chart,
            title = "Grafik",
            subtitle = (if (saku.chart == "garis") "Garis" else "Batang") + " \u00b7 " + saku.months + " bulan",
            onClick = { go(AturPage.Grafik) }
        )
        Hair(t.hairSoft)

        // Grup 2: Keuangan
        SettingsGroupLabel(t, "Keuangan")
        Hair(t.hair)
        NavRow(
            t = t,
            icon = Ic.Wallet,
            title = "Sumber Dana",
            subtitle = if (saku.multiAccount) {
                "${saku.accounts.size} sumber \u00b7 " + if (saku.splitBalance) "Saldo dipisah" else "Saldo digabung"
            } else {
                "Satu sumber dana tunggal"
            },
            onClick = { go(AturPage.SumberDana) }
        )
        Hair(t.hairSoft)
        NavRow(
            t = t,
            icon = Ic.Tag,
            title = "Kategori",
            subtitle = "${saku.cats.size} kategori",
            onClick = { go(AturPage.Kategori) }
        )
        Hair(t.hairSoft)

        // Grup 3: Otomatis & Pengingat
        SettingsGroupLabel(t, "Otomatis & Pengingat")
        Hair(t.hair)
        NavRow(
            t = t,
            icon = Ic.Bell,
            title = "Notifikasi & Widget",
            subtitle = if (saku.notifDetect) "Deteksi e-wallet aktif" else "Deteksi e-wallet nonaktif",
            onClick = { go(AturPage.NotifWidget) }
        )
        Hair(t.hairSoft)

        // Grup 4: Data & Cadangan
        SettingsGroupLabel(t, "Data & Cadangan")
        Hair(t.hair)
        NavRow(
            t = t,
            icon = Ic.Backup,
            title = "Cadangan ZIP",
            subtitle = "Ekspor / pulihkan data + foto",
            onClick = { go(AturPage.Cadangan) }
        )
        Hair(t.hairSoft)
        NavRow(
            t = t,
            icon = Ic.Trash,
            title = "Hapus Data",
            subtitle = "Hapus per bulan atau seluruh data",
            onClick = { go(AturPage.HapusData) }
        )
        Hair(t.hairSoft)
        NavRow(
            t = t,
            icon = Ic.Sparkle,
            title = "Panduan Awal",
            subtitle = "Buka ulang pengaturan fitur awal",
            onClick = onOpenOnboarding
        )
        Hair(t.hairSoft)

        Spacer(Modifier.height(22.dp))
        Text("Ringkasan Data", Type.label.copy(fontWeight = FontWeight.SemiBold), t.muted2)
        Spacer(Modifier.height(6.dp))
        Hair(t.hair)
        InfoRow(t, "Transaksi", saku.tx.size.toString())
        InfoRow(t, "Target nabung", saku.goals.size.toString())
        InfoRow(t, "Tersimpan", Fmt.rpMasked(savedTotal, saku.hideBalance))
        InfoRow(t, "Saldo", Fmt.rpMasked(balance, saku.hideBalance))
        InfoRow(t, "Versi", "V" + Fmt.appVersion(ctx))

        Spacer(Modifier.height(20.dp))
        Text("Saku \u00b7 V" + Fmt.appVersion(ctx), Type.label, t.ink)
        Text(
            "Data tersimpan lokal di perangkat ini. Tanpa akun, tanpa server.",
            Type.micro,
            t.muted2,
            Modifier.padding(top = 4.dp),
            maxLines = 2
        )
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun SumberDanaPage(
    saku: Saku,
    t: Tok,
    onChange: (Saku) -> Unit,
    onBack: () -> Unit
) {
    val balances = remember(saku.tx, saku.accounts) { saku.accountBalances() }
    var newName by remember { mutableStateOf("") }
    var kindIdx by remember { mutableIntStateOf(0) }
    val kinds = listOf(
        Triple("cash", "Tunai", "money"),
        Triple("ewallet", "E-Wallet", "wallet"),
        Triple("bank", "Bank", "card")
    )
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Sumber Dana", back = true, onBack = onBack)
        Spacer(Modifier.height(14.dp))

        Text("Mode sumber dana", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Tunggal", "Multi (Tunai / E-Wallet)"), if (saku.multiAccount) 1 else 0) { i ->
            onChange(saku.copy(multiAccount = i == 1))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (saku.multiAccount) {
                "Setiap transaksi memilih sumber dana dan fitur Pindah Saldo aktif."
            } else {
                "Semua transaksi dicatat dalam satu saldo tunggal tanpa memilih sumber dana."
            },
            Type.micro,
            t.muted2,
            maxLines = 3
        )

        if (saku.multiAccount) {
            Spacer(Modifier.height(20.dp))
            Text("Tampilan saldo di Beranda", Type.label, t.muted2)
            Spacer(Modifier.height(8.dp))
            Segmented(t, listOf("Gabung saja", "Pisahkan per sumber"), if (saku.splitBalance) 1 else 0) { i ->
                onChange(saku.copy(splitBalance = i == 1))
            }

            Spacer(Modifier.height(22.dp))
            Text("Daftar sumber dana", Type.label, t.muted2)
            Spacer(Modifier.height(6.dp))
            Hair(t.hair)

            saku.accounts.forEach { acc ->
                val used = saku.countForAccount(acc.id)
                val bal = balances[acc.id] ?: 0L
                val canDelete = saku.accounts.size > 1 && used == 0
                Row(
                    Modifier.fillMaxWidth().height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(t.accentWash),
                        contentAlignment = Alignment.Center
                    ) {
                        Glyph(glyphOf(acc.glyph), t.accent, 16.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(acc.name, Type.body.copy(fontWeight = FontWeight.SemiBold), t.ink)
                        Text(
                            Fmt.rpMasked(bal, saku.hideBalance) + if (used > 0) " \u00b7 $used transaksi" else "",
                            Type.micro,
                            t.muted2
                        )
                    }
                    TouchTarget {
                        Box(
                            Modifier.fillMaxSize().tap {
                                if (canDelete) {
                                    onChange(saku.copy(accounts = saku.accounts.filter { it.id != acc.id }))
                                }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Glyph(Ic.Trash, if (canDelete) t.muted2 else t.hair, 16.dp)
                        }
                    }
                }
                Hair(t.hairSoft)
            }

            Spacer(Modifier.height(18.dp))
            Text("Tambah sumber dana baru", Type.label, t.muted2)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                kinds.forEachIndexed { i, (_, label, glyph) ->
                    val on = kindIdx == i
                    Row(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) t.accentWash else Color.Transparent)
                            .border(1.dp, if (on) t.accent else t.hair, RoundedCornerShape(10.dp))
                            .tap { kindIdx = i },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Glyph(glyphOf(glyph), if (on) t.accent else t.muted2, 14.dp)
                        Spacer(Modifier.width(5.dp))
                        Text(label, Type.micro.copy(fontWeight = FontWeight.SemiBold), if (on) t.accent else t.muted)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    FieldShell(t) {
                        PlainField(
                            value = newName,
                            onValueChange = { newName = it.take(18) },
                            placeholder = "mis. GoPay, BCA, Dompet",
                            t = t,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
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
                                val (kind, _, glyph) = kinds[kindIdx]
                                onChange(
                                    saku.copy(
                                        accounts = saku.accounts + Account(
                                            id = "a" + System.nanoTime().toString(36),
                                            name = newName.trim(),
                                            kind = kind,
                                            glyph = glyph
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
                        if (newName.isBlank()) t.muted2 else (if (t.dark) t.bg else Color.White)
                    )
                }
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun NotifWidgetPage(
    saku: Saku,
    t: Tok,
    onChange: (Saku) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var accessTick by remember { mutableIntStateOf(0) }
    val hasAccess = remember(accessTick) { NotifHelper.hasAccess(ctx) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Notifikasi & Widget", back = true, onBack = onBack)
        Spacer(Modifier.height(14.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(t.sheet)
                .border(1.dp, t.hairSoft, RoundedCornerShape(14.dp))
                .tap {
                    val next = !saku.notifDetect
                    NotifHelper.syncEnabled(ctx, next)
                    onChange(saku.copy(notifDetect = next))
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Deteksi notifikasi e-wallet", Type.body.copy(fontWeight = FontWeight.SemiBold), t.ink)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Menawarkan pencatatan cepat saat ada notifikasi pembayaran masuk/keluar.",
                    Type.micro,
                    t.muted2,
                    maxLines = 3
                )
            }
            Spacer(Modifier.width(12.dp))
            ToggleSwitch(t, saku.notifDetect) { next ->
                NotifHelper.syncEnabled(ctx, next)
                onChange(saku.copy(notifDetect = next))
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (hasAccess) t.pos.copy(alpha = 0.12f) else t.accentWash)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (hasAccess) "Izin Akses Notifikasi aktif" else "Izin Akses Notifikasi belum aktif",
                    Type.label.copy(fontWeight = FontWeight.SemiBold),
                    if (hasAccess) t.pos else t.accent
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (hasAccess) {
                        "Saku hanya membaca notifikasi dari aplikasi e-wallet/bank yang didukung."
                    } else {
                        "Aktifkan Saku di pengaturan Akses Notifikasi Android agar fitur ini berjalan."
                    },
                    Type.micro,
                    t.muted2,
                    maxLines = 3
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                PrimaryButton("Buka Izin Notifikasi", t) {
                    NotifHelper.openSettings(ctx)
                }
            }
            Box(Modifier.width(96.dp)) {
                SecondaryButton("Cek lagi", t) { accessTick++ }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text("Aplikasi yang dikenali", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        Text(
            NotifHelper.supportedApps.map { it.second }.distinct().joinToString(" \u00b7 "),
            Type.bodySm,
            t.ink,
            maxLines = 4
        )
        Text(
            "Saku tidak pernah menyimpan transaksi secara diam-diam. Ketuk notifikasi Saku untuk meninjau lalu simpan.",
            Type.micro,
            t.muted2,
            Modifier.padding(top = 6.dp),
            maxLines = 3
        )

        Spacer(Modifier.height(24.dp))
        Hair(t.hair)
        Spacer(Modifier.height(16.dp))
        Text("Widget Layar Utama (4\u00d72)", Type.section, t.ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tambahkan widget Saku dari layar utama HP (tekan-tahan layar utama \u2192 Widget \u2192 Saku) untuk melihat saldo, ringkasan bulan ini, dan tombol + Catat cepat.",
            Type.micro,
            t.muted2,
            maxLines = 4
        )
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun HapusDataPage(
    saku: Saku,
    t: Tok,
    onChange: (Saku) -> Unit,
    onReplace: (Saku) -> Unit,
    onBack: () -> Unit
) {
    val months = remember(saku.tx) { saku.availableMonths() }
    var confirmMonth by remember { mutableStateOf<String?>(null) }
    var confirmAll by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Hapus Data", back = true, onBack = onBack)
        Spacer(Modifier.height(12.dp))
        Text(
            "Penghapusan bersifat permanen. Buat cadangan ZIP terlebih dahulu bila masih ingin menyimpan riwayat.",
            Type.micro,
            t.muted2,
            maxLines = 3
        )

        if (notice.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(notice, Type.label.copy(fontWeight = FontWeight.SemiBold), t.accent, maxLines = 2)
        }

        Spacer(Modifier.height(20.dp))
        Text("Hapus per bulan", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        Hair(t.hair)

        if (months.isEmpty()) {
            Text(
                "Belum ada transaksi yang tersimpan.",
                Type.bodySm,
                t.muted2,
                Modifier.padding(vertical = 16.dp)
            )
        } else {
            months.forEach { (ym, count) ->
                val armed = confirmMonth == ym
                Row(
                    Modifier.fillMaxWidth().height(54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(Fmt.ymLabel(ym), Type.body, t.ink)
                        Text("$count transaksi", Type.micro, t.muted2)
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (armed) t.error else t.hairSoft)
                            .tap {
                                if (armed) {
                                    val (next, photos) = saku.withoutMonth(ym)
                                    deletePhotos(photos)
                                    onChange(next)
                                    confirmMonth = null
                                    notice = "Transaksi ${Fmt.ymLabel(ym)} dihapus ($count transaksi)."
                                } else {
                                    confirmMonth = ym
                                    confirmAll = false
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            if (armed) "Yakin hapus?" else "Hapus",
                            Type.micro.copy(fontWeight = FontWeight.SemiBold),
                            if (armed) (if (t.dark) t.bg else Color.White) else t.error
                        )
                    }
                }
                Hair(t.hairSoft)
            }
        }

        Spacer(Modifier.height(26.dp))
        Text("Hapus seluruh data aplikasi", Type.label, t.error)
        Spacer(Modifier.height(6.dp))
        Text(
            "Menghapus semua transaksi, target nabung, foto struk, dan gambar target. Tema dan pengaturan aplikasi tetap dipertahankan.",
            Type.micro,
            t.muted2,
            maxLines = 3
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (confirmAll) t.error else Color.Transparent)
                .border(1.dp, t.error, RoundedCornerShape(24.dp))
                .tap {
                    if (confirmAll) {
                        val (next, files) = saku.clearedAll()
                        deletePhotos(files)
                        files.forEach { deleteGoalImage(it) }
                        onReplace(next)
                        confirmAll = false
                        confirmMonth = null
                        notice = "Seluruh transaksi dan target nabung berhasil dihapus."
                    } else {
                        confirmAll = true
                        confirmMonth = null
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (confirmAll) "Ketuk lagi untuk hapus SEMUA data" else "Hapus seluruh data",
                Type.body.copy(fontWeight = FontWeight.SemiBold),
                if (confirmAll) (if (t.dark) t.bg else Color.White) else t.error
            )
        }
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
        ScreenHeader(t, "Tema & Kaca", back = true, onBack = onBack)
        Spacer(Modifier.height(14.dp))

        NavRow(
            t = t,
            icon = Ic.Sliders,
            title = "Area Liquid Glass",
            subtitle = "Pilih bagian overlay yang memakai kaca",
            onClick = onUI
        )
        Hair(t.hairSoft)
        Spacer(Modifier.height(14.dp))

        Text("Mode", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Terang", "Gelap"), if (saku.dark) 1 else 0) { i ->
            onChange(saku.copy(dark = i == 1))
        }

        Spacer(Modifier.height(20.dp))
        Text("Material overlay", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Kertas", "Liquid Glass"), if (saku.glass) 1 else 0) { i ->
            onChange(saku.copy(glass = i == 1))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Sakelar utama untuk Dock, Tombol catat, dan Sheet. Efek penuh pada Android 13+.",
            Type.micro,
            t.muted2,
            maxLines = 3
        )

        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Koleksi Palet", Type.label, t.muted2)
            Spacer(Modifier.weight(1f))
            Text("${Palettes.size} tema", Type.micro, t.muted2)
        }
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

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Tema Custom", Type.label, t.muted2)
            Spacer(Modifier.weight(1f))
            Text(
                if (saku.paletteId == "custom") "sedang dipakai" else "ketuk warna untuk memakai",
                Type.micro,
                if (saku.paletteId == "custom") t.accent else t.muted2
            )
        }
        Spacer(Modifier.height(8.dp))
        CustomPreview(saku, t)

        Spacer(Modifier.height(16.dp))
        Text("Aksen (${AccentSwatches.size} warna)", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        AccentSwatches.chunked(6).forEach { group ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        if (on) Glyph(Ic.Check, Color.White, 17.dp, 2.dp)
                    }
                }
                repeat(6 - group.size) { Spacer(Modifier.size(38.dp)) }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(6.dp))
        Text("Nada latar (${Tones.size} pilihan)", Type.label, t.muted2)
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
                            .background(Color(if (saku.dark) tone.dBg else tone.lBg))
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

        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun CustomPreview(saku: Saku, t: Tok) {
    val preview = remember(saku.customAccent, saku.customTone, saku.dark) {
        buildTokens(Palette("custom", "Custom", toneOf(saku.customTone), saku.customAccent), saku.dark)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(preview.bg)
            .border(1.dp, t.hair, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Pratinjau langsung", Type.micro, preview.muted2)
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) t.accentWash else t.sheet)
            .border(1.dp, if (selected) t.accent else t.hairSoft, RoundedCornerShape(12.dp))
            .tap(onClick)
            .padding(11.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(16.dp).clip(RoundedCornerShape(5.dp))
                    .background(Color(if (t.dark) palette.tone.dBg else palette.tone.lBg))
                    .border(1.dp, t.hair, RoundedCornerShape(5.dp))
            )
            Box(
                Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(
                    if (t.dark) lerp(Color(palette.accent), Color.White, 0.42f) else Color(palette.accent)
                )
            )
            Box(
                Modifier.size(16.dp).clip(RoundedCornerShape(5.dp))
                    .background(Color(if (t.dark) palette.tone.dInk else palette.tone.lInk))
            )
            Spacer(Modifier.weight(1f))
            if (selected) Glyph(Ic.Check, t.accent, 14.dp)
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
        ScreenHeader(t, "Grafik", back = true, onBack = onBack)
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
    var glyphIdx by remember { mutableIntStateOf(0) }
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Kategori", back = true, onBack = onBack)
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
private fun UIPage(saku: Saku, t: Tok, onChange: (Saku) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Area Liquid Glass", back = true, onBack = onBack)
        Spacer(Modifier.height(12.dp))
        Text(
            "Pilih permukaan overlay yang memakai liquid glass asli (vibrancy + blur + lens).",
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
                Text("Material overlay masih Kertas \u2014 nyalakan di Tema & Kaca.", Type.micro, t.muted2)
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            "Permukaan di dalam halaman memakai efek kaca aman tanpa rekursi RenderNode, sedangkan " +
                "kaca pembiasan penuh dipasang pada overlay (Dock, Tombol catat, Sheet).",
            Type.micro,
            t.muted2,
            maxLines = 4
        )
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun HomePage(saku: Saku, t: Tok, onChange: (Saku) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Beranda", back = true, onBack = onBack)
        Spacer(Modifier.height(14.dp))

        // V1.6.8 #2: pilihan tampilan Beranda Baru vs Beranda Klasik (lama)
        Text("Gaya Beranda", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Baru (Kartu 3D)", "Klasik (Buku Tabungan)"), if (saku.homeStyle == "klasik") 1 else 0) { i ->
            onChange(saku.copy(homeStyle = if (i == 1) "klasik" else "baru"))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (saku.homeStyle == "baru") {
                "Kartu saldo 3D tilt-parallax, ringkasan sumber dana, dan kategori cepat. Grafik & riwayat lengkap di Transaksi."
            } else {
                "Tampilan satu lembar Buku Tabungan lama lengkap dengan grafik dan tabel transaksi di Beranda."
            },
            Type.micro,
            t.muted2,
            maxLines = 3
        )

        Spacer(Modifier.height(22.dp))
        Text("Privasi saldo", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Tampilkan", "Sembunyikan"), if (saku.hideBalance) 1 else 0) { i ->
            onChange(saku.copy(hideBalance = i == 1))
        }

        Spacer(Modifier.height(22.dp))
        Text("Ringkasan nabung", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Sembunyikan", "Tampilkan"), if (saku.showNabungHome) 1 else 0) { i ->
            onChange(saku.copy(showNabungHome = i == 1))
        }

        Spacer(Modifier.height(22.dp))
        Text("Bergulir dengan target lain", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Tidak", "Ya"), if (saku.nabungScroll) 1 else 0) { i ->
            onChange(saku.copy(nabungScroll = i == 1))
        }

        Spacer(Modifier.height(22.dp))
        Text("Pin di Beranda", Type.label, t.muted2)
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

@Composable
private fun CadanganPage(saku: Saku, t: Tok, onReplace: (Saku) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }

    val export = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) {
            status = "Ekspor dibatalkan."
        } else {
            status = "Membuat cadangan..."
            scope.launch {
                val res = withContext(Dispatchers.IO) { Backup.export(ctx, uri, saku) }
                status = res.fold(
                    onSuccess = { n -> "Backup tersimpan: ${saku.tx.size} transaksi, $n foto." },
                    onFailure = { "Gagal membuat backup: ${it.message}" }
                )
            }
        }
    }

    val impor = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            status = "Pulihkan dibatalkan."
        } else {
            status = "Memulihkan cadangan..."
            scope.launch {
                val res = withContext(Dispatchers.IO) { Backup.import(ctx, uri) }
                status = res.fold(
                    onSuccess = { (data, n) ->
                        onReplace(data.copy(onboarded = true))
                        "Pulihkan berhasil: ${data.tx.size} transaksi, $n foto."
                    },
                    onFailure = { "Arsip ditolak: ${it.message}" }
                )
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        ScreenHeader(t, "Cadangan ZIP", back = true, onBack = onBack)
        Spacer(Modifier.height(14.dp))
        Text(
            "Satu berkas ZIP berisi seluruh transaksi, sumber dana, kategori, target, pengaturan, " +
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
        InfoRow(t, "Sumber dana", saku.accounts.size.toString())
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
