package com.wyc.androiddemo.media

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.wyc.androiddemo.utils.Log
import java.io.*


/**
 *作者： wyc
 * <p>
 * 创建时间： 2020/10/21 11:30
 * <p>
 * 文件名字： com.wyc.androiddemo
 * <p>
 * 类的介绍：
 */
object MediaUtils {

    /**
     *  MimeType(*)             对应文件夹
     *
     * 图片(image/\*)	        DCIM, Pictures
     * 音频(audio/\*)	        Alarms, Music, Notifications, Podcasts, Ringtones
     * 视频(video/\*)	        Movies
     * 文档(file/\*)	        Documents,Download
     */
    enum class MediaDir {
        //图片
        DCIM, PICTURES,

        //音频
        ALARMS, MUSIC, NOTIFICATIONS, RINGTONES, PODCASTS,

        //视频
        MOVIES,

        //文档
        DOCUMENTS,
        DOWNLOAD,
    }

    private const val TAG = "MediaUtils"

    @JvmStatic
    fun externalMode(): String {
        val isHasPermission = Environment.isExternalStorageLegacy()
        return if (isHasPermission) {
            // 兼容模式
            "Legacy-View"
        } else {
            // 最新的存储模式
            "filtered-View"
        }
    }


    private fun getSuffixByDisplayName(mediaDir: MediaDir, displayName: String): String {
        return if (displayName.contains(".")) {
            displayName.substring(displayName.lastIndexOf(".") + 1)
        } else {
            getMimeType(mediaDir)
        }
    }

    /**
     * @param context
     * @param mediaDir See [MediaDir]
     * @param bitmap
     * @param displayName
     * @param relativePath 公共目录下的文件路径，不包括公共目录名字
     * @return uri
     */
    @JvmStatic
    @JvmOverloads
    fun saveBitmapToPublic(context: Context, mediaDir: MediaDir, bitmap: Bitmap?, displayName: String?, relativePath: String = ""): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        if (bitmap == null) {
            throw IllegalArgumentException("bitmap is null")
        }
        val suffix = if (TextUtils.isEmpty(displayName)) {
            null
        } else {
            getSuffixByDisplayName(mediaDir, displayName!!)
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/${suffix ?: "jpg"}")
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName
                    ?: "${System.currentTimeMillis()}.${suffix ?: "jpg"}")
            put(MediaStore.Images.Media.RELATIVE_PATH, getRelativePath(mediaDir, relativePath))
        }
        val insertUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        insertUri?.let {
            context.contentResolver.openOutputStream(it).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }
        return insertUri
    }

    /**
     * 获取 insert 方法中的第一个入参的方式
     * Images
     * MediaStore.Images.Media.EXTERNAL_CONTENT_URI
     * Audio
     * MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
     * Video
     * MediaStore.Video.Media.EXTERNAL_CONTENT_URI
     * Download
     * MediaStore.Downloads.EXTERNAL_CONTENT_URI
     * Documents 稍微有些特殊，需要通过 Files 获取
     * MediaStore.Files.getContentUri("external")
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getMediaUriByDir(mediaDir: MediaDir): Uri {
        return when (mediaDir) {
            MediaDir.DOWNLOAD -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            MediaDir.DCIM, MediaDir.PICTURES -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaDir.MOVIES -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaDir.ALARMS, MediaDir.RINGTONES, MediaDir.MUSIC,
            MediaDir.NOTIFICATIONS, MediaDir.PODCASTS -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            MediaDir.DOCUMENTS -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }
    }


    /**
     * 查询公共目录所有文件uri
     * uri	Uri	提供检索内容的 Uri，其 scheme 是content://
     * projection	    String[]	返回的列，如果传递 null 则所有列都返回(效率低下)
     * selection	    String	    过滤条件，即 SQL 中的 WHERE 语句(但不需要写 where 本身)，如果传 null 则返回所有的数据
     * selectionArgs	String[]	如果你在 selection 的参数加了 ? 则会被本字段中的数据按顺序替换掉
     * sortOrder	    String	    用来对数据进行排序，即 SQL 语句中的 ORDER BY(单不需要写ORDER BY 本身)，如果传 null 则按照默认顺序排序(可能是无序的)
     *
     * @param context
     * @param mediaDir See[MediaDir]
     * @param relativePath
     * @param sortOrder
     */
    @JvmStatic
    @JvmOverloads
    fun queryAllFileFromMedia(context: Context, mediaDir: MediaDir, relativePath: String? = "", sortOrder: String? = "${MediaStore.MediaColumns._ID} DESC"): MutableList<MediaInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        val external = getMediaUriByDir(mediaDir)
        val mediaInfoList = mutableListOf<MediaInfo>()
        context.contentResolver.query(external, null, "${MediaStore.MediaColumns.RELATIVE_PATH} like ?",
                arrayOf("%${getRelativePath(mediaDir, relativePath)}%"), sortOrder)?.use {
            while (it.moveToNext()) {
                //主键。图片 id，从 1 开始自增
                val index = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                //图片绝对路径
                val filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                //不带扩展名的文件名
                val title = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE))
                //类似于 image/jpeg 的 MIME 类型
                val mimeType = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
                //文件名
                val name = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                //文件大小，单位为 byte
                val size = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                //直接包含图片的文件夹就是该图片的 bucket，就是文件夹名
                val dirName = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME))
                //取自 EXIF 照片拍摄时间，若为空则等于文件修改时间，单位毫秒
                val dateTaken = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN))
                //文件最后修改时间，单位秒
                val dateModified = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
                val duration = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION))
                //添加到数据库的时间，单位秒
                val dataAdd = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))
                Log.d(TAG, "-----------------------------------")
                Log.d(TAG, "_id = $index")
                Log.d(TAG, "title = $title")
                Log.d(TAG, "filePath = $filePath")
                Log.d(TAG, "mimeType = $mimeType")
                Log.d(TAG, "name = $name")
                Log.d(TAG, "size = $size")
                Log.d(TAG, "dirName = $dirName")
                Log.d(TAG, "dateModified = ${formatFileDate(context, dateModified * 1000L)}")
                Log.d(TAG, "dataAdd = ${formatFileDate(context, dataAdd * 1000L)}")
                Log.d(TAG, "duration = $duration")
                val uri = ContentUris.withAppendedId(external, index)
                Log.d(TAG, "uri = $uri")
                val mediaInfo = MediaInfo(index, title, filePath, mimeType, name, size, dirName, dateModified, duration, uri)
                mediaInfoList.add(mediaInfo)
            }
        }
        return mediaInfoList
    }

    private fun formatFileDate(context: Context, millis: Long): String {
        return DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE) +
                "·" + DateUtils.formatDateTime(context, millis * 1000L, DateUtils.FORMAT_SHOW_TIME)
    }

    /**
     * 通过DisplayName查询文件对应的Uri
     * @param context
     * @param mediaDir See [MediaDir]
     * @param displayName 文件名字
     * @param relativePath 公共目录下的文件路径，不包括公共目录名字 如 (Environment.DIRECTORY_DCIM + File.separator +) "xxx"  xxx 为子项文件夹名
     */
    @JvmStatic
    @JvmOverloads
    fun queryUriByDisplayName(context: Context, mediaDir: MediaDir, displayName: String, relativePath: String? = ""): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        if (TextUtils.isEmpty(displayName)) {
            throw IllegalArgumentException("displayName cannot be null")
        }
        val external = getMediaUriByDir(mediaDir)
        context.contentResolver.query(external,
                arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.RELATIVE_PATH),
                "${MediaStore.MediaColumns.DISPLAY_NAME}=? and ${MediaStore.MediaColumns.RELATIVE_PATH} like ?",
                arrayOf(displayName, "%${getRelativePath(mediaDir, relativePath)}%"),
                "${MediaStore.MediaColumns._ID} DESC")?.use {
            if (it.moveToNext()) {
                val queryUri = ContentUris.withAppendedId(external, it.getLong(0))
                val childDirName = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH))
                Log.d(TAG, "queryUriByDisplayName success，uri = $queryUri，dirName = $childDirName")
                return queryUri
            }
        }
        return null
    }

    /**
     * 通过DisplayName查询文件真实路径
     * @param context
     * @param mediaDir See [MediaDir]
     * @param displayName 文件名字
     * @param relativePath 公共目录下的文件路径，不包括公共目录名字 如 (Environment.DIRECTORY_DCIM + File.separator +) "xxx"  xxx 为子项文件夹名
     */
    @JvmStatic
    @JvmOverloads
    fun queryRealPathByDisplayName(context: Context, mediaDir: MediaDir, displayName: String?, relativePath: String = ""): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        if (TextUtils.isEmpty(displayName)) {
            throw IllegalArgumentException("displayName cannot be empty")
        }
        val external = getMediaUriByDir(mediaDir)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? and ${MediaStore.MediaColumns.RELATIVE_PATH} like ?"
        val args = arrayOf(displayName, "%$relativePath%")
        val projection = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.RELATIVE_PATH)
        context.contentResolver.query(external, projection, selection, args, null)?.use {
            if (it.moveToNext()) {
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                Log.d(TAG, "queryRealPathByDisplayName success，path= $path")
                return path
            }
        }
        return null
    }


    /**
     * @param context
     * @param displayName
     * @param mediaDir See [MediaDir]
     * @param relativePath 公共目录下的文件路径，不包括公共目录名字
     */
    @JvmStatic
    @JvmOverloads
    fun createTempFile(context: Context, mediaDir: MediaDir, displayName: String, relativePath: String? = ""): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        try {
            val receiver = context.contentResolver
            val uri = insertFileIntoMediaStore(context, mediaDir, displayName, relativePath)
            uri?.let {
                receiver.openFileDescriptor(it, "w", null)
            }
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "createTempFile", e)
        }
        return null
    }


    /**
     * 判断Uri是否存在
     * @param context
     * @param uri
     */
    @JvmStatic
    fun isContentUriExists(context: Context, uri: Uri?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        if (uri == null) {
            return false
        }
        val resolver = context.contentResolver
        var afd: AssetFileDescriptor? = null
        return try {
            afd = resolver.openAssetFileDescriptor(uri, "r")
            null != afd
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "isContentUriExists Exception", e)
            false
        } finally {
            closeStream(afd)
        }

    }

    /**
     * 公共目录文件复制到私有目录
     * @param context
     * @param uri  公有目录uri或其他uri
     * @param path 私有目录文件
     */
    @JvmStatic
    fun copyFileToPrivate(context: Context, uri: Uri?, path: String?): Boolean {
        if (TextUtils.isEmpty(path)) {
            throw IllegalArgumentException("path is null")
        }
        return copyFileToPrivate(context, uri, File(path!!))
    }

    /**
     * 公共目录文件复制到私有目录
     * @param context
     * @param uri  公有目录uri或其他uri
     * @param file 私有目录文件
     */
    @JvmStatic
    fun copyFileToPrivate(context: Context, uri: Uri?, file: File?): Boolean {
        if (uri == null || !isContentUriExists(context, uri)) {
            throw IllegalArgumentException("uri is null or does not exist")
        }
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return false
        val dirFile = file?.parentFile
        if (dirFile != null && !dirFile.exists()) {
            dirFile.mkdirs()
        }
        val inStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
        var outStream: FileOutputStream? = null
        try {
            outStream = FileOutputStream(file)
            return copyFile(inStream, outStream)
        } catch (e: Exception) {
            Log.e(TAG, "copyFileToPrivate Exception", e)
        } finally {
            closeStream(inStream)
            closeStream(outStream)
        }
        return false
    }


    @Throws(IOException::class)
    private fun copyFile(inStream: InputStream, outStream: OutputStream): Boolean {
        return try {
            val buffer = ByteArray(4096)
            var byteCount: Int
            while (inStream.read(buffer).also { byteCount = it } != -1) { // 循环从输入流读取 buffer字节
                outStream.write(buffer, 0, byteCount) // 将读取的输入流写入到输出流
            }
            outStream.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "copyFile", e)
            false
        }
    }

    /**
     * 向公有目录保存文件
     * @param context
     * @param mediaDir See [MediaDir]
     * @param file  AndroidQ只能是私有目录的文件
     * @param relativePath 公共目录下的文件路径，不包括公共目录名字
     * @return uri
     */
    @JvmStatic
    @JvmOverloads
    fun saveFileToPublic(context: Context, mediaDir: MediaDir, file: File?, relativePath: String? = ""): Uri? {
        if (file == null || !file.exists()) {
            throw IllegalArgumentException("file is null or file does not exist")
        }
        return saveFileToPublic(context, mediaDir, FileInputStream(file), file.name, relativePath)
    }

    /**
     * 向公有目录保存文件
     * @param context
     * @param mediaDir See [MediaDir]
     * @param inputStream
     * @param displayName
     * @param relativePath 公共目录下的文件路径，不包括公共目录名字
     */
    @JvmStatic
    @JvmOverloads
    fun saveFileToPublic(context: Context, mediaDir: MediaDir, inputStream: InputStream?, displayName: String, relativePath: String? = ""): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        if (inputStream == null || inputStream.available() == 0) {
            throw IllegalArgumentException("inputStream is null or inputStream size 0")
        }
        val uri: Uri = insertFileIntoMediaStore(context, mediaDir, displayName, relativePath)
                ?: return null
        return copyFileToPublic(context, inputStream, uri)
    }


    //创建视频或图片的URI
    /**
     * values 样例图片
     * val values = ContentValues().apply {
     *      put(MediaStore.Images.Media.DESCRIPTION, "")
     *      put(MediaStore.Images.Media.RELATIVE_PATH, "")
     *      put(MediaStore.Images.Media.IS_PENDING, 1);
     *      put(MediaStore.Images.Media.TITLE, fileName);
     *      put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
     *      put(MediaStore.Images.Media.DATE_TAKEN, dateTaken);
     *      put(MediaStore.Images.Media.DATE_MODIFIED, dateModified / 1000L);
     *      put(MediaStore.Images.Media.MIME_TYPE, type);
     *      put(MediaStore.Images.Media.ORIENTATION, rotation);
     *      put(MediaStore.Images.Media.DATA, filePath);
     *      put(MediaStore.Images.Media.LATITUDE, latitude);
     *      put(MediaStore.Images.Media.LONGITUDE, longitude);
     *}
     * AndroidQ上，MediaStore中添加MediaStore.Images.Media.IS_PENDING flag，用来表示文件的Pending状态，0是可见，其他不可见，
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertFileIntoMediaStore(context: Context, mediaDir: MediaDir, displayName: String, relativePath: String?): Uri? {
        val suffix = getSuffixByDisplayName(mediaDir, displayName)
        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, displayName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, MimeUtils.getMimeTypeBySuffix(suffix))
            put(MediaStore.Files.FileColumns.IS_PENDING, 0)
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, getRelativePath(mediaDir, relativePath))
        }
        var uri: Uri? = null
        try {
            uri = context.contentResolver.insert(getMediaUriByDir(mediaDir), values)
        } catch (e: Exception) {
            Log.e(TAG, "insertFileIntoMediaStore", e)
        }
        return uri
    }


    /**
     * 根据类型和后缀匹配类型 默认后缀
     * @param mediaDir See[MediaDir]
     */
    private fun getMimeType(mediaDir: MediaDir): String {
        return when (mediaDir) {
            MediaDir.DCIM, MediaDir.PICTURES -> "image/jpg"
            MediaDir.MOVIES -> "video/mp4"
            MediaDir.DOWNLOAD -> "*/*"
            MediaDir.DOCUMENTS -> "*/*"
            MediaDir.ALARMS, MediaDir.RINGTONES, MediaDir.MUSIC,
            MediaDir.NOTIFICATIONS, MediaDir.PODCASTS -> "audio/mp3"
        }
    }

    /**
     * 通过type字段获取对应的相对路径
     * @param mediaDir 类型文件夹  See [MediaDir]
     * @param relativePath 公共目录下的文件路径，不包括公共目录名字
     */
    @JvmStatic
    @JvmOverloads
    fun getRelativePath(mediaDir: MediaDir, relativePath: String? = ""): String {
        return when (mediaDir) {
            MediaDir.DCIM -> {
                if (TextUtils.isEmpty(relativePath)) {
                    Environment.DIRECTORY_DCIM
                } else {
                    Environment.DIRECTORY_DCIM + File.separator + relativePath
                }
            }
            MediaDir.PICTURES -> {
                if (TextUtils.isEmpty(relativePath)) {
                    Environment.DIRECTORY_PICTURES
                } else {
                    Environment.DIRECTORY_PICTURES + File.separator + relativePath
                }
            }
            MediaDir.MOVIES -> {
                if (TextUtils.isEmpty(relativePath)) {
                    Environment.DIRECTORY_MOVIES
                } else {
                    Environment.DIRECTORY_MOVIES + File.separator + relativePath
                }
            }
            MediaDir.DOWNLOAD -> {
                if (TextUtils.isEmpty(relativePath)) {
                    Environment.DIRECTORY_DOWNLOADS
                } else {
                    Environment.DIRECTORY_DOWNLOADS + File.separator + relativePath
                }
            }
            MediaDir.MUSIC -> {
                if (TextUtils.isEmpty(relativePath)) {
                    Environment.DIRECTORY_MUSIC
                } else {
                    Environment.DIRECTORY_MUSIC + File.separator + relativePath
                }
            }
            MediaDir.ALARMS -> {
                if (TextUtils.isEmpty(relativePath)) {
                    Environment.DIRECTORY_ALARMS
                } else {
                    Environment.DIRECTORY_ALARMS + File.separator + relativePath
                }
            }
            MediaDir.RINGTONES -> {
                if (TextUtils.isEmpty(relativePath)) {
                    Environment.DIRECTORY_RINGTONES
                } else {
                    Environment.DIRECTORY_RINGTONES + File.separator + relativePath
                }
            }
            MediaDir.NOTIFICATIONS -> {
                if (TextUtils.isEmpty(relativePath)) {
                    Environment.DIRECTORY_NOTIFICATIONS
                } else {
                    Environment.DIRECTORY_NOTIFICATIONS + File.separator + relativePath
                }
            }
            MediaDir.PODCASTS -> {
                if (TextUtils.isEmpty(relativePath)) {
                    Environment.DIRECTORY_PODCASTS
                } else {
                    Environment.DIRECTORY_PODCASTS + File.separator + relativePath
                }
            }
            MediaDir.DOCUMENTS -> {
                if (TextUtils.isEmpty(relativePath)) {
                    Environment.DIRECTORY_DOCUMENTS
                } else {
                    Environment.DIRECTORY_DOCUMENTS + File.separator + relativePath
                }
            }
        }
    }

    /**
     *  另外一种方式生成outputStream
     *  val parcelFileDescriptor = resolver.openFileDescriptor(insertUri, "w")
     *  if (parcelFileDescriptor == null) {
     *      Log.w(TAG, "parcelFileDescriptor is null")
     *      return null
     *  }
     * val outputStream = FileOutputStream(parcelFileDescriptor.fileDescriptor)
     */
    private fun copyFileToPublic(context: Context, inputStream: InputStream, externalUri: Uri): Uri? {
        val resolver = context.contentResolver
        var outStream: OutputStream? = null
        try {
            outStream = resolver.openOutputStream(externalUri)
            if (outStream != null) {
                copyFile(inputStream, outStream)
                Log.d(TAG, "copyFileToPublic success")
                return externalUri
            } else {
                Log.w(TAG, "outStream is null")
            }
        } catch (e: IOException) {
            Log.e(TAG, "copyFileToPublic Exception", e)
        } finally {
            closeStream(inputStream)
            closeStream(outStream)
        }
        return null
    }

    /**
     * 在应用的清单中请求 ACCESS_MEDIA_LOCATION 权限。
     * @param context
     * @param uri
     */
    @JvmStatic
    fun getImageExif(context: Context, uri: Uri?): PhotoInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        if (context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "no ACCESS_MEDIA_LOCATION permission")
            throw Exception("no ACCESS_MEDIA_LOCATION permission")
        }
        if (uri == null || !isContentUriExists(context, uri)) {
            throw IllegalArgumentException("uri is null or does not exist")
        }

        // Get location data using the Exifinterface library.
        // Exception occurs if ACCESS_MEDIA_LOCATION permission isn't granted.
        val photoUri = MediaStore.setRequireOriginal(uri)
        context.contentResolver.openInputStream(photoUri)?.use { stream ->
            ExifInterface(stream).run {

                /**
                 * TAG_APERTURE(TAG_F_NUMBER)：光圈值。
                 * TAG_DATETIME：拍摄时间，取决于设备设置的时间。
                 * TAG_EXPOSURE_TIME：曝光时间。
                 * TAG_FLASH：闪光灯。
                 * TAG_FOCAL_LENGTH：焦距。
                 * TAG_IMAGE_LENGTH：图片高度。
                 * TAG_IMAGE_WIDTH：图片宽度。
                 * TAG_ISO：ISO。
                 * TAG_MAKE：设备品牌。
                 * TAG_MODEL：设备型号，整形表示，在ExifInterface中有常量对应表示。
                 * TAG_ORIENTATION：旋转角度，整形表示，在ExifInterface中有常量对应表示。
                 * TAG_WHITE_BALANCE：白平衡
                 */

                val fNumber = getAttribute(ExifInterface.TAG_F_NUMBER)
                val dateTime = getAttribute(ExifInterface.TAG_DATETIME)
                val exposureTime = getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                val flash = getAttribute(ExifInterface.TAG_FLASH)
                val focalLength = getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                val imageLength = getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
                val imageWidth = getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
                val iSOSpeedRatings = getAttribute(ExifInterface.TAG_ISO)
                val make = getAttribute(ExifInterface.TAG_MAKE)
                val model = getAttribute(ExifInterface.TAG_MODEL)
                val orientation = getAttribute(ExifInterface.TAG_ORIENTATION)
                val whiteBalance = getAttribute(ExifInterface.TAG_WHITE_BALANCE)

                // If lat/long is null, fall back to the coordinates (0, 0).
                //照片Exif信息中存储的都是WGS84坐标的经纬度，如果要使用百度的SDK通过经纬度获取位置描述信息，就需要转换成BD09坐标系。
                val floatArrayOf = floatArrayOf(0f, 0f)
                val latLongResult = this.getLatLong(floatArrayOf)
                Log.d(TAG, "latLong request $latLongResult latlng = ${floatArrayOf.toList()}")
                return PhotoInfo(fNumber, dateTime, exposureTime, flash, focalLength, imageLength, imageWidth,
                        iSOSpeedRatings, make, model, orientation, whiteBalance, floatArrayOf[0], floatArrayOf[1])
            }
        }
        return PhotoInfo()
    }

    /**
     * 在应用的清单中请求 ACCESS_MEDIA_LOCATION 权限。
     * @param context
     * @param mediaDir See [MediaDir]
     * @param displayName
     * @param relativePath
     */
    @JvmStatic
    @JvmOverloads
    fun getImageExif(context: Context, mediaDir: MediaDir, displayName: String, relativePath: String? = ""): PhotoInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        if (context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "no ACCESS_MEDIA_LOCATION permission")
            throw Exception("no ACCESS_MEDIA_LOCATION permission")
        }
        val uri = queryUriByDisplayName(context, mediaDir, displayName, relativePath)
        return getImageExif(context, uri)
    }

    /**
     * 关闭流文件
     */
    private fun closeStream(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Exception", e)
        }
    }


    /**
     * 修改文件是否能被其他应用看到
     * @param context
     * @param uri
     * @param state 0 可见  1 不可见
     */
    @JvmStatic
    fun updatePending(context: Context, uri: Uri?, state: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        if (uri == null || !isContentUriExists(context, uri)) {
            throw IllegalArgumentException("uri is null or does not exist")
        }
        var flag = -1
        try {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.IS_PENDING, state)
            flag = context.contentResolver.update(uri, values, null, null)
            //1 代表操作成功
            Log.d(TAG, "updatePending flag = $flag")
        } catch (e: Exception) {
            Log.e(TAG, "updatePending Exception = ", e)
        }
        return flag == 1
    }

    /**
     * 通过uri删除文件
     * @param context
     * @param uri
     * @param senderRequestCode
     */
    @JvmStatic
    fun deleteFile(context: Context, uri: Uri?, senderRequestCode: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "no READ_EXTERNAL_STORAGE permission")
            throw Exception("no READ_EXTERNAL_STORAGE permission")
        }
        if (uri == null || !isContentUriExists(context, uri)) {
            throw IllegalArgumentException("uri is null or does not exist")
        }
        var flag = -1
        try {
            flag = context.contentResolver.delete(uri, null, null)
            Log.d(TAG, "deleteFile flag = $flag")
        } catch (e: IOException) {
            Log.e(TAG, "deleteFile IOException", e)
        } catch (e1: RecoverableSecurityException) {
            Log.e(TAG, "deleteFile RecoverableSecurityException", e1)
            try {
                //在Android 10+的系统中，会抛出RecoverableSecurityException，捕获到这个异常后，
                // 从异常中获得了IntentSender，并使用它来向用户索取该uri的修改、删除权限
                startIntentSenderForResult(context as Activity,
                        e1.userAction.actionIntent.intentSender,
                        senderRequestCode,
                        null,
                        0,
                        0,
                        0,
                        null)
            } catch (e2: IntentSender.SendIntentException) {
                Log.e(TAG, "deleteFile SendIntentException", e2)
            }
        }
        return flag == 1
    }


    /**
     * 根据名字删除文件 需要在 Activity或Fragment中接收是否已有权限，方可进行删除操作，修改亦如此
     *  onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
     *  @param context
     *  @param mediaDir See [MediaDir]
     *  @param displayName
     *  @param relativePath
     */
    @JvmStatic
    @JvmOverloads
    fun deleteFile(context: Context, mediaDir: MediaDir, displayName: String, relativePath: String? = "", senderRequestCode: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        if (TextUtils.isEmpty(displayName)) {
            throw IllegalArgumentException("displayName cannot be null")
        }
        if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "no READ_EXTERNAL_STORAGE permission")
            throw Exception("no READ_EXTERNAL_STORAGE permission")
        }
        val queryUri = queryUriByDisplayName(context, mediaDir, displayName, relativePath)
        Log.d(TAG, "deleteFile uri = $queryUri")

        return deleteFile(context, queryUri, senderRequestCode)
    }

    /**
     * @param context
     * @param mediaDir See [MediaDir]
     * @param displayName
     * @param relativePath
     */
    @JvmStatic
    @JvmOverloads
    fun updateFile(context: Context, mediaDir: MediaDir, displayName: String, relativePath: String? = "", senderRequestCode: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        if (TextUtils.isEmpty(displayName)) {
            throw IllegalArgumentException("displayName cannot be null")
        }
        if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "no READ_EXTERNAL_STORAGE permission")
            throw Exception("no READ_EXTERNAL_STORAGE permission")
        }
        val queryUri = queryUriByDisplayName(context, mediaDir, displayName, relativePath)
        var outStream: OutputStream? = null
        try {
            queryUri?.let { uri ->
                outStream = context.contentResolver.openOutputStream(uri)
                //TODO("操作outStream")
            }

        } catch (e: IOException) {
            Log.e(TAG, "updateFile Exception", e)
        } catch (e1: RecoverableSecurityException) {
            Log.e(TAG, "updateFile RecoverableSecurityException", e1)
            try {
                startIntentSenderForResult(context as Activity,
                        e1.userAction.actionIntent.intentSender,
                        senderRequestCode,
                        null,
                        0,
                        0,
                        0,
                        null)
            } catch (e2: IntentSender.SendIntentException) {
                Log.e(TAG, "updateFile SendIntentException", e2)
            }
        }
    }

    /**
     * 根据媒体文件的ID来获取文件的Uri
     *
     * @param id
     * @param mediaDir See [MediaDir]
     * @return uri
     */
    @JvmStatic
    fun getMediaUriFromId(id: String, mediaDir: MediaDir): Uri {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        return getMediaUriByDir(mediaDir).buildUpon().appendPath(id).build()
    }

    /**
     *  透明背景的图片加载出来是黑色的。
     *  @param context
     *  @param uri
     *  @param width
     *  @param height
     */
    @JvmStatic
    @JvmOverloads
    fun loadThumbnailByUri(context: Context, uri: Uri?, width: Int = 100, height: Int = 100): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw Exception("version less than Android Q")
        }
        if (uri == null || !isContentUriExists(context, uri)) {
            throw IllegalArgumentException("uri is null or does not exist")
        }
        return try {
            context.contentResolver.loadThumbnail(uri, Size(width, height), null)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "loadThumbnailByUri", e)
            null
        }
    }

}