package dev.tsdroid.ui.component

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import coil.request.ImageResult
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object WallpaperCacheManager {
    private var cacheDir: File? = null
    private const val MAX_CACHE_SIZE_MB_DEFAULT = 100L

    fun init(context: Context) {
        cacheDir = File(context.cacheDir, "wallpaper_cache").also { it.mkdirs() }
    }

    fun getCacheDir(): File? = cacheDir

    fun getCachedFiles(): List<File> {
        return cacheDir?.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun getCachedFilesCount(): Int = getCachedFiles().size

    fun getCacheSizeBytes(): Long {
        return getCachedFiles().sumOf { it.length() }
    }

    fun getCacheSizeMB(): Float = getCacheSizeBytes() / (1024f * 1024f)

    fun getRandomCachedFile(): File? {
        val files = getCachedFiles()
        if (files.isEmpty()) return null
        return files.random()
    }

    suspend fun saveToCache(context: Context, imageUrl: String) = withContext(Dispatchers.IO) {
        val dir = cacheDir ?: return@withContext
        try {
            val fileName = imageUrl.hashCode().toString() + ".jpg"
            val file = File(dir, fileName)
            if (file.exists()) return@withContext

            val maxSizeBytes = SettingsCacheSizeHelper.getMaxCacheSize(context) * 1024L * 1024L
            if (getCacheSizeBytes() >= maxSizeBytes) return@withContext

            val conn = URL(imageUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            conn.disconnect()
        } catch (_: Exception) {
        }
    }

    fun clearCache() {
        cacheDir?.listFiles()?.forEach { it.delete() }
    }

    fun deleteFile(file: File) {
        if (file.parentFile == cacheDir) file.delete()
    }
}

object SettingsCacheSizeHelper {
    private const val KEY_MAX_CACHE_SIZE = "wallpaper_max_cache_size_mb"
    private const val MAX_CACHE_SIZE_DEFAULT = 100L

    fun getMaxCacheSize(context: Context): Long {
        val prefs = context.getSharedPreferences("wallpaper_cache_prefs", Context.MODE_PRIVATE)
        return prefs.getLong(KEY_MAX_CACHE_SIZE, MAX_CACHE_SIZE_DEFAULT)
    }

    fun setMaxCacheSize(context: Context, sizeMB: Long) {
        val prefs = context.getSharedPreferences("wallpaper_cache_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_MAX_CACHE_SIZE, sizeMB).apply()
    }
}
