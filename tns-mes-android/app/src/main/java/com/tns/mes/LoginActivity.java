package com.tns.mes;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tns.mes.api.ApiClient;
import com.tns.mes.api.MesApi;
import com.tns.mes.model.LoginRequest;
import com.tns.mes.model.LoginResponse;
import com.tns.mes.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 登录界面。
 *
 * <p>流程：
 * <ol>
 *   <li>若 SessionManager 中已有有效 Token，直接跳转到 {@link ScanningActivity}。</li>
 *   <li>否则展示用户名 / 密码表单，调用 POST /api/v1/auth/login。</li>
 *   <li>登录成功后保存 Token，跳转扫描界面。</li>
 * </ol>
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilUsername;
    private TextInputLayout tilPassword;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private View progressLogin;
    private View tvLoginStatus;

    private SessionManager session;
    private MesApi api;

    private Call<LoginResponse> loginCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        // 已登录则直接进入扫描界面
        if (session.isLoggedIn()) {
            startScanningActivity();
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // 绑定视图
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressLogin = findViewById(R.id.progressLogin);
        tvLoginStatus = findViewById(R.id.tvLoginStatus);

        api = ApiClient.getApi(this);

        // 密码框按"完成"键直接登录
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                attemptLogin();
                return true;
            }
            return false;
        });

        btnLogin.setOnClickListener(v -> attemptLogin());

        // 禁用返回键退出（避免误触），需要连按返回才退出由系统处理
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 直接结束 Activity（回到桌面）
                finish();
            }
        });
    }

    private void attemptLogin() {
        // 清除旧错误
        tilUsername.setError(null);
        tilPassword.setError(null);

        String username = getText(etUsername);
        String password = getText(etPassword);

        if (TextUtils.isEmpty(username)) {
            tilUsername.setError(getString(R.string.err_username_required));
            etUsername.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.err_password_required));
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        loginCall = api.login(new LoginRequest(username, password));
        loginCall.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@androidx.annotation.NonNull Call<LoginResponse> call,
                                   @androidx.annotation.NonNull Response<LoginResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccessful()) {
                    // 登录成功
                    session.saveAuth(response.body(), username);
                    startScanningActivity();
                    finish();
                } else {
                    String detail;
                    if (response.body() != null && response.body().getMsg() != null) {
                        detail = response.body().getMsg();
                    } else {
                        detail = "HTTP " + response.code();
                    }
                    showStatus(getString(R.string.err_login_failed, detail));
                }
            }

            @Override
            public void onFailure(@androidx.annotation.NonNull Call<LoginResponse> call,
                                  @androidx.annotation.NonNull Throwable t) {
                setLoading(false);
                if (call.isCanceled()) {
                    return;
                }
                showStatus(getString(R.string.err_login_failed, t.getMessage()));
            }
        });
    }

    private void startScanningActivity() {
        startActivity(new Intent(this, ScanningActivity.class));
    }

    private void setLoading(boolean loading) {
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        tvLoginStatus.setVisibility(View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? R.string.login_loading : R.string.action_login);
    }

    private void showStatus(String message) {
        tvLoginStatus.setVisibility(View.VISIBLE);
        if (tvLoginStatus instanceof android.widget.TextView) {
            ((android.widget.TextView) tvLoginStatus).setText(message);
        }
    }

    private static String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loginCall != null && !loginCall.isCanceled()) {
            loginCall.cancel();
        }
    }
}
