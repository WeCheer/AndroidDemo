package com.wyc.androiddemo;

import android.app.Application;
import android.content.Context;

/**
 * 作者： wyc
 * <p>
 * 创建时间： 2020/10/28 11:29
 * <p>
 * 文件名字： com.wyc.androiddemo
 * <p>
 * 类的介绍：
 */
public class App extends Application {

    public static final String TAG = "App";
    private static App sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static App getInstance() {
        return sInstance;
    }

    public static Context getAppContext() {
        return getInstance().getApplicationContext();
    }
}
