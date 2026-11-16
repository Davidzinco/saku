# Saku

Aplikasi keuangan pribadi untuk Android — catat pengeluaran, grafik tren, dan target nabung. Tanpa login, data tersimpan lokal.

## Teknologi

- Kotlin + Jetpack Compose **tanpa Material** (foundation + animation saja), AGP 9.2.1, Kotlin 2.4.10, Compose BOM 2026.09.00
- Tipografi dibundel sebagai resource: Archivo 400/500/600/700 + Newsreader 400 (`res/font/`)
- Ikon digambar sendiri di `Ui.kt` (grid 20, stroke 1.5) — bukan set ikon Material
- Penyimpanan: `SharedPreferences` + JSON (tanpa database, tanpa akun). Aplikasi mulai dari nol, tanpa data contoh

## Fitur

- Home: saldo akhir, masuk/keluar/selisih, grafik batang/garis 6–12 bulan (bisa diketuk dan digeser), pengeluaran per kategori, ringkasan nabung, tabel transaksi dengan saldo berjalan
- Catat/ubah transaksi: nominal berformat ribuan, Date, Time (picker geser), kategori, note; hapus dengan dua ketuk konfirmasi
- Geser halaman untuk pindah tab, atau ketuk/geser dock
- Nabung: setor/tarik nominal bebas, target bisa diubah, angka tersimpan dihitung dari transaksi (saldo sinkron)
- Setting: folder Theme (mode + 6 palet + custom), Category, Chart (batang/garis, 6/12 bulan)
- Tema default monokrom mode gelap

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
