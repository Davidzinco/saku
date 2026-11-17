package id.saku.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Foto untuk catatan transaksi. Berkas disalin ke `filesDir/photos/` supaya path-nya
 * tetap valid setelah URI galeri kedaluwarsa. Tidak ada permission kamera: foto diambil
 * lewat intent TakePicture + FileProvider, dan galeri lewat Photo Picker (tanpa izin baca).
 */
fun photosDir(ctx: Context): File = File(ctx.filesDir, "photos").apply { mkdirs() }

fun newPhotoFile(ctx: Context): File =
    File(photosDir(ctx), "p_" + System.nanoTime().toString(36) + ".jpg")

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

/** Hanya hapus berkas di dalam folder foto milik aplikasi. */
fun deletePhoto(path: String) {
    if (path.isEmpty()) return
    val marker = File.separator + "photos" + File.separator
    if (!path.contains(marker)) return
    try { File(path).delete() } catch (e: Exception) { }
}

@Composable
fun PhotoThumb(path: String, t: Tok, size: androidx.compose.ui.unit.Dp) {
    val bitmap = remember(path) {
        if (path.isEmpty()) null else BitmapFactory.decodeFile(path)?.asImageBitmap()
    }
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(t.bg)
            .border(1.dp, t.hair, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap, "Foto catatan", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Glyph(Ic.Film, t.muted2, 18.dp)
        }
    }
}

/** Pratinjau lebar untuk sheet detail transaksi. */
@Composable
fun PhotoPreview(path: String, t: Tok) {
    if (path.isEmpty()) return
    val bitmap = remember(path) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    if (bitmap == null) return
    Image(
        bitmap,
        "Foto catatan",
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, t.hair, RoundedCornerShape(10.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun PhotoField(t: Tok, path: String, onChange: (String) -> Unit) {
    val ctx = LocalContext.current
    val cameraFile = remember { newPhotoFile(ctx) }
    val cameraUri = remember(cameraFile) { photoUri(ctx, cameraFile) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            deletePhoto(path)
            onChange(cameraFile.absolutePath)
        }
    }
    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val dest = newPhotoFile(ctx)
            if (copyUriToFile(ctx, uri, dest)) {
                deletePhoto(path)
                onChange(dest.absolutePath)
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text("Foto", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (path.isNotEmpty()) {
                PhotoThumb(path, t, 52.dp)
                Spacer(Modifier.size(8.dp))
                TouchTarget {
                    Box(
                        Modifier.fillMaxSize().tap {
                            deletePhoto(path)
                            onChange("")
                        },
                        contentAlignment = Alignment.Center
                    ) { Glyph(Ic.Trash, t.muted2, 16.dp) }
                }
                Spacer(Modifier.size(8.dp))
            }
            PhotoAction(t, "Kamera") { camera.launch(cameraUri) }
            Spacer(Modifier.size(8.dp))
            PhotoAction(
                t,
                "Galeri"
            ) {
                gallery.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
        if (path.isEmpty()) {
            Text(
                "Opsional. Lampirkan struk atau foto ke catatan.",
                Type.micro,
                t.muted2,
                Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun PhotoAction(t: Tok, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, t.hair, RoundedCornerShape(10.dp))
            .tap(onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, Type.label.copy(fontWeight = FontWeight.SemiBold), t.ink)
    }
}
