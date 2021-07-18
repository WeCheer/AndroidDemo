package com.wyc.androiddemo.saf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.wyc.androiddemo.utils.Log


/**
 *作者： wyc
 * <p>
 * 创建时间： 2020/10/29 10:20
 * <p>
 * 文件名字： com.wyc.androiddemo
 * <p>
 * 类的介绍：
 */
object SAFUtils {

    private const val TAG = "SAFUtils"
    const val FILE_REQUEST_CODE = 0x11
    const val DIR_REQUEST_CODE = 0x12

    @JvmStatic
    fun queryFileFromDocumentUri(context: Context, external: Uri) {
        context.contentResolver.query(external, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val documentId = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DOCUMENT_ID))
                val mimeType = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
                val name = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                val size = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                Log.d(TAG, "-----------------------------------")
                Log.d(TAG, "documentId = $documentId")
                Log.d(TAG, "mimeType = $mimeType")
                Log.d(TAG, "name = $name")
                Log.d(TAG, "size = $size")
            }
        }
    }

    /**
     * 打开文档
     * 读取公共目录文件夹下的某个非媒体文件 公共目录 如 Download，DICM，Pictures等
     * @param mimeType 如 "application/pdf"
     * SAF API 调用后都是通过 onActivityResult来相应动作
     *
     * 在Activity或Fragment获取数据
     * override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
     *      super.onActivityResult(requestCode, resultCode, data)
     *      when (requestCode) {
     *          SENDER_REQUEST_CODE -> {
     *              if (resultCode == Activity.RESULT_OK) {
     *                  data?.data?.also { documentUri ->
     *                      val fileDescriptor =
     *                      contentResolver.openFileDescriptor(documentUri, "r") ?: return
     *                      // 可以使用PdfRenderer等类通过fileDescriptor读取pdf内容
     *                      Toast.makeText(this, "pdf读取成功", Toast.LENGTH_SHORT).show()
     *              }
     *          }
     *      }
     *  }
     */
    @JvmStatic
    @JvmOverloads
    fun openFile(activity: Activity, mimeType: String, uri: Uri? = null) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = mimeType
            // 我们需要使用ContentResolver.openFileDescriptor读取数据
            //ACTION_OPEN_DOCUMENT用于打开文件。
            addCategory(Intent.CATEGORY_OPENABLE)
            // 提供对用户选择的目录中的文件和子目录的读取访问权限
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            /**
             * 以下代码查找所有 PDF、ODT 和 TXT 文件：
             * intent.setType("#/#");  #替换成*
             *  intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
             *       application/pdf", // .pdf
             *       application/vnd.oasis.opendocument.text", // .odt
             *       text/plain" // .txt
             *   });
             */
            //为文件指定一个URI，该文件在加载时应显示在系统文件选择器中。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && uri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            }
        }
        activity.startActivityForResult(intent, FILE_REQUEST_CODE)
    }

    /**
     * 创建文档
     * 注：创建操作若重名的话不会覆盖原文档，会添加 (1) 最为后缀，如 document.pdf -> document(1).pdf
     * SAF API 调用后都是通过 onActivityResult来相应动作
     */
    @JvmStatic
    @JvmOverloads
    fun createFile(activity: Activity, mimeType: String, fileName: String, uri: Uri? = null) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
            //为应用创建文档之前在系统文件选择器中打开的目录指定URI。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && uri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            }
        }
        activity.startActivityForResult(intent, FILE_REQUEST_CODE)
    }

    /**
     * 获取目录永久访问权限
     */
    @JvmStatic
    fun requestDirAccess(context: Context, uri: Uri?) {
        uri?.apply {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.grantUriPermission(context.packageName, uri, takeFlags)
            // Check for the freshest data.
            context.contentResolver.takePersistableUriPermission(this, takeFlags)
        }
    }

    @JvmStatic
    fun releaseDirAccess(context: Context, uri: Uri?) {
        uri?.apply {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.revokeUriPermission(uri, takeFlags)
            // Check for the freshest data.
            context.contentResolver.releasePersistableUriPermission(this, takeFlags)
        }
    }

    /**
     * 用户选择目录后，可访问该目录下的所有内容
     * SAF API 调用后都是通过 onActivityResult来相应动作
     * Android 11 中无法访问 Downloads
     * 创建所选目录的DocumentFile，可以使用它进行文件操作
     * DocumentFile root = DocumentFile.fromTreeUri(this, uriTree);
     * 比如使用它创建文件夹
     * DocumentFile dir = root.createDirectory(”Test“);
     */
    @JvmStatic
    @JvmOverloads
    fun openDirectory(activity: Activity, uri: Uri? = null) {
        // 使用系统的文件选择器选择目录。
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            // 提供对用户选择的目录中的文件和子目录的读取访问权限
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            // 为加载时在系统文件选择器中打开的目录指定URI。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && uri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            }
        }
        activity.startActivityForResult(intent, DIR_REQUEST_CODE)
    }

    @JvmStatic
    fun deleteFile(context: Context, uri: Uri): Boolean {
        try {
            val resolver = context.contentResolver
            return DocumentsContract.deleteDocument(resolver, uri)
        } catch (e: Exception) {
            Log.e(TAG, "Exception", e)
        }
        return false
    }

}