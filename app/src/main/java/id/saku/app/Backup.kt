package id.saku.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * V1.4 #3 — satu arsip ZIP berisi `saku.json` (seluruh data) + direktori foto.
 *
 * Ekspor memakai SAF (ACTION_CREATE_DOCUMENT) dan impor memakai ACTION_OPEN_DOCUMENT,
 * jadi tidak ada permission penyimpanan. Path foto di dalam JSON dibuat relatif
 * supaya arsip bisa dipulihkan di perangkat lain yang punya path filesDir berbeda.
 */
object Backup {
    const val DB = "saku.json"
    const val MANIFEST = "manifest.json"
    const val PHOTO_DIR = "photos/"
    /** V1.5 #5 — gambar motivasi target nabung ikut satu arsip dengan foto. */
    const val GOAL_DIR = "goals/"

    private const val MAX_ENTRIES = 400
    private const val MAX_TOTAL = 100L * 1024 * 1024
    private const val MAX_ONE = 25L * 1024 * 1024
    private val SAFE_NAME = Regex("[A-Za-z0-9_.-]+")

    fun suggestedName(): String = "saku-backup-" + Fmt.stamp() + ".zip"

    private fun ZipOutputStream.text(name: String, value: String) {
        putNextEntry(ZipEntry(name))
        write(value.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    /** JSON dengan path foto relatif terhadap filesDir. */
    private fun portableJson(s: Saku): String {
        val o = JSONObject(encodeSaku(s))
        val arr = o.optJSONArray("tx") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i)
            val names = JSONArray()
            t.optJSONArray("photos")?.let { pa ->
                for (j in 0 until pa.length()) {
                    val p = pa.optString(j, "")
                    if (p.isNotEmpty()) names.put(PHOTO_DIR + File(p).name)
                }
            }
            if (names.length() == 0 && t.optString("photo").isNotEmpty()) {
                names.put(PHOTO_DIR + File(t.optString("photo")).name)
            }
            t.put("photos", names)
            t.remove("photo")
        }
        // V1.5 #5: path absolut gambar target dibuat relatif terhadap filesDir
        // supaya arsip tetap bisa dipulihkan di perangkat lain.
        o.optJSONArray("goals")?.let { goals ->
            for (i in 0 until goals.length()) {
                val g = goals.getJSONObject(i)
                val img = g.optString("image", "")
                if (img.isNotEmpty()) g.put("image", GOAL_DIR + File(img).name)
            }
        }
        o.put("photosRelative", true)
        return o.toString()
    }

    /** @return jumlah berkas foto yang ikut terarsip. */
    fun export(ctx: Context, uri: Uri, s: Saku): Result<Int> = runCatching {
        val out = ctx.contentResolver.openOutputStream(uri)
            ?: error("Tidak bisa menulis ke lokasi yang dipilih")
        var photos = 0
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.text(
                MANIFEST,
                JSONObject().apply {
                    put("app", "saku")
                    put("format", 1)
                    put("created", System.currentTimeMillis())
                    put("transactions", s.tx.size)
                }.toString()
            )
            zip.text(DB, portableJson(s))
            photosDir(ctx).listFiles()
                ?.filter { it.isFile }
                ?.sortedBy { it.name }
                ?.forEach { f ->
                    zip.putNextEntry(ZipEntry(PHOTO_DIR + f.name))
                    f.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                    photos++
                }
            // V1.5 #5: gambar target nabung
            goalsDir(ctx).listFiles()
                ?.filter { it.isFile }
                ?.sortedBy { it.name }
                ?.forEach { f ->
                    zip.putNextEntry(ZipEntry(GOAL_DIR + f.name))
                    f.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                    photos++
                }
        }
        photos
    }

    /**
     * Impor dengan validasi: nama entry aman (tidak ada path traversal), jumlah dan
     * ukuran dibatasi, `saku.json` wajib ada, dan isinya harus lolos [Saku.isSane].
     * Berkas foto lama sengaja tidak dihapus supaya impor tidak pernah membuang data.
     *
     * @return pasangan (data hasil impor, jumlah foto yang diekstrak).
     */
    fun import(ctx: Context, uri: Uri): Result<Pair<Saku, Int>> = runCatching {
        val input = ctx.contentResolver.openInputStream(uri)
            ?: error("Tidak bisa membaca berkas yang dipilih")
        val dir = photosDir(ctx)
        var raw: String? = null
        var photoCount = 0
        var total = 0L
        val chunk = ByteArray(16 * 1024)

        ZipInputStream(BufferedInputStream(input)).use { zis ->
            var entries = 0
            while (true) {
                val e = zis.nextEntry ?: break
                entries++
                if (entries > MAX_ENTRIES) error("Arsip memiliki terlalu banyak berkas")
                val name = e.name
                if (name.contains("..") || name.startsWith("/") || name.contains("\\")) {
                    error("Nama berkas tidak aman: $name")
                }
                val buf = ByteArrayOutputStream()
                var size = 0L
                while (true) {
                    val n = zis.read(chunk)
                    if (n < 0) break
                    size += n
                    total += n
                    if (size > MAX_ONE) error("Berkas di arsip terlalu besar: $name")
                    if (total > MAX_TOTAL) error("Arsip terlalu besar")
                    buf.write(chunk, 0, n)
                }
                val bytes = buf.toByteArray()
                when {
                    name == DB -> raw = String(bytes, Charsets.UTF_8)
                    name.startsWith(PHOTO_DIR) && name.length > PHOTO_DIR.length -> {
                        val fName = name.removePrefix(PHOTO_DIR)
                        if (!fName.matches(SAFE_NAME)) error("Nama foto tidak aman: $name")
                        if (!fName.contains("/")) {
                            File(dir, fName).outputStream().use { it.write(bytes) }
                            photoCount++
                        }
                    }
                    name.startsWith(GOAL_DIR) && name.length > GOAL_DIR.length -> {
                        val fName = name.removePrefix(GOAL_DIR)
                        if (!fName.matches(SAFE_NAME)) error("Nama gambar target tidak aman: $name")
                        File(goalsDir(ctx), fName).outputStream().use { it.write(bytes) }
                        photoCount++
                    }
                }
                zis.closeEntry()
            }
        }

        val json = raw ?: error("Arsip tidak berisi $DB")
        // Pastikan ini benar-benar JSON Saku sebelum dipakai.
        JSONObject(json).optJSONArray("tx") ?: error("Isi $DB tidak dikenali")
        val decoded = decodeSaku(json)
        val s = decoded.copy(
            tx = decoded.tx.map { t ->
                t.copy(
                    photo = "",
                    photos = t.allPhotos.map { p ->
                        if (p.startsWith(PHOTO_DIR)) File(ctx.filesDir, p).absolutePath else p
                    }
                )
            },
            // V1.5 #5: path relatif gambar target dipetakan ke filesDir tujuan.
            goals = decoded.goals.map { g ->
                val img = g.image
                if (img.startsWith(GOAL_DIR)) {
                    val fName = img.removePrefix(GOAL_DIR)
                    g.copy(image = if (fName.matches(SAFE_NAME)) File(goalsDir(ctx), fName).absolutePath else "")
                } else g
            }
        )
        if (!s.isSane()) error("Data di arsip tidak valid")
        s to photoCount
    }
}
