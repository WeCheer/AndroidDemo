package com.wyc.androiddemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.wyc.androiddemo.media.MediaUtils;
import com.wyc.androiddemo.saf.SAFUtils;
import com.wyc.androiddemo.utils.Log;
import com.wyc.androiddemo.utils.PathUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private String[] mPermissionArray = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_MEDIA_LOCATION};

    private List<String> mPermissionDeniedList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean isPermissionGranted = checkPermission();
        if (!isPermissionGranted) {
            requestPermission();
            return;
        }
//        copyFileToPrivate();
//        copyFileToPublic();

//        deleteFileByUri();

        MediaUtils.queryAllFileFromMedia(this, MediaUtils.MediaDir.PICTURES);
//        Uri uri = MediaUtils.queryUriByDisplayName(this, MediaUtils.MediaDir.DCIM, "IMG_6935773213065151616.jpg");
//        Log.d(TAG, "uri = " + uri);
//        String realPath = MediaUtils.queryRealPathByDisplayName(this, MediaUtils.MediaDir.DCIM, "IMG_6935773213065151616.jpg");
//        Log.d(TAG, "real path  = " + realPath);
//        PhotoInfo info = MediaUtils.getImageExif(this, uri);
//        Log.d(TAG, "info = " + info.toString());

        findViewById(R.id.updateState).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                SAFUtils.openFile(MainActivity.this, "*/*");
//                SAFUtils.createFile(MainActivity.this, "image/png", "simple.png");
                SAFUtils.openDirectory(MainActivity.this);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = null;
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SAFUtils.FILE_REQUEST_CODE) {
                // 获取选择文件Uri
                uri = data != null ? data.getData() : null;
                Log.d(TAG, "real path = " + PathUtils.getRealPath(this, uri));
            } else if (requestCode == SAFUtils.DIR_REQUEST_CODE) {
                // 获取选择文件Uri
                uri = data != null ? data.getData() : null;
                SAFUtils.requestDirAccess(this, uri);
            }
            Log.d(TAG, "uri = " + uri);
            Toast.makeText(this, "uri = " + uri, Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFileToPrivate() {
        //复制公共目录图片到私有目录
        Uri uri = MediaUtils.queryUriByDisplayName(this, MediaUtils.MediaDir.DCIM, "Compress-Capture-Origin-image.jpg");
        Log.d(TAG, "uri = " + uri);
        uri = MediaUtils.queryUriByDisplayName(this, MediaUtils.MediaDir.DCIM, "Compress-Capture-Origin-image.jpg", "wyc");
        Log.d(TAG, "uri = " + uri);
        File dirFile = getExternalFilesDir(Environment.DIRECTORY_DCIM);
        File file = new File(dirFile, "Compress-Capture-Origin-image1.jpg");
        boolean isSuccess = MediaUtils.copyFileToPrivate(this, uri, file);
        if (isSuccess) {
            Toast.makeText(this, "已复制到" + file.getAbsolutePath() + "中", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFileToPublic() {
        File dirFile = getExternalFilesDir(Environment.DIRECTORY_DCIM);
        File file = new File(dirFile, "Compress-Capture-Origin-image.jpg");
        if (!file.exists()) {
            Log.d(TAG, "文件不存在");
            return;
        }
        MediaUtils.MediaDir mediaDir = MediaUtils.MediaDir.DCIM;
        String dirName = "wyc";
        Uri uri = MediaUtils.saveFileToPublic(this, mediaDir, file, "wyc");
        Log.d(TAG, "uri = " + uri);
        if (uri != null) {
            Toast.makeText(this, "图片以保存到" + MediaUtils.getRelativePath(mediaDir, dirName) + "中", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteFileByUri() {
        Uri uri = Uri.parse("content://media/external/images/media/38519");
        MediaUtils.deleteFile(this, uri, 10086);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, mPermissionDeniedList.toArray(new String[0]), 10010);
    }

    private boolean checkPermission() {
        mPermissionDeniedList.clear();
        for (String permission : mPermissionArray) {
            if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                continue;
            }
            mPermissionDeniedList.add(permission);
        }
        return mPermissionDeniedList.size() == 0;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10010) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "缺少权限，请先授予权限", Toast.LENGTH_SHORT).show();
                    Toast.makeText(this, permissions[i], Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(this, "已获得权限", Toast.LENGTH_SHORT).show();
//            copyFileToPublic();
//            copyFileToPrivate();
        }
    }

}
