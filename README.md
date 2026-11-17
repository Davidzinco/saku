# Saku

Aplikasi keuangan pribadi untuk Android — catat pengeluaran, grafik tren, dan target nabung. Tanpa login, data tersimpan lokal.

## Teknologi

- Kotlin + Jetpack Compose **tanpa Material** (foundation + animation saja), AGP 9.2.1, Kotlin 2.4.10, Compose BOM 2026.09.00
- Tipografi dibundel sebagai resource: Archivo 400/500/600/700 + Newsreader 400 (`res/font/`)
- Ikon digambar sendiri di `Ui.kt` (grid 20, stroke 1.5) — bukan set ikon Material
- Penyimpanan: `SharedPreferences` + JSON (tanpa database, tanpa akun). Aplikasi mulai dari nol, tanpa data contoh
- Liquid glass: `io.github.kyant0:backdrop:2.0.1` + `io.github.kyant0:shapes:1.2.1` ([Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)), dipakai hanya pada dock / wash tab aktif / FAB
- Foto transaksi disimpan di `filesDir/photos/` lewat `FileProvider`; kamera memakai `TakePicture`, galeri memakai Photo Picker — tanpa permission penyimpanan

## Fitur

- Home: saldo akhir, masuk/keluar/selisih, grafik batang/garis 6–12 bulan (tekan-tahan atau geser untuk langsung melihat bulan itu, dengan strip gestur khusus), pengeluaran per kategori, ringkasan nabung, tabel transaksi dengan saldo berjalan dan animasi masuk per baris
- Catat/ubah transaksi: nominal berformat ribuan, kategori, note + **foto** (kamera/galeri), Date, Time (field ringkas + stepper + preset); hapus dengan dua ketuk konfirmasi
- Klik transaksi membuka **sheet detail** dulu (nominal, foto, kategori, catatan, tanggal, waktu, saldo setelah, target nabung), baru bisa masuk ke editor
- Geser halaman untuk pindah tab, atau ketuk/geser dock (halaman berpindah setelah jari dilepas)
- Nabung: setor/tarik nominal bebas, target bisa diubah, angka tersimpan dihitung dari transaksi (saldo sinkron)
- Setting: folder Theme (mode, **Kertas/Kaca**, 6 palet + custom), Category, Chart (batang/garis, 6/12 bulan)
- Tema default monokrom mode gelap; mode **Kaca** menyalakan liquid glass pada dock dan FAB (efek penuh di Android 13+, otomatis datar di bawahnya). Matikan lewat Theme › Dock › Kertas

## Aturan desain

Sumber kebenaran tampilan ada di [`DESIGN.md`](DESIGN.md); mock acuan di `design/konsep-buku-tabungan-v4.html` (render: `design/konsep-beranda-v4.png`).

## Build

Butuh JDK 17 dan Android SDK (compileSdk 37).

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`gradle.properties` memuat `android.builtInKotlin=false` dan `android.newDsl=false` agar Kotlin 2.4.10 bisa dipakai bersama AGP 9.

## Lisensi

Kode aplikasi ini milik pemilik repo. Library pihak ketiga mengikuti lisensinya masing-masing (`io.github.kyant0:backdrop`, `io.github.kyant0:shapes`). Daftar pekerjaan dan rekomendasi lanjutan ada di [`UPDATE.md`](UPDATE.md).
