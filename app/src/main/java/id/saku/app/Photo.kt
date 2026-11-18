package id.saku.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Foto untuk catatan transaksi (V1.4 #7/#11).
 *
 * - Kamera lewat TakePicture + FileProvider ke cacheDir, galeri lewat Photo Picker:
 *   tidak ada permission penyimpanan.
 * - Semua berkas dikompres ke WebP maksimal 1280px + thumbnail 320px, jadi sheet
 *   detail tidak pernah memuat bitmap 12MP penuh.
 * - Beberapa lampiran per transaksi didukung lewat `Tx.photos`.
 */
fun photosDir(ctx: Context): File = File(ctx.filesDir, "photos").apply { mkdirs() }

fun newPhotoFile(ctx: Context): File =
    File(photosDir(ctx), "p_" + System.nanoTime().toString(36) + ".webp")

fun newTempFile(ctx: Context): File =
    File(ctx.cacheDir, "cam_" + System.nanoTime().toString(36) + ".jpg")

fun photoUri(ctx: Context, file: File): Uri =
    FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)

fun copyUriToFile(ctx: Context, uri: Uri, file: File): Boolean = try {
    ctx.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output -> input.copyTo(output) }
    } ?: return false
    true
} catch (e: Exception) {
    false
}

private fun thumbOf(path: String): File {
    val f = File(path)
    return File(f.parentFile, f.nameWithoutExtension + "_t.webp")
}

/** Hanya hapus berkas di dalam folder foto milik aplikasi. */
fun deletePhoto(path: String) {
    if (path.isEmpty()) return
    val marker = File.separator + "photos" + File.separator
    if (!path.contains(marker)) return
    try {
        File(path).delete()
        thumbOf(path).delete()
    } catch (e: Exception) {
    }
}

fun deletePhotos(paths: List<String>) = paths.forEach(::deletePhoto)

/** Decode dengan inSampleSize: tidak pernah memuat bitmap penuh ke memori. */
private fun decodeSampled(file: File, maxDim: Int): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) sample *= 2
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    BitmapFactory.decodeFile(file.absolutePath, opts)
} catch (e: Exception) {
    null
}

@Suppress("DEPRECATION")
private fun rotateToUpright(bitmap: Bitmap, file: File): Bitmap = try {
    val exif = android.media.ExifInterface(file.absolutePath)
    val deg = when (exif.getAttributeInt(
        android.media.ExifInterface.TAG_ORIENTATION,
        android.media.ExifInterface.ORIENTATION_NORMAL
    )) {
        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (deg == 0f) bitmap else Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(deg) }, true
    )
} catch (e: Exception) {
    bitmap
}

private fun fit(bitmap: Bitmap, maxDim: Int): Bitmap {
    val longest = maxOf(bitmap.width, bitmap.height)
    if (longest <= maxDim) return bitmap
    val k = maxDim.toFloat() / longest.toFloat()
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * k).roundToInt().coerceAtLeast(1),
        (bitmap.height * k).roundToInt().coerceAtLeast(1),
        true
    )
}

/** Simpan WebP + thumbnail; kembalikan path berkas utama. */
@Suppress("DEPRECATION")
fun storeImage(ctx: Context, source: File): String? = try {
    val raw = decodeSampled(source, 1600) ?: return null
    val upright = rotateToUpright(raw, source)
    val full = fit(upright, 1280)
    val dest = newPhotoFile(ctx)
    dest.outputStream().use { full.compress(Bitmap.CompressFormat.WEBP, 82, it) }
    val small = fit(full, 320)
    thumbOf(dest.absolutePath).outputStream().use {
        small.compress(Bitmap.CompressFormat.WEBP, 70, it)
    }
    dest.absolutePath
} catch (e: Exception) {
    null
}

private fun loadBitmap(path: String, maxDim: Int, preferThumb: Boolean): Bitmap? {
    val full = File(path)
    if (!full.exists()) return null
    val thumb = thumbOf(path)
    val src = if (preferThumb && thumb.exists()) thumb else full
    return decodeSampled(src, maxDim)
}

@Composable
fun PhotoThumb(path: String, t: Tok, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(path) { loadBitmap(path, 320, preferThumb = true)?.asImageBitmap() }
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(t.bg)
            .border(1.dp, t.hair, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap, "Foto catatan", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Glyph(Ic.Image, t.muted2, 18.dp)
        }
    }
}

/** Pratinjau lebar untuk sheet detail transaksi. */
@Composable
fun PhotoPreview(path: String, t: Tok, modifier: Modifier = Modifier) {
    val bitmap = remember(path) { loadBitmap(path, 1280, preferThumb = false)?.asImageBitmap() }
    if (bitmap == null) return
    Image(
        bitmap,
        "Foto catatan",
        modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, t.hair, RoundedCornerShape(10.dp)),
        contentScale = ContentScale.Crop
    )
}

/** V1.4 #7 — pratinjau layar penuh dengan pinch-zoom dan geser. */
@Composable
fun PhotoViewer(t: Tok, paths: List<String>, start: Int, onClose: () -> Unit) {
    var index by remember { mutableStateOf(start.coerceIn(0, (paths.size - 1).coerceAtLeast(0))) }
    var zoom by remember(index) { mutableStateOf(1f) }
    var panX by remember(index) { mutableStateOf(0f) }
    var panY by remember(index) { mutableStateOf(0f) }
    val path = paths.getOrNull(index) ?: return
    val bitmap = remember(path) { loadBitmap(path, 1600, preferThumb = false)?.asImageBitmap() }

    Box(Modifier.fillMaxSize().background(if (t.dark) Color(0xF0000000) else Color(0xE9121A16))) {
        if (bitmap != null) {
            Image(
                bitmap,
                "Foto catatan",
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = panX
                        translationY = panY
                    }
                    .pointerInput(index) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                            if (zoom > 1f) {
                                panX += pan.x
                                panY += pan.y
                            } else {
                                panX = 0f
                                panY = 0f
                            }
                        }
                    },
                contentScale = ContentScale.Fit
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Foto tidak bisa dibuka", Type.body, Color.White)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (paths.size > 1) {
                Text("${index + 1} / ${paths.size}", Type.label, Color.White)
            }
            Spacer(Modifier.weight(1f))
            TouchTarget {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0x66FFFFFF))
                        .tap(onClose),
                    contentAlignment = Alignment.Center
                ) { Glyph(Ic.Close, Color.White, 18.dp) }
            }
        }

        if (paths.size > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ViewerArrow(Ic.ChevL) { index = (index - 1 + paths.size) % paths.size }
                ViewerArrow(Ic.ChevR) { index = (index + 1) % paths.size }
            }
        }
    }
}

@Composable
private fun ViewerArrow(icon: Ic, onClick: () -> Unit) {
    TouchTarget {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0x66FFFFFF))
                .tap(onClick),
            contentAlignment = Alignment.Center
        ) { Glyph(icon, Color.White, 18.dp) }
    }
}

/** Aksi kamera/galeri yang dipakai ikon di dalam field Note (V1.4 #7). */
data class PhotoActions(val camera: () -> Unit, val gallery: () -> Unit)

@Composable
fun rememberPhotoActions(onAdd: (String) -> Unit): PhotoActions {
    val ctx = LocalContext.current
    val cameraFile = remember { newTempFile(ctx) }
    val cameraUri = remember { photoUri(ctx, cameraFile) }
    val add = rememberUpdatedState(onAdd)

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            storeImage(ctx, cameraFile)?.let { add.value(it) }
            cameraFile.delete()
        }
    }
    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val tmp = newTempFile(ctx)
            if (copyUriToFile(ctx, uri, tmp)) {
                storeImage(ctx, tmp)?.let { add.value(it) }
            }
            tmp.delete()
        }
    }

    return remember {
        PhotoActions(
            camera = { camera.launch(cameraUri) },
            gallery = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }
}

/** Dua ikon kecil di ujung field Note, menggantikan tombol Kamera/Galeri di bawah. */
@Composable
fun PhotoNoteIcons(t: Tok, actions: PhotoActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TouchTarget {
            Box(
                Modifier.fillMaxSize().semantics { contentDescription = "Ambil foto" }.tap(actions.camera),
                contentAlignment = Alignment.Center
            ) { Glyph(Ic.Camera, t.muted2, 17.dp) }
        }
        TouchTarget {
            Box(
                Modifier.fillMaxSize().semantics { contentDescription = "Pilih foto" }.tap(actions.gallery),
                contentAlignment = Alignment.Center
            ) { Glyph(Ic.Image, t.muted2, 17.dp) }
        }
    }
}

/** Deretan thumbnail lampiran, bisa digulir dan dihapus satu per satu. */
@Composable
fun PhotoStrip(t: Tok, paths: List<String>, onRemove: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        paths.forEach { path ->
            Box(Modifier.size(60.dp)) {
                PhotoThumb(path, t, 60.dp)
                Box(
                    Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(t.sheet)
                        .border(1.dp, t.hair, CircleShape)
                        .tap { onRemove(path) },
                    contentAlignment = Alignment.Center
                ) {
                    Glyph(Ic.Close, t.muted2, 11.dp, 1.4.dp)
                }
            }
        }
    }
}

