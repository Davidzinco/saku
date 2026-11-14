# Saku

Aplikasi keuangan pribadi untuk Android — catat pengeluaran, grafik tren, dan target nabung. Tanpa login, data tersimpan lokal.

## Teknologi

- Kotlin + Jetpack Compose (Material 3), AGP 9.2.1, Kotlin 2.4.10, Compose BOM 2026.09.00
- Liquid glass dock memakai `io.github.kyant0:backdrop:2.0.1` — blur + refraksi tepi (`lens`) + chromatic aberration lewat shader AGSL
- Penyimpanan: `SharedPreferences` + JSON (tanpa database, tanpa akun)

## Fitur

- Beranda: saldo, ringkasan bulan, grafik tren 6 bulan (bisa diketuk), daftar transaksi
- Catat transaksi: jenis keluar/masuk, nominal, kategori, catatan, kalender pemilih tanggal sendiri
- Navigasi bulan ‹ › pada Beranda dan Transaksi
- Nabung: target dengan progress, tambah cepat Rp 100rb
- Pengaturan: tema gelap/terang, kelola kategori (tambah, urutkan, hapus)
- Dock kaca mengambang: ketuk atau geser (hold & swap) untuk pindah tab

## Build

Butuh JDK 17 dan Android SDK (compileSdk 37).

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`gradle.properties` memuat `android.builtInKotlin=false` dan `android.newDsl=false` agar Kotlin 2.4.10 bisa dipakai bersama AGP 9.

## Lisensi

Kode aplikasi ini milik pemilik repo. Library pihak ketiga mengikuti lisensinya masing-masing (`com.kyant0:backdrop`).
