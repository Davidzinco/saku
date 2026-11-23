V1.x (upcoming)
integrasi scan dan ai serta sync dengan aplikasi lain. persiapan penambahan menu backup to google drvie

s
V1.6.3
1. beri saldo nya menjadi 2 bagian atau bisa di tambah manual di seting yaitu saldo cash atau e-wallet 
2. coba tampilan pada home di rubah karena terlalu terlihat seperti terlalu minimalis dan mungkin tuntuk tampilan home sekarang beberapa bisa di pindah ke transaksi daripada di home. singkatnya kita coba rombak tampilan home karena terlalu minimalis tapi berikan opsi nya pada seting untuk menggunakan tampilan sekarang atau yang baru.
3. berikan cara agar orang tidak lupa mencatat setiap pengeluaran. saya ada beberapa saran seperti notifikasi jika terdeteksi notif pembayaran dari aplikasi e-wallet dan itu bisa di nyalakan atau di matikan agar pengguna tidak merasa tidak nyaman dan ada saran lagi seperti tambah widget dengan sangat simple dan informatif serta mudah dalam mengisi data
4. Optimasi aplikasi karena saya merasa aplikasi sudah mulai berat dari terlihatnya patah patah saat berganti menu atau sekedar scroll history transaksi
5. berikan opsi hapus data total atau di bulan tertentu saja.
6. 

V1.6 (small update)
1. bootom menu nya belum sesuai, saran saya ambil data dari applikasi "C:\Users\Davidzinco\WorkBuddy AI\2026-11-17-12-42-19\LiquidGlassApp"
2. redesain jam nya menjadi lebih minimalis simple dan enak di tangan, gunakan semua teori yang ada untuk membuat menu jam yang lebih nyaman

> **Kebijakan distribusi APK (2026-11-23):** file `*.apk` **tidak lagi disimpan
> di repository**. Semua build yang sebelumnya ter-commit
> (`saku-1.3/1.4/1.5-debug.apk`) dikeluarkan dari tracking dan dipindahkan ke
> asset **GitHub Releases** — `v1.3`, `v1.4`, `v1.5`, `v1.6`. `.gitignore`
> kini mengabaikan semua `*.apk` tanpa pengecualian. Salinan lokal tetap ada di
> root repo / root workspace hanya untuk keperluan build & upload.

### Status pengerjaan V1.6 (2026-11-21, selesai)

Dua area yang sama dengan V1.5 #1/#2 dikerjakan ulang karena hasil sebelumnya
**belum sesuai**. Kali ini dock mengambil **data langsung dari proyek acuan**,
dan Time Input didesain ulang dari nol memakai prinsip ergonomi, bukan sekadar
menata ulang chip. Aturan kaca overlay V1.4, grid 4 kolom
Tgl/Keterangan/Jumlah/Saldo, serta kontrak geser/tahan V1.3 tidak disentuh.

1. **Selesai** — dock mengikuti `LiquidGlassApp/LiquidBottomTabs.kt`:
   - Geometri: kapsul **64dp** (sebelumnya 66dp), inset 4dp, baris isi 56dp,
     ikon **22dp**, label **11sp**, jarak ikon–label 2dp.
   - Pill indikator naik dari kapsul kecil 44dp / `slot−16dp` menjadi **56dp
     selebar slot** (`slot−8dp`, inset 4dp), sehingga benar-benar
     "menggelinding" mengikuti tab aktif seperti komponen acuan.
   - Pembesaran bukan lagi milik satu tab (V1.5: 1.14× pada tab aktif saja),
     melainkan **satu `pressProgress` bersama** (`spring 1.0 / 300`) yang
     dipakai kontainer `1 + 16dp/lebar`, **semua** isi tab `1.0 → 1.2`
     (tab aktif tambah 1.06 agar permintaan V1.5 #1 tetap terasa), dan pill
     sampai faktor `64/56`. Pill juga menerima squash/stretch dari kecepatan
     geser (EMA, dijepit ±0.18) seperti `layerBlock` di acuan.
   - Skala X pill dijepit ke rasio `slot/(slot−8dp)` supaya tidak menimpa tab
     tetangga pada layar lebar. Jumlah state animasi turun **5 → 1**; semua
     state tetap dibaca di lambda `graphicsLayer`, tanpa recomposition per frame.
   - Sistem geser/tahan tidak diubah: halaman tetap baru dibuka **setelah jari
     dilepas** (V1.3 #2), wash tetap snap ke slot terdekat.
2. **Selesai** — Time Input didesain ulang total; grid 36 chip V1.5 dihapus:
   - Field 52dp: ikon jam, `HH:mm` 22sp, chevron, chip `24j/12j` terpisah
     (tidak ada clickable bersarang). "Sekarang" ada di kepala popover.
   - **Dial radial 12 posisi, dua langkah (jam → menit)**: jam memakai model
     jam analog (`12,1,…11` pada 12j / `00,02,…22` pada 24j), menit
     `00,05,…55`; lingkaran + jarum digambar satu `Canvas`, 12 sel memakai
     `tap` biasa. Ketuk jam otomatis lanjut ke menit, ketuk menit menutup
     popover — **dua ketuk** untuk waktu penuh. Badge AM/PM di tengah dial
     saat format 12j.
   - **Teori yang diterapkan:** Hick (12 pilihan per langkah, bukan 36),
     Fitts (target radial ~44dp, jarak antar pusat `2·r·sin15° ≥ lebar sel`,
     semuanya dekat ibu jari), recognition-over-recall (model jam analog),
     progressive disclosure, dan koreksi `−/+ 1` pada segmen aktif supaya
     jam ganjil / menit tepat tetap bisa dipilih. Empat preset
     pagi/siang/sore/malam menjadi jalur satu ketuk.
   - Tetap tanpa keyboard/IME, tanpa `LazyColumn`/scroll bersarang, tanpa
     auto-scroll; tinggi popover konstan saat segmen berganti.
3. **Kosong** pada daftar asli — tidak ada pekerjaan tambahan yang diminta.
   Versi aplikasi tetap mengikuti: `versionCode = 6`, `versionName = "1.6"`.

**Verifikasi build (2026-11-21):**

- `:app:compileDebugKotlin`, `:app:assembleDebug`, dan `:app:lintDebug`
  (**termasuk satu kali `--rerun-tasks`**) **BUILD SUCCESSFUL**; **0 error**,
  13 warning + 11 hint, semuanya sudah ada sejak V1.4 (tidak ada warning baru
  dari `App.kt` maupun `Sheet.kt`). Dua state baru sengaja memakai
  `mutableFloatStateOf` / `mutableIntStateOf` supaya tidak ada autoboxing di
  jalur geser dock.
- `apksigner verify` → `Verifies` (v2); `aapt dump badging` → minSdk 26,
  targetSdk 37, `versionCode='6'`, `versionName='1.6'`. `classes3.dex`
  membesar 1.149.352 → 1.169.828 byte, jadi kode baru benar-benar terpaket.
- APK `saku-1.6-debug.apk` (11.189.447 byte), salinan di root workspace
  `Saku-V1.6-LiquidGlass.apk`, SHA256
  `d1b55a69028c45191d083ab179bffe51164ff116cc40eeeaf0d9052f6f2f288e`.
  Sejak 2026-11-23 berkas ini **hanya diunggah sebagai asset Release `v1.6`**,
  bukan disimpan di repo (lihat kebijakan distribusi di atas).
- **Uji di perangkat belum dilakukan** (`adb devices` kosong). Karena kedua
  area ini soal rasa/ergonomi, smoke test sangat disarankan: tahan & geser
  dock (halaman tidak boleh berpindah sebelum jari lepas, pill tidak menimpa
  tetangga), buka Time Input, pilih jam lalu menit, coba `±1`, AM/PM, preset,
  dan format 12j/24j.
- Perubahan V1.6 **sudah di-commit dan di-push** ke `origin/master` pada
  2026-11-23 bersamaan dengan pengeluaran semua APK dari repository. Tag dan
  Release **`v1.6`** dibuat dengan `Saku-V1.6-LiquidGlass.apk` sebagai asset
  unduhan (Release "Latest"). Repo tinggal menyimpan source + dokumen.

V1.5 (UI enhancement and polish)
1. Buat bottom menu yang terselect membesar bukan hanya menunya saja sama kan dengan project "C:\Users\Davidzinco\WorkBuddy AI\2026-11-17-12-42-19\LiquidGlassApp"
2. buatkan jam nya lebih minimalis dan lebih nyaman serta stylis
3. change the icon for the app
4. perdetail informasi yang di tunjukan pada menu transaksi
5. pada opsi nabung bisa di berikan upload image agar membuat semgant dalam menabung, dan buat agar bsia di tap dan menampilkan tabugnan secara detail serta history kapan kita menabung
6. pada custom thame perbanyak dan permudah pemiliha theme.
7. silakan tambah hal yang sekiranya dapat membuat aplikasi nya lebih bagus dan fluit agar enak di liat dan buat tetap
8. mulai update informasi versi aplikasi karena setiap saya cek selalu menunjukna  V1.0

### Status pengerjaan V1.5 (2026-11-19, selesai)

> **Catatan 2026-11-21:** poin **1** dan **2** di bawah berstatus *selesai
> secara build*, tetapi **belum memenuhi ekspektasi** — keduanya sudah
> dikerjakan ulang di **V1.6** (lihat status V1.6 di atas). Jangan jadikan
> blok ini sebagai acuan tampilan dock / Time Input saat ini.

Implementasi kedua setelah V1.5 sebelumnya direvert. Kali ini perubahan
**disesuaikan dengan grammar aplikasi** dan dijaga agar tidak menurunkan performa:
kolom saldo berjalan (ciri khas *Buku Tabungan*) dipertahankan, tidak ada decode
bitmap di thread komposisi, tidak ada `LazyColumn` bersarang di dalam sheet yang
sudah menggulir, dan aturan kaca overlay dari patch crash V1.4 tidak dilanggar.

1. **Selesai** — tab dock yang terpilih kini membesar **seluruhnya** (ikon 20→21dp,
   label ikut, skala 1.14× dengan spring `0.72 / 420`) seperti perilaku indikator
   di `LiquidGlassApp`; kapsul wash ikut 1.035×. Transform murni lewat
   `graphicsLayer` dengan state dibaca di lambda, jadi per frame cuma invalidate
   fase gambar — tidak ada recomposition Dock. Sistem geser/tahan V1.3–V1.4 tidak
   diubah sama sekali.
2. **Selesai** — Time Input lebih minimalis: satu field 52dp yang seluruhnya bisa
   diketuk, angka 20sp, badge AM/PM, chevron; tombol clear berubah jadi "Sekarang"
   di kepala popover. Popover tiga kolom yang harus digulir diganti **grid chip
   statis** (jam 8 kolom, menit 6 kolom, AM/PM, 4 preset) — satu ketuk langsung
   memilih, tanpa scroll bersarang dan tanpa auto-scroll `animateScrollToItem`
   yang dulu bisa menarik posisi daftar. Tetap tanpa keyboard / inset IME.
3. **Selesai** — ikon aplikasi diganti: sampul buku tabungan dominan aksen terang
   (`#8FA5F2`) di atas latar gelap, tiga baris catatan, dan cap "catat" berplus.
   Masih adaptif + monochrome, tetapi jelas berbeda dari ikon kertas pucat lama.
4. **Selesai** — informasi transaksi diperkaya **tanpa mengubah grid 4 kolom**:
   baris tanggal kini menampilkan hari, meta memuat glyph kategori, penanda foto
   (`Ic.Image`) dan penanda target nabung (`Ic.Pin`), dan deskripsi TalkBack ikut
   menyebut jumlah lampiran/target. Halaman Transaksi mendapat tiga angka
   turunan (rata-rata keluar, hari tercatat, jumlah catatan) yang dihitung sekali
   per bulan. Sheet detail tambah baris Hari, Lampiran, Jenis, dan Target nabung.
5. **Selesai** — target nabung punya gambar motivasi:
   - `Goal.image` (default `""`, data V1.4 tetap terbaca) tersimpan di
     `filesDir/goals/` sebagai WebP ≤960px dengan koreksi EXIF; DataStore dan
     backup ZIP (`goals/`) ikut membawanya, lengkap dengan path relatif + validasi
     nama saat impor.
   - Gambar bisa dipilih **saat membuat target** maupun lewat **Ubah target**.
     Berkas kamera/galeri masih mentah di `cacheDir` sampai tombol Simpan ditekan:
     membatalkan sheet tidak menyisakan berkas yatim dan tidak pernah menghapus
     gambar yang masih dipakai data tersimpan.
   - Kepala target di halaman Nabung bisa diketuk → **sheet detail** berisi gambar
     lebar, tersimpan, sisa target, progress, tombol Setor/Tarik, dan **riwayat
     menabung** (tanggal + jam setiap setoran/penarikan, 8 terbaru + "Lihat semua").
     Gambar juga muncul di ringkasan/chip Nabung pada Home.
   - Decode gambar dilakukan di `Dispatchers.IO` lewat `LaunchedEffect` + cache
     `LruCache` kecil, bukan di komposisi; riwayat memakai `Column` biasa (bukan
     `LazyColumn`) karena `SheetScaffold` sudah menggulir vertikal.
   - `savedMap()` menghitung saldo semua target dalam **satu lintasan** transaksi,
     menggantikan `savedOf()` berulang per target di Home/Nabung.
6. **Selesai** — Custom theme diperluas & dipermudah: 6 nada kertas (+Mawar,
   +Laut), 9 palet (+Mawar, +Laut, +Zaitun), 12 swatch aksen. Setting › Theme
   kini punya **pratinjau langsung** kombinasi custom, grid aksen 6 kolom, dan
   deretan chip nada kertas yang bisa digeser; memilih salah satu langsung
   memakai palet custom.
7. **Selesai** — polish: `pressable(scale, alpha)` dipakai pada tombol utama/
   sekunder, baris menu Setting, segmen, field Time, dan kartu target; progress
   `Track` memakai spring dengan **nilai dijepit 0..1** (spring tanpa clamp bisa
   overshoot dan membuat `fillMaxWidth(fraction)` melempar exception). Grid chip
   waktu sengaja tetap memakai `tap` biasa supaya 36 chip tidak membuat 72 state
   animasi. Umpan balik tekan tidak dipasang pada baris `LazyColumn` dan tidak
   menyentuh FAB kaca.
8. **Selesai** — `versionCode = 5`, `versionName = "1.5"`; Setting › Data kini
   menampilkan **Versi** yang dibaca dari `PackageManager` (bukan hardcode), dan
   footer Setting memakai versi yang sama. `aapt dump badging` membuktikan
   `versionCode='5' versionName='1.5'`.

**Yang sengaja TIDAK diulang dari V1.5 lama (2026-11-18, direvert):**

- `TxRow` tidak diubah jadi dua kolom — kolom **Saldo** dan header Tgl/Jumlah/
  Saldo tetap ada, karena itu *signature move* di `DESIGN.md`.
- Tidak ada `loadGoalImage()` yang men-decode bitmap langsung di dalam komposisi.
- Tidak ada spring tanpa clamp pada `fillMaxWidth`, tidak ada wheel `LazyColumn`
  dengan `animateScrollToItem` per perubahan pilihan.
- Gambar lama tidak dihapus saat pengguna memilih gambar baru tetapi belum simpan.

**Verifikasi build (2026-11-19):**

- `:app:compileDebugKotlin` dan `:app:assembleDebug` **BUILD SUCCESSFUL**; tidak
  ada warning kode Kotlin baru (hanya warning deprecated plugin Gradle yang sudah
  ada sejak V1.2). `compileDebugKotlin` dijalankan ulang saat verifikasi lint dan
  tetap sukses.
- `:app:lintDebug --rerun-tasks` **BUILD SUCCESSFUL**, **0 error**, 13 warning,
  12 hint. Semua warning sudah ada di V1.4 (6× `android.media.ExifInterface`,
  `Bitmap.createScaledBitmap`, posisi parameter `modifier` di `Ui.kt`, versi
  Gradle/dependensi, `mipmap-anydpi-v26`); tidak ada warning baru dari kode V1.5.
  Satu-satunya error lint sebelumnya berasal dari `local.properties` lokal
  (`sdk.dir=C:////Android////sdk` → `PropertyEscape`), bukan dari source: berkas
  itu ada di `.gitignore` dan sudah diperbaiki menjadi
  `sdk.dir=C\:\\Android\\Sdk`. Laporan lama sempat ter-cache, sehingga perlu
  `--rerun-tasks` untuk memaksa analisis ulang.
- `apksigner verify` → `Verifies` (v2); `aapt dump badging` → minSdk 26,
  targetSdk 37, `versionCode='5'`, `versionName='1.5'`, ikon adaptif terbaca.
- APK `saku-1.5-debug.apk` (10.679.533 byte), salinan di root workspace
  `Saku-V1.5-LiquidGlass.apk`, SHA256
  `2b0258857c48d0764d32e43fbf1c8fb3a7223c532e54012c01cdc95f3b730591`.
- **Uji di perangkat belum dilakukan** karena saat pengerjaan tidak ada perangkat
  `adb` yang terhubung (`adb devices` kosong). Sebelum dipasang di HP utama,
  disarankan smoke test: buka Setting › Theme (semua area Kaca), buat target
  nabung + gambar, buka detail/riwayat, simpan transaksi dengan Time Input baru,
  lalu ekspor/impor backup. Aturan kaca overlay tidak disentuh oleh perubahan ini.
- Perubahan sudah **di-commit** sebagai `ced6e85` (+ `96e61ba` untuk koreksi
  status dokumen ini) dan **di-push** ke `origin/master`
  (`github.com/Davidzinco/saku`) pada 2026-11-21; remote kini di **`96e61ba`**
  (sebelumnya `a0113ee` / V1.4). Rilis **`v1.5`** kemudian dibuat pada
  2026-11-23 (asset `Saku-V1.5-LiquidGlass.apk`) saat semua APK dipindahkan
  keluar dari repository ke GitHub Releases; rilis terbaru saat ini adalah
  **`v1.6`**.

V1.4 (bug fix and add new feature)
1. untuk slide chart itu kadang transaksi tidak terload, ngelag atau tidak optimal dan hilangkan slider pada bawah chart.
2. jam pada menu transaksi pasih belum effisien buat seperti desain in "C:\Users\Davidzinco\WorkBuddy AI\2026-11-17-12-42-19\saku-compose\design\time.webp"  
3. Siapkan backup/restore sebagai prasyarat V1.x: ekspor db JSON + folder filesDir/photos/ jadi satu arsip, plus impor dengan validasi. Tanpa ini foto hilang saat pindah HP.
4. samakan bottom menu nya dengan aplikasi yang sebelumnya yaitu lebih rounded tidak kotak dan saat di hold membesar 
5. Pindahkan SharedPreferences + satu string JSON ke DataStore/Room bila transaksi sudah ratusan; Store.save() sekarang menulis seluruh database setiap kali ada perubahan.
6. Sheet detail: tambah navigasi transaksi sebelumnya/berikutnya dan konfirmasi hapus yang tidak menutup sheet secara mendadak.
7. Foto: izinkan lebih dari satu lampiran per transaksi, atau setidaknya pratinjau layar penuh dengan pinch-zoom untuk struk. dan juga ganti ui untuk menangkap foto atau memilih foto menjadi icon yang berada di note bukan sebagai button di bawah note.
8. rencanakan perubahan desain total untuk merubah beberapa menu yang munggukin bisa menggunakan tema liquid glass. namun tetap memeberikan pilihan opsi pata Setting > Theme > UI di situ tersedia pilihan mana saja yang mau atau akan di ubah menjadi liquid atau paper design.
9. hilangkan ini daro beberapa ui ini seperti
    1. "Opsional. Lampirkan struk atau foto ke catatan."
    2. "Saldo ahir"
    3. "Lembar [date]"
    4. "Tertinggi Oktober, [pengeluaran_tertinggi]"
    5. "tahan & geser"
10. Pada nabung yang di tampilkan di home memiliki opsi tersendiri mau di tampilkan atau tidak pada menu setting, dan bergulir dengan nabung yang lain atau tidak, dan bisa di pilih yang akan di pin di home nabung yang mana
11. Kompres foto saat disimpan. Photo.kt saat ini menyalin berkas kamera/galeri apa adanya dan BitmapFactory.decodeFile memuat bitmap penuh; foto 12MP bisa bikin sheet detail berat. Simpan WebP ~1280px + thumbnail terpisah.
12. Update file DESIGN.md

### Status pengerjaan V1.4 (2026-11-18, selesai)

1. **Selesai** — scrub grafik tidak lagi membuat daftar transaksi kosong/ngelag. `AnimatedContent` yang berganti kunci tiap bulan di Home dan Transaksi dihapus; `LazyColumn` kini dibuat sekali, animasi masuk berjalan per baris lewat `rowEnter(i, monthKey)`. Data grafik dihitung sekali per jendela lewat `trendFast()` (satu lintasan transaksi, bukan 6–12 kali filter) dan di-`remember` per tahun/paruh tahun. Slider 26dp di bawah chart **dihapus** — grafiknya sendiri yang jadi ruang gestur, dengan down tetap dikonsumsi dan pager tetap dimatikan saat scrub.
2. **Selesai** — Time Input baru mengikuti `design/time.webp`: field 48dp dengan contextual prefix (ikon jam), clear-time (×), chevron, popover tiga kolom ber-interval (jam / menit tiap 5 menit / AM-PM), selected segment disorot aksen, toggle 24 jam/12 jam, dan preset pagi/siang/sore/malam. Stepper lama dihapus.
3. **Selesai** — Setting > Backup: ekspor satu ZIP (`saku.json` + `photos/`) lewat `ACTION_CREATE_DOCUMENT`, impor lewat `ACTION_OPEN_DOCUMENT`, tanpa permission. Impor divalidasi (path traversal, jumlah/ukuran entry, `saku.json` wajib, `Saku.isSane()`), path foto dibuat relatif saat ekspor lalu dipetakan ke `filesDir` perangkat tujuan. Foto lama tidak dihapus saat impor.
4. **Selesai** — dock dan wash jadi kapsul penuh (`RoundedCornerShape(50)`), bukan sudut 22dp; selama jari menahan dock membesar ke 1.045× dengan spring lalu kembali saat lepas. Sistem geser V1.3 tidak diubah.
5. **Selesai** — `SharedPreferences` + satu string JSON diganti **DataStore Preferences** dengan satu key per transaksi. `Store.save()` sekarang hanya menulis record yang berubah (diff terhadap snapshot), bukan seluruh database. Data lama dimigrasikan otomatis sekali saat `load()` pertama. Room tidak dipilih karena butuh KSP/codegen yang berisiko pada AGP 9.2.1 + Kotlin 2.4.10; DataStore memberi manfaat yang sama tanpa itu. `README.md` mencatat keputusan ini.
6. **Selesai** — sheet detail punya navigasi transaksi sebelumnya/berikutnya (`n / total` + chevron). Menghapus tidak lagi menutup sheet mendadak: sheet tetap terbuka, pindah ke transaksi tetangga bila ada, dan menampilkan "Transaksi dihapus" + tombol Tutup bila tidak ada.
7. **Selesai** — satu transaksi bisa punya sampai 6 lampiran (`Tx.photos`, tetap membaca `Tx.photo` lama). Pratinjau layar penuh dengan pinch-zoom/pan dan navigasi antar foto. Aksi kamera/galeri pindah jadi dua ikon di dalam field Note; tombol dan caption di bawah Note dihapus.
8. **Selesai (dikoreksi patch crash)** — Setting > Theme > **UI** berisi **3 area overlay** (Dock, Tombol catat, Sheet) yang masing-masing bisa dipilih **Kertas/Kaca**. Theme > Mode dasar tetap jadi sakelar utama. Implementasinya lewat `Saku.uiGlass` + `LocalGlassUi`. **"Ringkasan Home" dihapus dari pilihan** setelah ditemukan crash native saat Kaca aktif — lihat bagian "Perbaikan crash mode Kaca" di bawah. Desain total (semua permukaan *overlay* jadi kaca sekaligus) tetap tersedia sebagai pilihan, bukan paksaan.
9. **Selesai** — teks yang diminta dihapus: "Opsional. Lampirkan struk atau foto ke catatan.", label "Saldo akhir" di Home, "Lembar [date]", caption "Tertinggi …", dan "tahan & geser". Label berbayang di halaman lain ("Saldo akhir" di Nabung/Setting) disingkat jadi "Saldo" supaya nilainya tetap bermakna.
10. **Selesai** — Setting > **Home**: tampilkan/sembunyikan ringkasan nabung, bergulir bersama target lain atau tidak, dan pin target tertentu (atau otomatis pilih yang paling banyak). Saat "bergulir" aktif, Home memakai `LazyRow` berisi semua target.
11. **Selesai** — foto dikompres saat disimpan: decode dengan `inSampleSize`, koreksi rotasi EXIF, WebP maksimal 1280px (quality 82) + thumbnail 320px terpisah. Thumbnail dipakai di daftar, berkas utama baru didecode (masih tersampel) di pratinjau penuh. `file_paths.xml` ditambah `cache-path` untuk berkas mentah kamera.
12. **Selesai** — `DESIGN.md` dan `README.md` disinkronkan (anggaran tinggi grafik turun ke ~124dp, Time Input, dock kapsul, opsi UI per area, foto, backup, penyimpanan, changelog).

### Perbaikan crash mode Kaca (2026-11-18, patch V1.4)

**Laporan:** «setiap saya buka dan di pilih ke liquid selalu crash dan tidak bisa di buka».

**Hasil `adb`** (perangkat `RRCYA016B5A` / `SM-A566B`, Android 17 / SDK 37):

- `logcat -b crash`: `Fatal signal 11 (SIGSEGV), code 2 (SEGV_ACCERR)` pada **RenderThread** (`tid 6971`, `pid 6831`, `id.saku.app`).
- `Cause: stack pointer is not in a rw map; likely due to stack overflow.`
- Backtrace lebih dari 500 frame berulang: `libhwui.so` → `RenderNode::prepareTreeImpl` ↔ `SkiaDisplayList::prepareListAndChildren`. **Bukan** exception Java/Kotlin, jadi tidak ada stack trace aplikasi.
- DataStore yang tersimpan berisi `{"home":true,"sheet":true}` — area **Ringkasan Home** ikut dinyalakan, dan Home adalah halaman default, makanya app langsung crash saat dibuka.

**Akar masalah (dikonfirmasi dari sumber Backdrop 2.0.1):** `LayerBackdropNode.draw()` merekam subtree `layerBackdrop` ke satu `GraphicsLayer`; `LayerBackdrop.drawBackdrop()` lalu memanggil `drawLayer()` pada `GraphicsLayer` yang sama. V1.4 memasang `glassPanel()` di `Beranda.SaldoBlock`, yaitu **di dalam** subtree yang sedang direkam — display list akhirnya memuat dirinya sendiri dan HWUI menyiapkan `RenderNode` yang sama tanpa henti sampai stack RenderThread habis. Aplikasi liquid glass sebelumnya tidak crash karena `LiquidBottomTabs` dirender sebagai **saudara** dari Box `layerBackdrop`, bukan turunannya.

**Yang diperbaiki:**

1. `Beranda.SaldoBlock` tidak lagi memakai `glassPanel()` — ringkasan kembali ke permukaan datar (secara visual setara, karena di belakangnya hanya ada latar `t.bg` polos).
2. `App.kt` menambah `CompositionLocalProvider(LocalGlassUi provides GlassUi())` **di dalam** subtree `sakuLayerBackdrop`: `backdrop` sengaja `null` di sana, sehingga tidak ada komponen halaman yang bisa memasang kaca lagi. Ini pengaman struktural untuk perubahan berikutnya.
3. `GlassUi.home` dihapus; `glassAreas` tinggal 3 area overlay (Dock / Tombol catat / Sheet). `Saku.glassPref()` punya hard guard: area di luar daftar selalu `false`.
4. Preferensi lama dibersihkan: `safeUiGlass()` membuang kunci asing/terlarang di `decodeSaku`, `decodePrefs`, `encodeUiGlass`, dan `Store.load()` menulis ulang `K_UI` bila ada kunci yang dibuang. Jadi perangkat yang sudah telanjur menyimpan `"home":true` tidak bisa menyalakan lagi jalur yang crash. (DataStore lama juga berisi `"sheet":true` — sheet kaca tetap aktif dan diuji, karena sheet memang saudara dari layer yang direkam.)
5. `Setting > Theme > UI` dan `Ui.kt` diberi catatan kenapa halaman/ringkasan selalu kertas.

**Verifikasi (`adb`, perangkat `RRCYA016B5A` / SM-A566B, Android 17 / SDK 37):**

- `:app:compileDebugKotlin` dan `:app:assembleDebug` sukses, `apksigner verify` → `Verifies` (v2).
- `adb install -r saku-1.4-debug.apk` → `Success`; `logcat -b all -c` lalu cold start. **Buffer `crash` kosong**, tidak ada `SIGSEGV` / `RenderNode::prepareTreeImpl` / `FATAL EXCEPTION`.
- Preferensi lama di perangkat benar-benar dibersihkan: `{"home":true,"sheet":true}` → **`{"sheet":true}`** (kunci `home` hilang dari `saku_v2.preferences_pb`).
- Setting › Theme › UI kini menampilkan **3 area** (Dock, Tombol catat, Sheet), semuanya dalam posisi **Kaca** — jadi mode liquid aktif penuh, bukan dinonaktifkan diam-diam.
- Sheet "Catat transaksi" dibuka lewat FAB: kaca bekerja (konten Home terlihat buram di belakang sheet), tidak crash. Sheet detail transaksi juga dibuka, tidak crash.
- APK final: `saku-1.4-debug.apk` (12.869.286 byte), SHA256 `c81fde54a0a019efc3552978c5f19d679984db27decc4e64941039ba3c4479c6`; salinan di root workspace `Saku-V1.4-LiquidGlass.apk`.

### Catatan

- Build `assembleDebug` sukses tanpa warning kode Kotlin (hanya warning deprecated plugin Gradle yang sudah ada sejak V1.2). APK V1.4 (termasuk patch crash) disimpan sebagai `saku-1.4-debug.apk` di root repo.
- V1.x tetap rencana: scan + AI, sinkronisasi aplikasi lain, backup ke Google Drive. Fondasi backup/restore lokal untuk itu sudah ada.

V1.3 (bug fix)

1. untuk slider pada chart itu bisa di hold dan langsung memunculkan data pada bulan itu juga, dan fix untuk saat di slide ke kanan malah ke menu transaksi jadi berikan space untuk menggeser chart pada bulan
2. pada bar di bawah bug sudah di benahi tapi untuk sistemnya jangan di rubah yaitu saat hold belum di lepas jangan membuka halaman nya
3. kadang ketika terlalu cepat menekan menu seperti home ke transaksi secara berulang mengalami freze atau berhenti di tengah tengah
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
