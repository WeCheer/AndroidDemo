package com.wyc.androiddemo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import java.io.*


/**
 *作者： wyc
 * <p>
 * 创建时间： 2020/10/29 10:21
 * <p>
 * 文件名字： com.wyc.androiddemo
 * <p>
 * 类的介绍：
 */
object FileUtils {

    private const val TAG = "FileUtils"

    private fun closeStream(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Exception", e)
        }
    }

    @JvmStatic
    fun readBitmapFileByUri(context: Context, uri: Uri?): Bitmap? {
        if (uri == null) {
            return null
        }
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        var tagBitmap: Bitmap? = null
        try {
            parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (parcelFileDescriptor != null && parcelFileDescriptor.fileDescriptor != null) {
                val fileDescriptor = parcelFileDescriptor.fileDescriptor
                //转换uri为bitmap类型
                tagBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            }
        } catch (e: IOException) {
            Log.e(TAG, "readBitmapFileByUri", e)
        } finally {
            closeStream(parcelFileDescriptor)
        }
        return tagBitmap
    }

    /**
     * 保持Bitmap到本地
     *
     * @param bitmap
     * @param path AndroidQ只能是私有目录路径
     * @return
     */
    @JvmStatic
    fun saveBitmap(bitmap: Bitmap?, path: String?): Boolean {
        if (bitmap == null) {
            Log.w(TAG, "saveBitmap, bitmap is null!")
            return false
        }
        if (TextUtils.isEmpty(path)) {
            Log.w(TAG, "saveBitmap, path is null!")
            return false
        }
        var fos: FileOutputStream? = null
        val file = File(path!!)
        try {
            if (file.exists()) {
                file.delete()
            }
            if (!file.exists()) {
                file.createNewFile()
            }
            fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            return true
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Exception", e)
        } finally {
            closeStream(fos)
        }
        return false
    }

    /**
     * 保存FileProvider对应的文件
     *
     * @param context
     * @param uri
     * @param path AndroidQ之后只能是私有目录路径
     * @return
     */
    fun saveFile(context: Context, uri: Uri?, path: String?): Boolean {
        if (uri == null || path == null) {
            Log.w(TAG, "saveFile, bitmap is null!")
            return false
        }
        if (TextUtils.isEmpty(path)) {
            Log.w(TAG, "saveFile, path is null!")
            return false
        }
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        var fileOutputStream: FileOutputStream? = null
        var fileInputStream: FileInputStream? = null
        try {
            parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
            fileInputStream = FileInputStream(fileDescriptor)
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
            if (!file.exists()) {
                file.createNewFile()
            }
            fileOutputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var length: Int
            while (fileInputStream.read(buf).also { length = it } != -1) {
                fileOutputStream.write(buf, 0, length)
            }
            fileOutputStream.flush()
            return true
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "saveFile, Exception = $e")
        } finally {
            closeStream(parcelFileDescriptor)
            closeStream(fileInputStream)
            closeStream(fileOutputStream)
        }
        return false
    }

    @Throws(IOException::class)
    private fun readTextFromUri(context: Context, uri: Uri): String {
        var inputStream: InputStream? = null
        var reader: BufferedReader? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            return stringBuilder.toString()
        } catch (e: Exception) {
        } finally {
            closeStream(inputStream)
            closeStream(reader)
        }
        return ""
    }

    @JvmStatic
    fun saveFileToUri(context: Context, saveUri: Uri, source: InputStream) {
        context.contentResolver?.let { resolver ->
            resolver.openOutputStream(saveUri)?.use { outPut ->
                var read: Int
                val buffer = ByteArray(2014)
                while (source.read(buffer).also { read = it } != -1) {
                    outPut.write(buffer, 0, read)
                }
                outPut.flush()
            }
        }
    }
}