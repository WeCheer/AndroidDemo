package com.wyc.androiddemo.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream

/**
 *作者： wyc
 * <p>
 * 创建时间： 2021/1/12 11:32
 * <p>
 * 文件名字： com.wyc.androiddemo.media
 * <p>
 * 类的介绍：
 */
object DownloadUtils {

    private const val TAG = "DownloadUtils"

    /**
     * 下载安装文件apk
     * @param inputStream 文件下载输入流
     */
    @JvmStatic
    @JvmOverloads
    fun downloadApkAndInstall(context: Context, inputStream: InputStream, apkName: String, relativePath: String = "") {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        try {
            val bis = BufferedInputStream(inputStream)
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, apkName)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.also {
                val outputStream = context.contentResolver.openOutputStream(uri) ?: return
                val bos = BufferedOutputStream(outputStream)
                val buffer = ByteArray(1024)
                var bytes = bis.read(buffer)
                while (bytes >= 0) {
                    bos.write(buffer, 0, bytes)
                    bos.flush()
                    bytes = bis.read(buffer)
                }
                bos.close()
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    ///获取到的这个uri本来就是content://开头，所以不需要使用FileProvider
                    //需要主线程及UI线程安装apk
                    installAPK(context, uri)
                }
            }
            bis.close()
        } catch (e: Exception) {
            Log.e(TAG, "downloadApkAndInstall Exception", e)
        }
    }


    private fun installAPK(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        context.startActivity(intent)
    }

}