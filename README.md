# Saku

Aplikasi keuangan pribadi untuk Android — catat pengeluaran, grafik tren, dan target nabung. Tanpa login, data tersimpan lokal.

## Teknologi

- Kotlin + Jetpack Compose **tanpa Material** (foundation + animation saja), AGP 9.2.1, Kotlin 2.4.10, Compose BOM 2026.09.00
- Tipografi dibundel sebagai resource: Archivo 400/500/600/700 + Newsreader 400 (`res/font/`)
- Ikon digambar sendiri di `Ui.kt` (grid 20, stroke 1.5) — bukan set ikon Material
- Penyimpanan: `SharedPreferences` + JSON (tanpa database, tanpa akun). Aplikasi mulai dari nol, tanpa data contoh

## Fitur

- Beranda: saldo akhir, masuk/keluar/selisih, grafik 6 bulan, pengeluaran per kategori, ringkasan nabung, tabel transaksi dengan saldo berjalan
- Catat transaksi: jenis keluar/masuk, nominal, kategori, catatan, kalender pemilih tanggal
- Navigasi bulan ‹ › pada Beranda dan Transaksi
- Nabung: target dengan progress, tambah cepat Rp 100rb, hapus target
- Atur: tema terang/gelap, kelola kategori (tambah, urutkan, hapus), ringkasan data
- Dock datar mengambang: ketuk atau geser untuk pindah tab

## Aturan desain

Sumber kebenaran tampilan ada di [`DESIGN.md`](DESIGN.md); mock acuan di `design/konsep-buku-tabungan-v3.html`.

## Build

Butuh JDK 17 dan Android SDK (compileSdk 37).

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`gradle.properties` memuat `android.builtInKotlin=false` dan `android.newDsl=false` agar Kotlin 2.4.10 bisa dipakai bersama AGP 9.

## Lisensi

Kode aplikasi ini milik pemilik repo. Library pihak ketiga mengikuti lisensinya masing-masing (`com.kyant0:backdrop`).
