package io.github.catplan.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import kotlin.math.max

/** 相册照片的文件存取：拷贝进应用私有目录、按需缩放解码、删除 */
object PhotoStore {

    private const val DIR = "album"

    /** 把相册选中的图片拷贝为应用私有文件，返回绝对路径；失败返回 null */
    fun copyToAlbum(context: Context, uri: Uri, catId: Long): String? = runCatching {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val file = File(dir, "cat_${catId}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        file.absolutePath
    }.getOrNull()

    fun delete(path: String): Boolean = runCatching { File(path).delete() }.getOrDefault(false)

    /** 缩放解码，避免大图 OOM */
    fun load(path: String, targetSize: Int = 512): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetSize) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()
}
