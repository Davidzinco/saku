# Saku

Aplikasi keuangan pribadi untuk Android — catat pengeluaran, grafik tren, dan target nabung. Tanpa login, data tersimpan lokal.

## Teknologi

- Kotlin + Jetpack Compose **tanpa Material** (foundation + animation saja), AGP 9.2.1, Kotlin 2.4.10, Compose BOM 2026.09.00
- Tipografi dibundel sebagai resource: Archivo 400/500/600/700 + Newsreader 400 (`res/font/`)
- Ikon digambar sendiri di `Ui.kt` (grid 20, stroke 1.5) — bukan set ikon Material
- Penyimpanan: **DataStore Preferences** (`androidx.datastore:datastore-preferences:1.1.7`) dengan satu key per transaksi, tanpa akun dan tanpa server. `save()` hanya menulis record yang berubah; data lama dari `SharedPreferences` dimigrasikan otomatis sekali. Room sengaja tidak dipakai agar tidak perlu KSP/codegen pada AGP 9.2.1 + Kotlin 2.4.10. Aplikasi mulai dari nol, tanpa data contoh
- Liquid glass: `io.github.kyant0:backdrop:2.0.1` + `io.github.kyant0:shapes:1.2.1` ([Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)), bisa dipilih per **area overlay** (dock / FAB / sheet). Hanya untuk permukaan yang merupakan saudara dari layer `LayerBackdrop`: kaca di dalam subtree yang direkam menyebabkan rekursi `RenderNode` dan `SIGSEGV` native (diperbaiki 2026-11-18, lihat `UPDATE.md`)
- Foto transaksi: kamera via `TakePicture` + `FileProvider`, galeri via Photo Picker — tanpa permission penyimpanan. Berkas mentah dikompres ke WebP 1280px + thumbnail 320px di `filesDir/photos/`
- Backup: satu arsip ZIP (`saku.json` + foto) lewat Storage Access Framework, tanpa permission

## Fitur

- Home: saldo, masuk/keluar/selisih, grafik batang/garis 6–12 bulan (tekan-tahan langsung memilih bulan, data dihitung sekali per jendela), pengeluaran per kategori, ringkasan nabung, tabel transaksi dengan saldo berjalan dan animasi masuk per baris
- Catat/ubah transaksi: nominal berformat ribuan, kategori, Note dengan **ikon kamera/galeri** untuk sampai 6 lampiran foto, Date, dan **Time Input** (field berikon jam + popover jam/menit/AM-PM ber-interval, format 12/24 jam, preset); hapus dengan dua ketuk konfirmasi
- Klik transaksi membuka **sheet detail** dulu (nominal, foto, kategori, catatan, tanggal, waktu, saldo setelah, target nabung) dengan navigasi sebelumnya/berikutnya, pratinjau foto pinch-zoom, dan hapus yang tidak menutup sheet; dari sini baru masuk editor
- Geser halaman untuk pindah tab, atau ketuk/geser dock kapsul yang membesar saat ditahan (halaman berpindah setelah jari dilepas)
- Nabung: setor/tarik nominal bebas, target bisa diubah, angka tersimpan dihitung dari transaksi (saldo sinkron). Di Home bisa disembunyikan, digulir bersama target lain, atau di-pin ke satu target
- Setting: Theme (mode, **Kertas/Kaca**, **UI per area**, 6 palet + custom), Category, Chart, Home, dan **Backup** (ekspor/pulihkan ZIP dengan validasi)
- Tema default monokrom mode gelap. **Kaca** menyalakan liquid glass — efek penuh di Android 13+, otomatis datar di bawahnya — dan bisa dipilih per bagian lewat Theme › UI (Dock, Tombol catat, Sheet); matikan semua lewat Theme › Mode dasar › Kertas. Komponen di dalam halaman (termasuk Ringkasan Home) selalu kertas

## Aturan desain

Sumber kebenaran tampilan ada di [`DESIGN.md`](DESIGN.md); mock acuan di `design/konsep-buku-tabungan-v4.html` (render: `design/konsep-beranda-v4.png`) dan referensi Time Input di `design/time.webp`.

## Build

Butuh JDK 17 dan Android SDK (compileSdk 37).

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`gradle.properties` memuat `android.builtInKotlin=false` dan `android.newDsl=false` agar Kotlin 2.4.10 bisa dipakai bersama AGP 9.

## Lisensi

Kode aplikasi ini milik pemilik repo. Library pihak ketiga mengikuti lisensinya masing-masing (`io.github.kyant0:backdrop`, `io.github.kyant0:shapes`). Daftar pekerjaan dan rekomendasi lanjutan ada di [`UPDATE.md`](UPDATE.md).
