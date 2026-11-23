package id.saku.app

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * V1.6.8 #6 — Layar sambutan & halaman khusus pengaturan awal saat pertama kali
 * dibuka. Menjelaskan fitur utama Saku dan memberi toggle langsung untuk
 * menyalakan atau mematikan fitur yang diinginkan pengguna.
 */
@Composable
fun OnboardingFlow(
    saku: Saku,
    t: Tok,
    onChange: (Saku) -> Unit,
    onFinish: (Saku) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val totalSteps = 3

    Column(
        Modifier
            .fillMaxSize()
            .background(t.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Baris atas: indikator progres & tombol Lewati
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SAKU", Type.wordmark, t.ink)
            Spacer(Modifier.width(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(totalSteps) { i ->
                    Box(
                        Modifier
                            .width(if (i == step) 22.dp else 7.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (i <= step) t.accent else t.hair)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Lewati",
                Type.label.copy(fontWeight = FontWeight.SemiBold),
                t.muted2,
                Modifier
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                    .tap { onFinish(saku.copy(onboarded = true)) }
            )
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { if (forward) it / 5 else -it / 5 } +
                    fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(tween(220)) { if (forward) -it / 5 else it / 5 } + fadeOut(tween(180)))
            },
            modifier = Modifier.weight(1f),
            label = "onboarding"
        ) { current ->
            when (current) {
                0 -> WelcomeStep(t)
                1 -> FeatureToggleStep(saku, t, onChange) {
                    NotifHelper.openSettings(ctx)
                }
                else -> ThemeReadyStep(saku, t, onChange)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                Box(Modifier.width(104.dp)) {
                    SecondaryButton("Kembali", t) { step-- }
                }
            }
            Box(Modifier.weight(1f)) {
                PrimaryButton(
                    label = when (step) {
                        0 -> "Atur fitur saya"
                        1 -> "Pilih tema"
                        else -> "Mulai pakai Saku"
                    },
                    t = t
                ) {
                    if (step < totalSteps - 1) {
                        step++
                    } else {
                        onFinish(saku.copy(onboarded = true))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(t: Tok) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 18.dp, bottom = 12.dp)
    ) {
        // Kartu sambutan bersih tanpa gradien & eyebrow slop (DESIGN.md)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(t.sheet)
                .border(1.dp, t.hair, RoundedCornerShape(10.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "Catat pengeluaran dalam hitungan detik.",
                    Type.title.copy(fontSize = 23.sp, lineHeight = 30.sp),
                    t.ink,
                    maxLines = 3
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Semua data tersimpan lokal di perangkatmu. Tanpa akun, tanpa iklan, dan bisa dicadangkan ke ZIP kapan saja.",
                    Type.bodySm,
                    t.muted,
                    maxLines = 4
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Text("Yang bisa dilakukan Saku", Type.section, t.ink)
        Spacer(Modifier.height(12.dp))

        IntroPoint(
            t,
            Ic.Wallet,
            "Pisahkan Tunai & E-Wallet",
            "Pantau saldo per sumber dana atau gabungkan jadi satu saldo utama, lengkap dengan fitur pindah saldo."
        )
        IntroPoint(
            t,
            Ic.Bell,
            "Pengingat dari Notifikasi E-Wallet",
            "Deteksi notifikasi pembayaran GoPay, OVO, DANA, ShopeePay, dan m-banking untuk catat satu ketuk."
        )
        IntroPoint(
            t,
            Ic.Palette,
            "Liquid Glass & Tema Lengkap",
            "Pilih tampilan Beranda modern atau Buku Tabungan klasik, plus efek kaca dan belasan palet warna."
        )
    }
}

@Composable
private fun IntroPoint(t: Tok, icon: Ic, title: String, desc: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(t.sheet)
                .border(1.dp, t.hairSoft, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Glyph(icon, t.accent, 18.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, Type.body.copy(fontWeight = FontWeight.SemiBold), t.ink)
            Spacer(Modifier.height(2.dp))
            Text(desc, Type.micro, t.muted2, maxLines = 3)
        }
    }
}

/**
 * Halaman khusus untuk menjelaskan dan menyalakan/mematikan fitur satu per satu.
 */
@Composable
private fun FeatureToggleStep(
    saku: Saku,
    t: Tok,
    onChange: (Saku) -> Unit,
    onOpenNotifSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val hasNotifPerm = NotifHelper.hasAccess(ctx)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 10.dp, bottom = 12.dp)
    ) {
        Text("Atur fitur sesuai kebutuhanmu", Type.title, t.ink, maxLines = 2)
        Spacer(Modifier.height(4.dp))
        Text(
            "Nyalakan yang berguna dan matikan yang belum perlu. Semua bisa diubah lagi kapan saja di Pengaturan.",
            Type.micro,
            t.muted2,
            maxLines = 3
        )
        Spacer(Modifier.height(16.dp))

        FeatureToggleCard(
            t = t,
            icon = Ic.Home,
            title = "Tampilan Beranda Baru",
            desc = "Kartu saldo modern dengan efek tilt 3D, ringkasan sumber dana & kategori cepat. Matikan untuk tampilan Klasik.",
            checked = saku.homeStyle == "baru",
            onToggle = { on -> onChange(saku.copy(homeStyle = if (on) "baru" else "klasik")) }
        )
        Spacer(Modifier.height(10.dp))

        FeatureToggleCard(
            t = t,
            icon = Ic.Sparkle,
            title = "Efek Liquid Glass",
            desc = "Permukaan kaca tembus pandang pada dock navigasi dan tombol catat (penuh di Android 13+).",
            checked = saku.glass,
            onToggle = { on -> onChange(saku.copy(glass = on)) }
        )
        Spacer(Modifier.height(10.dp))

        FeatureToggleCard(
            t = t,
            icon = Ic.Wallet,
            title = "Pisahkan Tunai & E-Wallet",
            desc = "Lacak saldo Tunai, E-Wallet, dan sumber dana lain secara terpisah dengan fitur Pindah Saldo.",
            checked = saku.multiAccount,
            onToggle = { on -> onChange(saku.copy(multiAccount = on, splitBalance = on)) }
        )
        Spacer(Modifier.height(10.dp))

        FeatureToggleCard(
            t = t,
            icon = Ic.EyeOff,
            title = "Sembunyikan Saldo Awal",
            desc = "Nominal saldo ditutup saat aplikasi dibuka (bisa dibuka cepat lewat ikon mata di Beranda).",
            checked = saku.hideBalance,
            onToggle = { on -> onChange(saku.copy(hideBalance = on)) }
        )
        Spacer(Modifier.height(10.dp))

        FeatureToggleCard(
            t = t,
            icon = Ic.Bell,
            title = "Deteksi Notifikasi E-Wallet",
            desc = "Menawarkan pencatatan cepat saat ada notifikasi pembayaran dari GoPay, OVO, DANA, ShopeePay, atau m-banking.",
            checked = saku.notifDetect,
            onToggle = { on ->
                NotifHelper.syncEnabled(ctx, on)
                onChange(saku.copy(notifDetect = on))
            },
            extra = if (saku.notifDetect) {
                {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(t.accentWash)
                            .tap(onOpenNotifSettings)
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (hasNotifPerm) "Izin Akses Notifikasi sudah aktif" else "Ketuk untuk aktifkan Izin Akses Notifikasi",
                            Type.micro.copy(fontWeight = FontWeight.SemiBold),
                            t.accent,
                            Modifier.weight(1f)
                        )
                        Glyph(Ic.ChevR, t.accent, 14.dp)
                    }
                }
            } else null
        )
    }
}

@Composable
private fun FeatureToggleCard(
    t: Tok,
    icon: Ic,
    title: String,
    desc: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(t.sheet)
            .border(1.dp, if (checked) t.accent.copy(alpha = 0.35f) else t.hairSoft, RoundedCornerShape(14.dp))
            .tap { onToggle(!checked) }
            .padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (checked) t.accentWash else t.bg),
                contentAlignment = Alignment.Center
            ) {
                Glyph(icon, if (checked) t.accent else t.muted2, 17.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, Type.body.copy(fontWeight = FontWeight.SemiBold), t.ink)
            }
            Spacer(Modifier.width(8.dp))
            ToggleSwitch(t, checked, onToggle)
        }
        Spacer(Modifier.height(6.dp))
        Text(desc, Type.micro, t.muted2, maxLines = 3)
        extra?.invoke()
    }
}

@Composable
private fun ThemeReadyStep(
    saku: Saku,
    t: Tok,
    onChange: (Saku) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 10.dp, bottom = 12.dp)
    ) {
        Text("Pilih nuansa tampilan", Type.title, t.ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Mode gelap dan palet favorit bisa diganti kapan saja di Pengaturan › Tema.",
            Type.micro,
            t.muted2,
            maxLines = 2
        )
        Spacer(Modifier.height(16.dp))

        Text("Mode", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Segmented(t, listOf("Terang", "Gelap"), if (saku.dark) 1 else 0) { i ->
            onChange(saku.copy(dark = i == 1))
        }

        Spacer(Modifier.height(18.dp))
        Text("Palet populer", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Palettes.take(8).chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { p ->
                    val on = saku.paletteId == p.id
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) t.accentWash else t.sheet)
                            .border(1.dp, if (on) t.accent else t.hairSoft, RoundedCornerShape(12.dp))
                            .tap { onChange(saku.copy(paletteId = p.id)) }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(p.accent))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            p.name,
                            Type.label.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                            if (on) t.ink else t.muted,
                            Modifier.weight(1f)
                        )
                        if (on) Glyph(Ic.Check, t.accent, 15.dp)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
