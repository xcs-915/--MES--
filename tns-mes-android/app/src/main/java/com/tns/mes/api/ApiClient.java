package com.tns.mes.api;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tns.mes.util.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit2 客户端封装。
 *
 * <p>基地址指向 Android 模拟器中宿主机（即运行 MES 后端的电脑）：
 * {@code http://10.0.2.2:8080/tns-mes/} （10.0.2.2 是模拟器访问宿主 localhost 的别名）。</p>
 *
 * <p>特性：
 * <ul>
 *   <li>自动注入 Authorization: Bearer &lt;jwt&gt; 请求头（来自 {@link SessionManager}）</li>
 *   <li>DEBUG 构建输出 HTTP 请求/响应日志</li>
 *   <li>连接 / 读取 / 写入超时各 30 秒</li>
 * </ul>
 */
public final class ApiClient {

    private static final String TAG = "ApiClient";

    /** 模拟器访问宿主 localhost 的基地址 */
    public static final String BASE_URL = "http://10.0.2.2:8080/tns-mes/";

    private static final long TIMEOUT_SECONDS = 30L;

    private static volatile Retrofit retrofit = null;
    private static volatile MesApi mesApi = null;

    private ApiClient() {
        // 工具类，禁止实例化
    }

    /**
     * 获取 Retrofit 实例（懒加载、线程安全）。
     *
     * @param context 任意 Context，内部会取 ApplicationContext
     */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    retrofit = buildRetrofit(context.getApplicationContext());
                }
            }
        }
        return retrofit;
    }

    /**
     * 获取 MES API 接口实例。
     */
    public static MesApi getApi(Context context) {
        if (mesApi == null) {
            synchronized (ApiClient.class) {
                if (mesApi == null) {
                    mesApi = getClient(context).create(MesApi.class);
                }
            }
        }
        return mesApi;
    }

    /**
     * 在 Application.onCreate 中调用，提前初始化连接池。
     */
    public static void warmUp(Context context) {
        getApi(context);
    }

    private static Retrofit buildRetrofit(final Context appContext) {
        // 认证拦截器：为每个请求自动附加 JWT
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder()
                    .header("Accept", "application/json");

            SessionManager session = new SessionManager(appContext);
            String authHeader = session.getAuthHeader();
            if (authHeader != null) {
                builder.header("Authorization", authHeader);
            }

            return chain.proceed(builder.build());
        };

        // 日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message ->
                Log.d(TAG, message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
