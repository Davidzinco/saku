# Saku

Aplikasi keuangan pribadi untuk Android — catat pengeluaran, grafik tren, dan target nabung. Tanpa login, data tersimpan lokal.

## Teknologi

- Kotlin + Jetpack Compose **tanpa Material** (foundation + animation saja), AGP 9.2.1, Kotlin 2.4.10, Compose BOM 2026.09.00
- Tipografi dibundel sebagai resource: Archivo 400/500/600/700 + Newsreader 400 (`res/font/`)
- Ikon digambar sendiri di `Ui.kt` (grid 20, stroke 1.5) — bukan set ikon Material
- Penyimpanan: **DataStore Preferences** (`androidx.datastore:datastore-preferences:1.1.7`) dengan satu key per transaksi, tanpa akun dan tanpa server. `save()` hanya menulis record yang berubah; data lama dari `SharedPreferences` dimigrasikan otomatis sekali. Room sengaja tidak dipakai agar tidak perlu KSP/codegen pada AGP 9.2.1 + Kotlin 2.4.10. Aplikasi mulai dari nol, tanpa data contoh
- Liquid glass: `io.github.kyant0:backdrop:2.0.1` + `io.github.kyant0:shapes:1.2.1` ([Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)), bisa dipilih per **area overlay** (dock / FAB / sheet). Hanya untuk permukaan yang merupakan saudara dari layer `LayerBackdrop`: kaca di dalam subtree yang direkam menyebabkan rekursi `RenderNode` dan `SIGSEGV` native (diperbaiki 2026-11-18, lihat `UPDATE.md`)
- Foto transaksi: kamera via `TakePicture` + `FileProvider`, galeri via Photo Picker — tanpa permission penyimpanan. Berkas mentah dikompres ke WebP 1280px + thumbnail 320px di `filesDir/photos/`
- Gambar target nabung: WebP ≤960px di `filesDir/goals/`, di-decode di `Dispatchers.IO` dengan cache kecil (bukan di thread komposisi). Ikut tersimpan di DataStore dan arsip backup
- Backup: satu arsip ZIP (`saku.json` + `photos/` + `goals/`) lewat Storage Access Framework, tanpa permission
- Versi aplikasi: `versionName` dari Gradle dibaca lewat `PackageManager` dan ditampilkan di Setting (V1.6.8 = `1.6.8`, `versionCode = 7`)

## Fitur

- Home: pilihan **Beranda Baru** (kartu saldo 3D tilt-parallax bernuansa glass-inspired, **ikon mata** sembunyikan saldo, ringkasan **Sumber Dana** Tunai/E-Wallet/custom, kategori cepat, nabung, dan transaksi terbaru) atau **Beranda Klasik** (Buku Tabungan satu lembar); opsi bisa dipilih di Pengaturan › Beranda
- Sumber Dana: pisahkan atau gabungkan **Tunai**, **E-Wallet**, dan sumber dana custom, atau pakai satu saldo tunggal; mendukung **Pindah Saldo** antar sumber dana tanpa mengubah total kekayaan
- Transaksi: header saldo besar yang **mengecil & sticky** saat digulir, filter sumber dana, grafik batang/garis 6–12 bulan (tekan-tahan scrub), statistik turunan, dan tabel 4 kolom dengan **kolom saldo berjalan**
- Catat/ubah transaksi: nominal berformat ribuan, sumber dana / pindah saldo, kategori, Note dengan **ikon kamera/galeri** sampai 6 foto, Date, dan **Time Input** (**dial radial 2 cincin 1–24** untuk format 24 jam + **AM/PM di kepala popover** untuk format 12 jam, segmen `HH:MM`, `±1`, 4 preset, "Sekarang")
- Deteksi Notifikasi E-Wallet & Widget: `NotificationListenerService` opsional untuk mendeteksi notifikasi pembayaran GoPay/OVO/DANA/ShopeePay/m-banking (tawarkan catat cepat, tidak auto-simpan) + **Widget 4×2** di layar utama
- Tabungan (Nabung): kartu target visual modern dengan foto motivasi dominan, overlay gradient, progress bar, tombol cepat **Setor/Tarik**, serta detail + riwayat menabung
- Onboarding & Pengaturan: layar sambutan dengan **halaman khusus toggle fitur awal**, Pengaturan yang ditata ulang (Tampilan, Keuangan, Otomatis & Pengingat, Data & Cadangan), **14 palet + 8 nada kertas + 18 aksen custom**, Backup ZIP, serta **Hapus Data** per bulan atau seluruh aplikasi

## Aturan desain

Sumber kebenaran tampilan ada di [`DESIGN.md`](DESIGN.md); mock acuan di `design/konsep-buku-tabungan-v4.html` (render: `design/konsep-beranda-v4.png`) dan referensi Time Input di `design/time.webp`.

## Build

Butuh JDK 17 dan Android SDK (compileSdk 37).

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`gradle.properties` memuat `android.builtInKotlin=false` dan `android.newDsl=false` agar Kotlin 2.4.10 bisa dipakai bersama AGP 9.

Versi saat ini **V1.6.8** (`versionCode = 7`). **APK tidak disimpan di repository** — file `*.apk` ada di `.gitignore` dan setiap versi diunggah sebagai asset **GitHub Releases** (`v1.3`, `v1.4`, `v1.5`, `v1.6`, …): unduh dari <https://github.com/Davidzinco/saku/releases>. Salinan lokal hasil build tetap ada di mesin untuk keperluan upload. Setting › Data menampilkan versi yang dibaca langsung dari `PackageManager`, jadi tidak bisa lagi tertinggal di V1.0.

## Lisensi

Kode aplikasi ini milik pemilik repo. Library pihak ketiga mengikuti lisensinya masing-masing (`io.github.kyant0:backdrop`, `io.github.kyant0:shapes`). Daftar pekerjaan dan rekomendasi lanjutan ada di [`UPDATE.md`](UPDATE.md).
