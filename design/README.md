# design/

Mock HTML dan render PNG-nya. Ini artefak desain, **bukan kode produksi** — sumber kebenaran tetap [`../DESIGN.md`](../DESIGN.md), dan kode ada di `../app/`.

| File | Arah | Dipakai untuk |
|---|---|---|
| `konsep-buku-tabungan-v4.html` → `konsep-beranda-v4.png` | **Buku Tabungan v4** | Arah aktif (V1.2): Home Kertas dengan grafik batang 12 bulan, Home Monokrom gelap dengan grafik garis, sheet catat transaksi dengan Time picker dan footer sticky |
| `konsep-buku-tabungan-v3.html` → `konsep-beranda-v3.png` | Buku Tabungan v3 | Arah yang diimplementasikan di V1.0 (satu lembar, kolom saldo berjalan) |
| `konsep-buku-tabungan-v2.html` → `konsep-beranda-v2.png` | Buku Tabungan v2 | Putaran penyempurnaan kedua (riwayat) |
| `konsep-buku-tabungan.html` → `konsep-beranda.png` | Buku Tabungan v1 | Kandidat pertama, sebelum audit (riwayat) |

## Yang berubah di v4 (V1.2)

- Grafik: bentuk **batang/garis**, rentang **6/12 bulan**, jendela tidak bergeser, batang tersorot mengikuti bulan yang dilihat, bisa diketuk dan digeser.
- Time picker geser (Jam/Menit) menggantikan input keyboard; tidak ada inset IME yang menggeser sheet.
- Sheet punya **footer sticky**; hapus transaksi butuh dua ketuk konfirmasi.
- Nominal berformat ribuan saat mengetik; nominal keluar berwarna `error` (merah).
- Navigasi antar halaman dengan geser (pager); dock tetap sinkron.
- Setting dipecah jadi folder: Theme, Category, Chart, Data.
- Default tema: **Monokrom + mode gelap**.

## Cara render ulang

Buka file HTML di browser dengan lebar ±1620px (deviceScaleFactor 2), tinggi ±940px, lalu tangkap layar penuh. Frame ponsel di dalam mock berukuran 390×844 (sama dengan lebar kerja dp aplikasi).
