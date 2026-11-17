V1.x (upcoming)
integrasi scan dan ai serta sync dengan aplikasi lain. persiapan penambahan menu backup to google drvie 

V1.4 (rekomendasi, belum dikerjakan)
1. Kompres foto saat disimpan. `Photo.kt` saat ini menyalin berkas kamera/galeri apa adanya dan `BitmapFactory.decodeFile` memuat bitmap penuh; foto 12MP bisa bikin sheet detail berat. Simpan WebP ~1280px + thumbnail terpisah.
2. Uji fallback kaca di perangkat Android 8.0–11 (minSdk 26). `blur()` butuh API 31 dan `lens()` butuh API 33; library sudah punya jalur datar, tapi belum diverifikasi di perangkat nyata.
3. Tambah instrumentation test untuk tiga regresi yang paling mudah kambuh: scrub chart vs geser pager, klik tab cepat berulang, dan dock tahan-belum-lepas.
4. Siapkan backup/restore sebagai prasyarat V1.x: ekspor `db` JSON + folder `filesDir/photos/` jadi satu arsip, plus impor dengan validasi. Tanpa ini foto hilang saat pindah HP.
5. Pindahkan `SharedPreferences` + satu string JSON ke DataStore/Room bila transaksi sudah ratusan; `Store.save()` sekarang menulis seluruh database setiap kali ada perubahan.
6. Sheet detail: tambah navigasi transaksi sebelumnya/berikutnya dan konfirmasi hapus yang tidak menutup sheet secara mendadak.
7. Foto: izinkan lebih dari satu lampiran per transaksi, atau setidaknya pratinjau layar penuh dengan pinch-zoom untuk struk.
8. Item 8 pada daftar V1.3 masih kosong — isi bila ada temuan baru dari pemakaian harian.
9. Sinkronkan `DESIGN.md` bila mode Kaca diputuskan jadi default permanen; saat ini mode Kertas tetap tersedia lewat Setting > Theme > Dock.
10. Rapikan tooling build: `android.builtInKotlin=false`, `android.newDsl=false`, dan plugin `org.jetbrains.kotlin.android` sudah deprecated di AGP 9 / Kotlin 2.4.10. Build tetap sukses tanpa warning kode Kotlin, tapi migrasi ke DSL baru perlu diuji sebelum upgrade AGP berikutnya.

V1.3 (bug fix)
1. untuk slider pada chart itu bisa di hold dan langsung memunculkan data pada bulan itu juga, dan fix untuk saat di slide ke kanan malah ke menu transaksi jadi berikan space untuk menggeser chart pada bulan 
2. pada bar di bawah bug sudah di benahi tapi untuk sistemnya jangan di rubah yaitu saat hold belum di lepas jangan membuka halaman nya 
3.  kadang ketika terlalu cepat menekan menu seperti home ke transaksi secara berulang mengalami freze atau berhenti di tengah tengah
4. transaksi yang muncul pada menu home masih belum memliki animasi
5. untuk menu transaksi pada bagian jam nya ganti desain nya karena sangat tidak efisien.
6. pada saat transaksi di klik bukan langsung memunculkan menu edit melainkan menampilkan menu detail
7. tambah kan menu foto pada note di catatan transaksi
8. 


### Status pengerjaan V1.3 (2026-11-17, selesai)
1. **Selesai** — grafik kini bisa ditekan-tahan: bulan di bawah jari langsung dipilih sejak `ACTION_DOWN`, data bulan itu langsung tampil tanpa harus menggeser dulu. Ditambah strip geser khusus 26dp di bawah sumbu bulan. Geser horizontal tidak lagi pindah ke menu Transaksi: down dikonsumsi oleh handler grafik dan `HorizontalPager.userScrollEnabled` dimatikan selama scrub. Handler tidak lagi restart saat cursor berubah (dulu `pointerInput(cursor)` memutus drag).
2. **Selesai** — sistem dock tidak diubah: wash tetap mengikuti jari dengan spring yang sama, tetapi `onTab` baru dipanggil setelah jari dilepas.
3. **Selesai** — semua perpindahan tab lewat `selectTab()` dengan satu `Job` yang dibatalkan sebelum animasi baru, jadi `animateScrollToPage` tidak lagi bertumpuk.
4. **Selesai** — baris transaksi di Home (dan Transaksi) masuk dengan fade + naik 16px, stagger 26ms per baris; tetap menghormati `motionScale()`.
5. **Selesai** — dua track `Scrubber` diganti `TimeField`: satu field 48dp yang dibuka saat diperlukan, stepper jam/menit, dan 4 preset pagi/siang/sore/malam. Tidak ada keyboard, tidak ada inset IME.
6. **Selesai** — klik baris membuka `TxDetailSheet` (jenis, nominal, foto, kategori, catatan, tanggal panjang, waktu, saldo setelah, target nabung). Tombol "Ubah transaksi" membuka editor; hapus tetap dua ketuk.
7. **Selesai** — field Foto di sheet catat: kamera via `TakePicture` + `FileProvider`, galeri via Photo Picker, tanpa permission penyimpanan. Berkas disalin ke `filesDir/photos/` dan dihapus saat transaksi dihapus/foto diganti. Pratinjau muncul di sheet detail.
8. *(kosong di daftar asli — belum ada perubahan)*

### Tambahan di luar daftar V1.3
- Liquid glass dari `Kyant0/AndroidLiquidGlass` (`io.github.kyant0:backdrop:2.0.1` + `shapes:1.2.1`) dipasang pada dock, wash tab aktif, dan FAB: `vibrancy() + blur() + lens()` dengan highlight, shadow, dan inner shadow. Seluruh layar direkam lewat satu `LayerBackdrop` dengan latar opaque.
- Setting > Theme > Dock mendapat pilihan **Kertas / Kaca** (`Saku.glass`, tersimpan di JSON) supaya arah *Buku Tabungan* di `DESIGN.md` tetap bisa dipakai penuh.
- `DESIGN.md` dan `README.md` disinkronkan dengan mode opsional ini.

V1.2 (bug fix and add new feature)
1. Bug nomer 8. (ada bug slide bar di bawah yaitu saat berada di menu atur tidak bisa di geser ke home ) masih ada lebih tepat nya adalah saat di geser kembali ke beranda tidak mau
2. mengubah, menambah transaksi tidak memiliki animasi
3. terlalu mudah keluar aplikasi seperti saat di menu mencatat aplikasi dan menekan back dia langsung keluar aplikasi
4. pada saat memasukan jam ada bug di mana layar bawah ter angkat menjadi space kosong, dan saranku jam nya di ganti degenga desain yang cukup untik seperti slider atau yang lain.
5. bahasanya jangan terlalu baku gabungan ingris yang umum tidak apa apa seperti home, setting, date.
6. tambah icon aplikasi
7. tambah animasi lebih keren lagi agar lebih menarik seperti contoh saat berganti bulan terdapat animasi munculnya transaksi, dan seperti saldo masuk, keluar, selisih memiliki animasi
8. mungkin default color nya adalah monokrom dengan mode gelap.
9. bisa di slide tanpa harus memencet menu yang berada di bawah.
10. untuk grafik garis tidak bisa di tekan.
11. untuk grafik pengeluaran bar dan garis bisa di slide 
12. warna transaksi keluar atau mines adalah merah.


V1.1 (Add some feature)
1. saat input nominal setiap 3 angka di belakang berikan , 
2. pada saat memasukan tanggal berikan berikan juga jam nya
3. tidak adanya edit/hapus transaksi
4. pada bulan yang di tampilkan di home bulan yang di tampilkan selalu 6 atau 12 bulan sekaligus (berikan pilihanya di setingan) aadan bar yang ter select bukan hanya di pojok kanan melainkan mengikuti di mana arah yang di tuju dan bulannya tidak bergeser (serperti sekarang)
5. pada menu tabung terlalu minimalis dan terlalu minim fitur seperti:
    1. saat sudah meng-input uang tidak bisa di edit
    2. hanya bisa memberikan input 100rb
    3. saldonya harus sync dnegnan saldo ahir
    4. improve lah ui nya
6. di atur tema mungkin lebih baik di kasih beberapa opsi yaitu 2 defauld terang dan gelap dan ada lagi template color pallet yang populer dan custom sendiri
7. di atur jangan langsung tunjukan semua nya melainkan nanti harus masuk ke folder baru seperti tema di dalamnya berisi tema, pengaturan kategori di dalam nya berisi pengaturan kategori
8. ada bug slide bar di bawah yaitu saat berada di menu atur tidak bisa di geser ke home 
9. berikan pilihan pada data bar di home menjadi ada grafik naik turun 
10. tambahkan animasi lebih agar terlihat lebih enak dan fluit