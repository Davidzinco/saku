# DESIGN.md — Saku

Arah desain untuk aplikasi Android **Saku** (Kotlin + Jetpack Compose). Dokumen ini adalah sumber kebenaran: kalau kode menyimpang dari sini, dokumen ini yang menang.

Mock acuan (HTML, bukan kode produksi): `design/konsep-buku-tabungan-v4.html` → render `design/konsep-beranda-v4.png`. Versi lama (v1–v3) disimpan sebagai riwayat arah desain. Nilai px di mock = dp di Compose pada lebar 390dp.

---

## Context (dari discovery)

- **Artifact type:** mobile app — personal finance tracker, single user, data lokal, tanpa akun.
- **Positioning:** utilitarian dan tenang; bukan fintech marketing, bukan dashboard analitik.
- **Audience:** pengguna Android 20–35 di Indonesia, uang tunai + e-wallet, sering cek saldo sambil berdiri.
- **Aksi utama:** mencatat satu pengeluaran dalam di bawah 10 detik.
- **Adjectives:** tenang, tercetak, jujur, rapi.
- **Terjemahan kata → gerakan visual:**
  - tenang → tanpa warna menyala, tanpa gradien, tanpa bayangan bertumpuk; pemisah berupa garis rambut dan jarak.
  - tercetak → tata letak kolom buku tabungan, label kecil di atas angka, angka tabular rata kanan.
  - jujur → semua angka punya sumber yang bisa ditelusuri (saldo berjalan, total kategori yang cocok dengan total keluar).
  - rapi → satu grid 4 kolom yang sama di semua layar, margin 24dp konsisten.
- **Aesthetic essence:** kertas, tinta, cap.
- **Single-minded proposition:** "ini buku tabungan saya", bukan "ini aplikasi keuangan".
- **References yang dipakai:** buku tabungan bank (kolom tanggal/keterangan/debit/kredit/saldo) dan rekening koran cetak.
- **Yang dihindari:** dark fintech neon (hitam + lime), glassmorphism, kartu SaaS bertumpuk, chip kategori pelangi.
  - **Pengecualian V1.4 (terkontrol):** glassmorphism diizinkan sebagai **mode opsional per area**, bukan desain paksaan. Setting › Theme › Mode dasar (Kertas/Kaca) jadi sakelar utama; Setting › Theme › **UI** memilih sendiri mana dari **Dock / Tombol catat / Sheet** yang memakai kaca. Default: Dock + FAB kaca, Sheet kertas. Mode **Kertas** tetap permukaan datar + hairline seperti yang ditetapkan dokumen ini. Tidak ada gradien atau glow tambahan; warna kaca tetap berasal dari token `Tok`, bukan warna bawaan library. Tata letak, ikon, dan label tidak pernah berubah antar mode.
  - **Batas teknis (patch crash 2026-11-18):** kaca hanya untuk **permukaan overlay**, yaitu saudara dari Box `sakuLayerBackdrop` — tidak pernah untuk komponen di dalam halaman (Ringkasan Home, daftar transaksi, dll.). Alasannya: `LayerBackdrop` merekam subtree itu ke satu `GraphicsLayer` dan `drawBackdrop` memanggil `drawLayer()` pada layer yang sama; kaca di dalam subtree membuat display list mereferensi dirinya sendiri dan memicu rekursi `RenderNode::prepareTreeImpl` sampai RenderThread `SIGSEGV`. `App.kt` menjaga aturan ini secara struktural dengan `CompositionLocalProvider(LocalGlassUi provides GlassUi())` di dalam subtree pager, dan `Saku.glassPref()` menolak area di luar daftar. Secara visual tidak ada yang hilang: di belakang Ringkasan Home hanya ada latar polos `t.bg`, jadi efek lensa tidak punya apa pun untuk dibiaskan.
- **Mode:** terang dan gelap, setara. **Density:** padat-sopan (setiap inci berisi, tapi ada udara di antara section).

## Aesthetic

- **Direction:** *Buku Tabungan* — aplikasi diperlakukan sebagai satu lembar rekening cetak, bukan kumpulan kartu.
- **Defining trait:** semua konten hidup di satu lembar kertas dengan garis rambut; tidak ada kartu terpisah, tidak ada elevation kecuali dua elemen mengambang (dock dan tombol catat).
- **Signature move:** **kolom saldo berjalan** di setiap baris transaksi (saldo setelah transaksi itu), dicetak pudar, dan baris teratas dicap cobalt. Fitur ini yang membuat bentuknya tidak bisa tertukar dengan aplikasi keuangan lain.

## Typography

- **Display / angka besar:** Newsreader (Google Fonts, OFL) — dipakai **hanya** untuk nominal ≥ 28sp (saldo utama, total nabung).
- **UI / body:** Archivo (Google Fonts, OFL) — semua teks, label, nominal kecil.
- **Bundle sebagai resource:** `res/font/archivo_regular.ttf`, `archivo_medium.ttf`, `archivo_semibold.ttf`, `archivo_bold.ttf`, `newsreader_regular.ttf` (opsz 6–72, weight 400). Jangan pakai `FontFamily.Default` untuk teks apa pun.
- **Scale** (rasio ~1.28, basis 14sp):

| step | size | line-height | pakai |
|------|------|-------------|-------|
| hero | 43sp | 1.06 | saldo akhir |
| display | 32sp | 1.15 | total nabung |
| title | 19sp | 1.3 | judul layar lain (Transaksi/Nabung/Atur) |
| body | 14sp | 1.25 | catatan transaksi, nama goal |
| body-sm | 13.5sp | 1.2 | nominal baris |
| label | 11.5sp | 1.2 | label field, meta |
| micro | 10.5sp | 1.2 | waktu, caption grafik, hitungan |

- **Weights:** 400 (Newsreader), 500 (mayoritas teks), 600 (judul section, nominal aktif), 700 (wordmark saja).
- **Tabular numerals wajib** untuk semua angka rupiah: `fontFeatureSettings = "tnum"`. Tanpa ini kolom saldo tidak rata.
- **Tracking:** wordmark `0.22em`; label uppercase hanya pada header kolom tabel `0.08em`; selain itu `0`. Jangan pakai ALL CAPS untuk label section.
- **Hitungan uang:** `Rp` sebagai prefix kecil 16sp warna `muted` di hero; di dalam baris, `Rp` dihilangkan karena header kolom sudah menyebut konteks. Minus memakai `−` (U+2212), bukan hyphen.

## Color

**Strategi:** kertas hijau-abu pucat dengan tinta hijau-hitam dan satu cap cobalt. Ini menjauh dari dua kluster default aplikasi keuangan (hitam + neon, atau krem + terakota) karena kertasnya berhue hijau, bukan krem, dan aksennya biru tinta, bukan lime atau oranye.

**Distribusi:** 60 netral (kertas/lembar/garis) / 30 tinta / 10 cap cobalt.

| role | OKLCH | hex | pakai |
|------|-------|-----|-------|
| bg (kertas) | oklch(94.8% 0.011 123) | `#ECEFE7` | latar layar |
| surface (lembar) | oklch(97.7% 0.007 116) | `#F7F8F3` | dock, sheet, field |
| fg (tinta) | oklch(25.9% 0.020 163) | `#1B2721` | teks utama, nominal |
| muted | oklch(51.0% 0.021 161) | `#5C6A62` | label, meta penting |
| muted-2 (teks kecil) | oklch(51.5% 0.020 153) | `#5F6B62` | tanggal, waktu, caption, header kolom |
| faint (non-teks) | oklch(67.1% 0.019 148) | `#8E998F` | titik pemisah, ikon nonaktif, disabled |
| accent (cap) | oklch(40.0% 0.134 265) | `#23418F` | aksi utama, tab aktif, cap saldo |
| accent-wash | oklch(40.0% 0.134 265 / 7%) | `rgba(35,65,143,.07)` | latar tab aktif, ikon nabung |
| success / masuk | oklch(49.8% 0.079 156) | `#39704F` | nominal masuk, progress nabung |
| warning | oklch(49.7% 0.095 87) | `#7A5E12` | peringatan lunak |
| error | oklch(49.5% 0.142 31) | `#A33A2B` | kegagalan sistem saja |
| hair | oklch(25.9% 0.020 163 / 10%) | `rgba(27,39,33,.10)` | garis pemisah tabel |
| hair-soft | oklch(25.9% 0.020 163 / 6%) | `rgba(27,39,33,.06)` | garis antar baris, batas section |
| bar | oklch(86.6% 0.019 138) | `#CDD6CA` | batang grafik tidak aktif |
| tint-1..4 | oklch(30.5/58.2/75.8/88.0% 0.02 150) | `#24332B #71806F #A9B4A7 #D3DAD0` | segmen kategori |

**Aturan warna:**
1. **Pengeluaran memakai warna `error` (merah) + tanda `−`.** Diminta eksplisit oleh pemilik aplikasi di V1.2; masuk/keluar tetap dibedakan oleh tanda dan posisi, bukan warna saja.
2. Kategori dibedakan lewat **glyph**, bukan warna. Grafik kategori memakai satu ramp tinta 4 tingkat, bukan empat hue.
3. Cap cobalt hanya untuk tiga hal: aksi utama (FAB, tombol simpan), tab aktif, dan saldo baris teratas. Kalau muncul di tempat keempat, hapus.
4. Tidak ada gradien. Tidak ada glow.

**Palet:** satu nada kertas + satu aksen, dihitung menjadi token lengkap (bg, lembar, tinta, muted, garis rambut, ramp kategori). Palet bawaan: Kertas, Krem & Terakota, Baja, Hutan, Ungu, Monokrom — plus custom (8 aksen × 4 nada kertas). **Default aplikasi: Monokrom + mode gelap.**

**Dark mode (dirancang, bukan inversi):**

| role | hex |
|------|-----|
| bg | `#121A16` (oklch 20.8% 0.014 163) |
| surface | `#1A241F` (oklch 24.9% 0.017 163) |
| fg | `#E7EDE5` (oklch 93.9% 0.012 138) |
| muted | `#A3AFA5` |
| muted-2 | `#8B978D` |
| accent | `#8FA5F2` (cobalt dinaikkan lightness, saturasi turun) |
| success | `#7FBE99` |
| hair | `rgba(231,237,229,.12)` / hair-soft `rgba(231,237,229,.07)` |
| bar | `#2B3A32` (oklch 33.2% 0.024 161) |

## Spacing, radius, shadow

- **Base 4dp.** Skala: 2, 4, 6, 8, 10, 12, 16, 20, 24, 32.
- **Margin layar:** 24dp kiri-kanan, tanpa layar yang melebar penuh.
- **Radius:** tiga nilai saja — `4dp` (batang grafik, baris progress), `10dp` (ikon tile, field), `22dp` (dock dan sheet). Tombol catat bulat penuh.
- **Shadow:** hanya untuk elemen mengambang (`dock`, FAB). Aturannya `0 10px 28px -8px rgba(27,39,33,.22)`; FAB memakai bayangan berwarna aksen `rgba(35,65,143,.45)`. Semua elemen lain memakai **garis rambut**, bukan bayangan. Satu elemen tidak boleh punya garis rambut dan bayangan sekaligus.

## Layout dan komposisi

- **Grid daftar:** 4 kolom — Tanggal `40dp` / Keterangan `1fr` / Jumlah `82dp` / Saldo `76dp`, gap `10dp`.
- **Ritme:** rapat di dalam grup (6–10dp), lega antar section (10–12dp padding + garis rambut). Section dipisah garis **atau** jarak, tidak keduanya.
- **Anggaran tinggi layar Beranda** (390×844, harus berakhir di atas dock):

```
status            36dp
wordmark          26dp   (V1.4: "Lembar [date]" dihapus)
saldo              96dp   (V1.4: label "Saldo akhir" dihapus; angka 43 + 3 statistik 14.5)
grafik            124dp   (nav 28 + batang 46 + sumbu 10 + padding; V1.4: strip 26 + caption dihapus)
kategori           88dp   (judul 12.5 + batang 6 + legenda 2×2)
nabung             46dp   (ikon 26 + progress 4; bisa disembunyikan atau jadi deretan target)
daftar            24 + 22 + 49×n dp
dock              66dp, kapsul penuh, bottom 24dp
```

- **Urutan section Beranda wajib:** saldo → grafik bulan → kategori → nabung → transaksi. Jangan menambah section tanpa menghapus yang lain; layar ini sudah penuh.
- **Grafik:** bentuk *batang* atau *garis* (dipilih di Setting › Chart), rentang **6 bulan** (Jan–Jun / Jul–Des) atau **12 bulan** (Jan–Des). Jendela tidak bergeser saat pindah bulan: batang/titik yang tersorot mengikuti bulan yang sedang dilihat, bukan selalu di ujung kanan. Grafik adalah *ruang gestur* — menekan (tanpa menggeser) langsung memilih bulan di bawah jari; menggeser mengikuti posisi jari, bukan akumulasi ambang. Down dikonsumsi di area ini dan `userScrollEnabled` pager dimatikan selama scrub, supaya geser kanan tidak pindah ke halaman Transaksi. **V1.4:** strip slider 26dp dan caption "Tertinggi …"/"tahan & geser" dihapus, jadi anggaran turun ke ~124dp. Data dihitung sekali per jendela (`trendFast()`, satu lintasan transaksi) dan daftar transaksi tidak pernah dibongkar saat bulan berubah — animasi masuk kini per baris.
- **Tidak ada layout tengah.** Semua teks rata kiri; semua angka rata kanan pada kolomnya.
- **Layar lain mengikuti grammar yang sama:** judul layar 19sp di kiri atas, tanpa kartu, daftar dengan garis rambut, angka tabular.
- **Breakpoint:** 360–430dp saja (portrait). Tidak ada layout tablet atau landscape yang dirancang; di layar lebih lebar, konten tetap 24dp margin, bukan melebar.

## Komponen dan state

**Navigasi halaman**
- Empat halaman (Home, Transaksi, Nabung, Setting) hidup di satu pager horizontal. Geser ke kiri/kanan untuk pindah halaman tanpa menyentuh dock; dock selalu menyorot halaman yang sedang tampil.
- Perpindahan halaman 320ms, transform + opacity saja (`slideInHorizontally` 1/5 lebar + fade).

**Time Input (sheet) — V1.4 #2, acuan `design/time.webp`**
- Satu field 48dp: *contextual prefix* berupa ikon jam, nilai `HH:mm` atau `hh:mm aa`, clear-time (×) yang mengembalikan jam ke waktu saat ini, dan chevron pembuka popover. Collapsed by default.
- Popover: tiga kolom dengan *configured intervals* — Jam (0–23 atau 1–12), Menit (tiap 5 menit, nilai aktual selalu disisipkan), dan AM/PM saat format 12 jam. *Selected segments* disorot `accent`. Toggle format 24 jam/12 jam di kepala popover; 4 preset pagi/siang/sore/malam di kakinya.
- Tidak ada keyboard, jadi tidak ada inset IME yang mengangkat sheet; `imePadding` tetap hanya di kontainer sheet. Angka tabular dan rata tengah pada kolom. Stepper V1.3 dihapus.

**Sheet**
- Nominal: pemisah ribuan muncul saat mengetik (`1.250.000`), angka saja.
- Urutan field: jenis → Nominal → Category → **Note (+ ikon kamera/galeri)** → Date → Time. **V1.4 #7:** lampiran foto jadi dua ikon 17dp di ujung field Note, bukan tombol atau section tersendiri; thumbnail muncul di bawahnya hanya bila ada. Maksimal 6 lampiran, tanpa permission penyimpanan (FileProvider + Photo Picker), disimpan WebP 1280px + thumbnail 320px. Tidak ada caption "Opsional. …".
- Footer sticky: aksi utama (dan "Hapus transaksi" saat mengubah) selalu di bawah sheet, tidak ikut tergulir.
- Tutup: scrim, tombol ×, atau back. Hapus butuh dua ketuk konfirmasi.
- **Sheet detail transaksi:** klik baris tidak lagi langsung membuka editor. Detail menampilkan jenis, nominal, deretan foto, lalu baris Kategori / Catatan / Tanggal panjang / Waktu / Saldo setelah / Target nabung. **V1.4 #6:** ada navigasi `n / total` + chevron ke transaksi sebelumnya/berikutnya; menghapus tidak menutup sheet mendadak (pindah ke tetangga atau tampil konfirmasi + "Tutup"). **V1.4 #7:** ketuk foto membuka pratinjau layar penuh dengan pinch-zoom. Grammar hairline dan tipografi sama dengan sheet lain.

**Dock navigasi**
- 5 slot: Beranda, Transaksi, [+], Nabung, Atur. Tinggi 66dp, **radius kapsul penuh (50%)** sejak V1.4 #4, surface + hairline.
- Tab aktif: warna `accent` + wash lembut berbentuk kapsul penuh.
- Ikon 20dp, stroke 1.5, label 10sp.
- State: default, aktif, ditekan (wash 12%), **ditahan (dock membesar 1.045× dengan spring, V1.4 #4)**, digeser (pill mengikuti jari, snap ke slot terdekat). Aksi utama (FAB) selalu terlihat, tidak pernah tersembunyi di menu.
- **V1.3 #2/#3:** saat digeser, wash mengikuti jari tetapi halaman **baru dibuka setelah jari dilepas** (sistem geser tidak diubah). Semua perpindahan tab lewat satu jalur dengan animasi lama dibatalkan, jadi klik cepat berulang tidak membuat pager berhenti di tengah.
- **Mode Kaca per area (V1.4 #8, dikoreksi patch crash):** `drawBackdrop` dari Backdrop 2.0.1 — `vibrancy() + blur(4–6dp) + lens(9–12dp)` dengan highlight tepi, shadow, dan inner shadow — bisa dipasang pada **Dock, Tombol catat, dan Sheet** (`glassPanel`), dipilih sendiri di Setting › Theme › UI. Seluruh halaman direkam oleh satu `LayerBackdrop` dengan latar opaque (`drawRect(bg)` sebelum `drawContent()`) supaya tidak ada piksel kaca transparan. **Ketiga permukaan kaca dirender sebagai saudara dari Box yang direkam**, bukan di dalamnya — kalau `drawBackdrop` ikut terekam, display list memuat dirinya sendiri dan HWUI mengulang `RenderNode::prepareTreeImpl` sampai stack RenderThread habis (`SIGSEGV`, tanpa exception Kotlin). Karena itu Ringkasan Home dan seluruh isi pager selalu kertas; pengaman struktural ada di `App.kt` (`LocalGlassUi` null di dalam subtree) dan `Data.kt` (`safeUiGlass()` + hard guard `glassPref()`). Warna tetap `Tok.sheet` / `Tok.accentWash` / `Tok.accent`; bentuk kapsul dock dan bulat penuh FAB tidak berubah. Di Android <12 efek otomatis menjadi permukaan datar. Tidak ada perubahan pada tata letak, ikon, atau label.

**Baris transaksi**
- Default / ditekan (`hair-soft` overlay) / baris teratas (saldo berwarna `accent`, tebal 600).
- Nominal tanpa `Rp`, tabular, rata kanan. Meta 10.5sp: kategori, pemisah titik 2.5dp, jam.

**Tombol**
- Primary: isi `accent`, teks putih, radius bulat penuh, tinggi 48dp. Label = aksi ("Simpan transaksi").
- Secondary: garis rambut 1dp, teks tinta, latar transparan.
- Tertiary: teks saja, warna `accent`.
- State: default, ditekan (opacity .88 / scale .98), fokus (ring 2dp `accent` offset 2dp), disabled (faint 40%), loading (label diganti spinner, lebar tetap).

**Field (sheet catat transaksi)**
- Label di atas field, 11.5sp `muted`. Placeholder bukan label.
- Nominal: keyboard angka, prefix `Rp` di dalam field, validasi saat blur (bukan saat mengetik).
- Error: teks 11.5sp `error` di bawah field + garis field `error`; input tidak dihapus.
- Kalender: grid 7 kolom, tanggal terpilih = lingkaran `accent`, hari ini = garis rambut.

**Empty / loading / error**
- Kosong: satu kalimat arahan + satu aksi. "Belum ada transaksi bulan ini. Catat yang pertama."
- Loading: skeleton baris (bukan spinner penuh layar), tinggi sama dengan baris asli.
- Error: "Data gagal disimpan. Coba lagi." + tombol ulangi. Tidak ada kata "oops" atau permintaan maaf.

**Focus ring:** 2dp `accent` dengan offset 2dp, terlihat di kedua mode; jangan hapus outline tanpa pengganti.

## Motion

- Skala durasi: 120ms (tekan), 180ms (masuk elemen), 240ms (sheet), 320ms (pindah layar).
- Easing: `FastOutSlowIn` untuk masuk, `LinearOutSlowIn` untuk keluar. Tidak ada bounce.
- Yang dianimasikan: transform dan opacity saja. Jangan animasi `width/height/padding`.
- Motion tanda tangan: pill dock bergerak dengan spring `dampingRatio 0.82`, `stiffness 650` saat tab berubah, dan snap `0.72 / 900` setelah digeser.
- V1.2: baris transaksi baru/berubah diberi kilatan `accent` 16% selama 900ms; daftar memudar masuk 240ms saat bulan berganti; angka saldo/masuk/keluar/selisih bergerak 360–420ms; batang grafik tumbuh bertahap 18ms per batang; sheet punya animasi keluar 240ms sebelum dilepas.
- V1.3 #4: baris transaksi masuk dengan fade + translasi 16px, stagger 26ms per baris (maksimal 8 baris pertama), durasi `Motion.ENTER`. Tetap transform + opacity, dan langsung snap bila animasi sistem dimatikan.
- V1.4 #1: animasi baris diulang lewat kunci bulan, bukan dengan membongkar `LazyColumn` (fade seluruh daftar 240ms saat ganti bulan dihapus). V1.4 #4: dock ditekan membesar 1.045× dengan spring `0.7 / 420`, kembali 1.0 saat lepas — tetap transform murni.
- `prefers-reduced-motion` → durasi 0ms, transisi jadi cross-fade instan.

## Iconography

- Satu set: 20dp grid, stroke 1.5dp, ujung dan sambungan membulat, sudut luar ikut radius 4dp.
- Ikon dock digambar sendiri (rumah, struk, dompet, slider) supaya tidak memakai `Icons.Rounded.*` default Material yang jadi penanda template. V1.4 menambah jam (Time Input), kamera/gambar (lampiran Note), backup/restore, dan pin nabung dengan grammar yang sama.
- Ikon kategori: outline, satu warna tinta, dibedakan bentuk. Tidak ada tile berwarna per kategori.
- Ikon 24dp untuk aksi di dalam sheet, 16dp untuk dekorasi inline.

## Imagery

- Tidak ada ilustrasi, maskot, atau tekstur dekoratif. Identitas visual dibawa oleh tipografi, garis, dan angka.
- Satu-satunya "gambar" adalah data: grafik batang, batang komposisi kategori, progress nabung. Semuanya harus membawa angka nyata di dekatnya, bukan dekorasi.
- **Pengecualian V1.4:** foto bukti transaksi yang dilampirkan pengguna. Foto bukan elemen layout — tidak ada placeholder ilustrasi saat kosong, tidak ada caption promosi, dan di Home tidak pernah ditampilkan. Disimpan terkompresi (WebP 1280px + thumbnail) agar tidak membebani sheet.

## Accessibility

- **Kontras (diukur):** tinta/kertas 13.3:1, muted 4.9:1, muted-2 `#5F6B62` 4.8:1, aksen 8.1:1, success 5.0:1, warning `#7A5E12` 5.3:1, error 5.7:1. Dark: tinta 14.9:1, muted 7.8:1, aksen 7.5:1.
- **Jangan pakai `#8E998F` untuk teks** (2.5:1). Nilai itu hanya untuk titik pemisah, ikon nonaktif, dan border.
- **Target sentuh:** minimal 44×44dp (dock, baris, tombol). Baris 49dp sudah memenuhi.
- **Arti tidak boleh bergantung pada warna:** masuk/keluar dibedakan oleh tanda `+`/`−` dan posisi, bukan warna.
- **Talkback:** setiap baris dibaca "16 November, makan warung, keluar 18.500, saldo 16.421.404". Grafik diberi `contentDescription` berisi angka, bukan "grafik batang".
- **Font scaling:** layout harus tetap utuh sampai 1.3× (angka hero turun ke 34sp otomatis).

## Copywriting (Bahasa Indonesia)

- Sentence case di mana-mana, kecuali wordmark dan header kolom.
- Nama aksi konsisten dari tombol sampai konfirmasi: tombol "Simpan transaksi" → toast "Transaksi tersimpan".
- Kata yang dipakai: saldo, masuk, keluar, catat, nabung, kategori, lembar. Kata yang dihindari: "transaksi berhasil diproses", "fitur", "dashboard".
- Angka rupiah: `Rp 16.421.404` (spasi setelah Rp, titik ribuan). Nominal negatif: `−18.500`.
- Judul section tidak memakai titik tengah sebagai pemisah meta; pemisah adalah elemen titik 2.5dp atau garis rambut.
- **V1.4 #9 — copy yang dihapus:** "Lembar [date]" (wordmark), label "Saldo akhir" di Home, caption "Opsional. Lampirkan struk atau foto ke catatan.", "Tertinggi [bulan], [nominal]", dan petunjuk "tahan & geser". Label yang masih dibutuhkan untuk memberi arti angka disingkat ("Saldo"), bukan dihapus buta.

## Tokens (source of truth)

Adapter: **Kotlin/Compose** — perluas `Tok` di `Theme.kt`:

```kotlin
val Light = Tok(
    bg = Color(0xFFECEFE7), sheet = Color(0xFFF7F8F3),
    ink = Color(0xFF1B2721), muted = Color(0xFF5C6A62),
    muted2 = Color(0xFF5F6B62), faint = Color(0xFF8E998F),
    hair = Color(0x1A1B2721), hairSoft = Color(0x0F1B2721),
    accent = Color(0xFF23418F), accentWash = Color(0x1223418F),
    pos = Color(0xFF39704F), warn = Color(0xFF7A5E12), error = Color(0xFFA33A2B),
    bar = Color(0xFFCDD6CA),
    tint1 = Color(0xFF24332B), tint2 = Color(0xFF71806F),
    tint3 = Color(0xFFA9B4A7), tint4 = Color(0xFFD3DAD0)
)
val Dark = Tok(
    bg = Color(0xFF121A16), sheet = Color(0xFF1A241F),
    ink = Color(0xFFE7EDE5), muted = Color(0xFFA3AFA5),
    muted2 = Color(0xFF8B978D), faint = Color(0xFF6B7770),
    hair = Color(0x1FE7EDE5), hairSoft = Color(0x12E7EDE5),
    accent = Color(0xFF8FA5F2), accentWash = Color(0x1F8FA5F2),
    pos = Color(0xFF7FBE99), warn = Color(0xFFD8B45C), error = Color(0xFFE38A7A),
    bar = Color(0xFF2B3A32),
    tint1 = Color(0xFFE7EDE5), tint2 = Color(0xFF9DAE9F),
    tint3 = Color(0xFF5A6B5E), tint4 = Color(0xFF33443A)
)
```

Skala tipografi: `hero 43sp`, `display 32sp`, `title 19sp`, `body 14sp`, `bodySm 13.5sp`, `label 11.5sp`, `micro 10.5sp`, `tab 10sp`. Line-height seperti tabel di atas.

Spacing: `s1 2dp, s2 4dp, s3 6dp, s4 8dp, s5 10dp, s6 12dp, s7 16dp, s8 20dp, s9 24dp`.

### Perhitungan saldo berjalan

Untuk daftar terurut terbaru → terlama, saldo setelah baris ke-i:

```
saldoSetelah(i) = saldoTotal − Σ amount(j) untuk semua j < i
```

dengan `amount` bertanda (`+` untuk masuk, `−` untuk keluar). Baris pertama selalu sama dengan saldo total. Hitung sekali per render bulan, jangan di dalam item composable.

## Do / Don't (ringkas)

| Lakukan | Jangan |
|---|---|
| satu lembar, garis rambut, jarak | kartu bertumpuk dengan bayangan |
| angka tabular rata kanan | angka proporsional yang tepinya bergerigi |
| satu aksen cobalt, dipakai 3 tempat | gradien, glow, atau lima warna kategori |
| pengeluaran merah `error` + `−` | mengandalkan warna saja tanpa tanda |
| grafik selalu dengan angka | batang tanpa nilai dan tanpa caption |
| label sentence case 11.5sp | ALL CAPS ber-tracking di setiap section |
| ikon 20dp stroke 1.5 digambar sendiri | set ikon Material default |

## Slop audit

- **Tanggal:** 16 November 2026 | **Hasil:** lolos setelah 3 putaran revisi.
- **Tell yang tertangkap dan diperbaiki:**
  1. v1 memakai palet hitam `#05080A` + lime `#C8FF00` (kluster default fintech) → diganti kertas hijau + cobalt.
  2. v1 memakai ALL CAPS 8.5–10sp ber-tracking di setiap label + meta bertitik tengah → sentence case, satu header kolom saja.
  3. v1 memakai enam warna chip kategori (merah, kuning, biru, ungu, pink, tosca) → satu ramp tinta 4 tingkat.
  4. v1 menumpuk border 1dp + bayangan offset pada dock → hanya satu pendekatan (hairline untuk datar, bayangan untuk mengambang).
  5. v1 memakai dot-matrix Doto untuk saldo (terbaca sebagai kegagalan render, bukan mesin cetak) → Newsreader.
  6. Kontras `#8E998F` pada kertas 2.5:1 → dinaikkan ke `#5F6B62` (4.8:1) untuk teks; nilai lama hanya untuk non-teks.
- **Craft gate:** komponen punya matriks state, motion hanya transform/opacity dengan spring tunggal, ikon satu set, tidak ada imagery stok, dark mode dirancang lewat lightness.
- **Wireframe check:** struktur tanpa warna tetap khas — satu kolom cetak dengan 4 kolom tabel dan saldo berjalan; bukan grid kartu.

## Changelog

- **2026-11-16:** versi awal. Arah *Buku Tabungan* dipilih dari tiga kandidat (Buku Tabungan, Struk Kasir, Dompet), dua putaran penyempurnaan (kasar → halus → halus dan padat). Mock acuan: `design/konsep-buku-tabungan-v3.html`.
- **2026-11-16 (implementasi):** seluruh aplikasi ditulis ulang mengikuti dokumen ini. Kode lama (kertas hitam + lime, dock kaca `backdrop`, ikon Material, chip warna kategori, label ALL CAPS) dihapus. Font Archivo + Newsreader dibundel, ikon digambar sendiri di `Ui.kt`, dock menjadi satu lembar surface + garis rambut, tabel transaksi 4 kolom dengan saldo berjalan, kategori memakai satu ramp tinta. Data contoh dihapus — aplikasi mulai kosong.
- **2026-11-16 (V1.2 · mock v4):** `design/konsep-buku-tabungan-v4.html` + render `konsep-beranda-v4.png`: tiga frame (Home Kertas batang 12 bulan, Home Monokrom gelap garis, sheet dengan Time picker). Dokumen ini diperbarui untuk pager, Time picker, footer sticky, palet, dan default monokrom gelap.
- **2026-11-16 (V1.2):** tema default monokrom + mode gelap; pengeluaran berwarna `error`; input nominal berformat ribuan; Date + Time (picker geser, tanpa keyboard); edit/hapus transaksi; grafik Batang/Garis dengan rentang 6/12 bulan, bisa diketuk dan digeser; geser halaman untuk pindah tab; Setting dipecah jadi folder Theme/Category/Chart; ikon aplikasi adaptif.
- **2026-11-17 (V1.3):** scrub grafik posisional (tekan-tahan langsung ganti bulan) + strip gestur khusus + pager dinonaktifkan saat scrub; dock baru pindah halaman setelah jari dilepas; klik tab cepat tidak lagi menumpuk animasi; animasi masuk baris transaksi; Time field stepper + preset menggantikan dua Scrubber; sheet detail transaksi sebelum editor; foto pada note (FileProvider + Photo Picker, tanpa permission). Liquid glass dari `Kyant0/AndroidLiquidGlass` dipasang pada dock/wash/FAB sebagai mode **Kaca** opsional dengan toggle **Kertas/Kaca** di Theme — pengecualian terkontrol atas aturan "hindari glassmorphism" di atas.
- **2026-11-18 (V1.4):** scrub grafik dioptimalkan (`trendFast` + `LazyColumn` stabil) dan strip/caption bawah chart dihapus; Time Input mengikuti `design/time.webp` (field + popover 3 kolom interval); backup/restore ZIP via SAF dengan validasi; dock kapsul penuh yang membesar saat ditahan; penyimpanan pindah ke DataStore per-record; sheet detail dengan navigasi sebelumnya/berikutnya dan hapus tanpa menutup sheet; multi-foto + pinch-zoom + ikon kamera di dalam Note; Setting › Theme › UI untuk memilih kaca per area; Setting › Home untuk tampil/gulir/pin ringkasan nabung; kompresi foto WebP + thumbnail; lima copy redundan dihapus. Referensi baru: `design/time.webp`.
- **2026-11-18 (patch crash Kaca):** `adb` menemukan `SIGSEGV` native di RenderThread (>500 frame `RenderNode::prepareTreeImpl` berulang, "likely due to stack overflow") saat mode Kaca aktif. Penyebabnya: V1.4 memasang `glassPanel()` pada Ringkasan Home, yaitu **di dalam** subtree yang direkam `LayerBackdrop`; `drawLayer()` lalu mereferensi display list-nya sendiri. Kaca untuk komponen halaman dihapus, `App.kt` menutup akses backdrop di dalam pager lewat `CompositionLocalProvider` bertingkat, `glassAreas` jadi 3 area overlay (Dock / Tombol catat / Sheet), dan preferensi lama disanitasi (`safeUiGlass()` + hard guard `glassPref()` + penulisan ulang `K_UI` saat `load()`). Diuji di perangkat: app cold start + semua area Kaca + sheet kaca + sheet detail, tanpa crash. Rincian di `UPDATE.md`.
