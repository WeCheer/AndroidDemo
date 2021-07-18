package com.wyc.androiddemo.utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 作者： wyc
 * <p>
 * 创建时间： 2020/10/20 17:13
 * <p>
 * 文件名字： com.wyc.androiddemo
 * <p>
 * 类的介绍：
 */
public class PathUtils {

    private static final String TAG = "PathUtils";

    /**
     * android7.0以上处理方法
     * 把公有目录的文件复制到私有目录，并返回私有目录的路径
     */
    private static String getFilePathForN(Context context, Uri uri) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
        try {
            if (returnCursor == null) {
                Log.w(TAG, "cursor is null");
                return null;
            }
            returnCursor.moveToFirst();
            String name = returnCursor.getString(returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            long size = returnCursor.getLong(returnCursor.getColumnIndex(OpenableColumns.SIZE));
            Log.d(TAG, "name = " + name);
            Log.d(TAG, "size = " + size);
            //创建私有目录
            File file = new File(context.getExternalCacheDir(), name);

            //通过Uri获取出入流
            inputStream = context.getContentResolver().openInputStream(uri);
            //通过私有目录文件创建输出流
            outputStream = new FileOutputStream(file);
            int read;
            int bufferSize = 2048;
            if (inputStream != null) {
                //读写操作
                final byte[] buffers = new byte[bufferSize];
                while ((read = inputStream.read(buffers)) != -1) {
                    outputStream.write(buffers, 0, read);
                }
                outputStream.flush();
                return file.getPath();
            }
        } catch (Exception e) {
            Log.e(TAG, "getFilePathForN", e);
        } finally {
            closeStream(returnCursor);
            closeStream(inputStream);
            closeStream(outputStream);
        }
        return null;
    }

    private static void closeStream(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "closeStream", e);
        }
    }

    /**
     * 全平台处理方法
     */
    @SuppressLint("ObsoleteSdkInt")
    public static String getRealPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        final boolean isN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

        if (uri == null) {
            throw new IllegalArgumentException("uri cannot be null");
        }
        if (isN) {
            return getFilePathForN(context, uri);
        }

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:", "");
                }
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * 获取此Uri的数据列的值。这对于MediaStore uri和其他基于文件的内容提供程序非常有用。
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (IllegalArgumentException e) {
            //do nothing
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
