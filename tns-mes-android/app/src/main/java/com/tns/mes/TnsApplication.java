package com.tns.mes;

import android.app.Application;
import android.util.Log;

import com.tns.mes.api.ApiClient;

/**
 * 应用入口，负责全局初始化。
 *
 * 在 AndroidManifest.xml 中通过 android:name=".TnsApplication" 注册。
 */
public class TnsApplication extends Application {

    private static final String TAG = "TnsApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "TNS-MES Application starting...");

        // 预热 Retrofit / OkHttp 客户端（在后台线程完成连接池初始化）
        ApiClient.warmUp(getApplicationContext());
    }
}
