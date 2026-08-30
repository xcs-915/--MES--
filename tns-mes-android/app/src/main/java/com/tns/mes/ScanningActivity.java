package com.tns.mes;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.tns.mes.api.ApiClient;
import com.tns.mes.api.ApiResponses;
import com.tns.mes.api.MesApi;
import com.tns.mes.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 上料扫描界面。
 *
 * <p>功能：
 * <ul>
 *   <li>扫描 / 输入工单编号（{@code etWorkOrder}）</li>
 *   <li>扫描 / 输入物料标签（{@code etMaterial}）</li>
 *   <li>确认上料：先做本地非空校验，再查询上料校验接口定义并展示状态</li>
 *   <li>顶部工具栏提供退出登录</li>
 * </ul>
 *
 * <p>说明：上料校验的真实接口地址待后端确定后，将通过接口定义查询动态获取；
 * 当前提交逻辑会调用 POST /api/v1/interfaces/definitions 查询编码 {@code LOADING_CHECK}
 * 的接口定义，并据此展示状态。</p>
 */
public class ScanningActivity extends AppCompatActivity {

    /** 上料校验接口在接口定义表中的编码 */
    private static final String INTERFACE_CODE_LOADING_CHECK = "LOADING_CHECK";

    private TextInputEditText etWorkOrder;
    private TextInputEditText etMaterial;
    private MaterialButton btnScanWorkOrder;
    private MaterialButton btnScanMaterial;
    private MaterialButton btnSubmit;
    private ImageView imgStatus;
    private TextView tvStatus;
    private ProgressBar progressSubmit;

    private SessionManager session;
    private MesApi api;

    private Call<ApiResponses.ApiResult<ApiResponses.InterfaceDefinition>> lookupCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        // 未登录则返回登录界面
        if (!session.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_scanning);

        // 绑定视图
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        etWorkOrder = findViewById(R.id.etWorkOrder);
        etMaterial = findViewById(R.id.etMaterial);
        btnScanWorkOrder = findViewById(R.id.btnScanWorkOrder);
        btnScanMaterial = findViewById(R.id.btnScanMaterial);
        btnSubmit = findViewById(R.id.btnSubmit);
        imgStatus = findViewById(R.id.imgStatus);
        tvStatus = findViewById(R.id.tvStatus);
        progressSubmit = findViewById(R.id.progressSubmit);

        // 工具栏副标题显示当前登录用户
        String user = session.getDisplayName();
        if (!TextUtils.isEmpty(user)) {
            toolbar.setSubtitle(getString(R.string.subtitle_scanning) + " · " + user);
        }
        toolbar.setNavigationOnClickListener(v -> {
            // 导航图标（退出登录图标）-> 退出
            confirmLogout();
        });
        // 右上角溢出菜单：退出登录
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                confirmLogout();
                return true;
            }
            return false;
        });

        api = ApiClient.getApi(this);

        // 扫描按钮：聚焦对应输入框并提示使用扫码枪
        btnScanWorkOrder.setOnClickListener(v -> focusForScan(etWorkOrder));
        btnScanMaterial.setOnClickListener(v -> focusForScan(etMaterial));

        // 工单输入完成 -> 跳转到物料输入
        etWorkOrder.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                etMaterial.requestFocus();
                return true;
            }
            return false;
        });
        // 物料输入完成 -> 直接提交
        etMaterial.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                submitLoading();
                return true;
            }
            return false;
        });

        // 回车键（扫码枪常见结束键）触发跳转/提交
        etWorkOrder.setOnKeyListener((v, keyCode, event) -> {
            if (isEnterKey(event, keyCode)) {
                etMaterial.requestFocus();
                return true;
            }
            return false;
        });
        etMaterial.setOnKeyListener((v, keyCode, event) -> {
            if (isEnterKey(event, keyCode)) {
                submitLoading();
                return true;
            }
            return false;
        });

        btnSubmit.setOnClickListener(v -> submitLoading());

        setStatus(Status.INFO, getString(R.string.status_ready));
    }

    /**
     * 扫描按钮点击：清空当前输入、聚焦、提示。
     */
    private void focusForScan(TextInputEditText target) {
        target.setText("");
        target.requestFocus();
        Toast.makeText(this, R.string.scan_hint, Toast.LENGTH_SHORT).show();
    }

    private static boolean isEnterKey(KeyEvent event, int keyCode) {
        return event != null && event.getAction() == KeyEvent.ACTION_DOWN
                && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_TAB);
    }

    /**
     * 确认上料。
     */
    private void submitLoading() {
        String workOrder = getText(etWorkOrder);
        String material = getText(etMaterial);

        if (TextUtils.isEmpty(workOrder)) {
            setStatus(Status.ERROR, getString(R.string.err_work_order_required));
            etWorkOrder.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(material)) {
            setStatus(Status.ERROR, getString(R.string.err_material_required));
            etMaterial.requestFocus();
            return;
        }

        setSubmitting(true);
        setStatus(Status.LOADING, getString(R.string.submit_loading));

        // 查询上料校验接口定义（真实校验端点待后端配置后在此调用）
        lookupCall = api.lookupInterface(
                new ApiResponses.InterfaceRequest(INTERFACE_CODE_LOADING_CHECK));
        lookupCall.enqueue(new Callback<ApiResponses.ApiResult<ApiResponses.InterfaceDefinition>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponses.ApiResult<ApiResponses.InterfaceDefinition>> call,
                                   @NonNull Response<ApiResponses.ApiResult<ApiResponses.InterfaceDefinition>> response) {
                setSubmitting(false);

                ApiResponses.ApiResult<ApiResponses.InterfaceDefinition> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccessful()
                        && body.getData() != null) {
                    ApiResponses.InterfaceDefinition def = body.getData();
                    String url = def.getInterfaceUrl();
                    if (TextUtils.isEmpty(url)) {
                        setStatus(Status.WARNING,
                                "上料校验接口已定义但未配置 URL，请联系管理员");
                    } else {
                        // TODO: 接口定义就绪后，按 def.getMethod() 与 url 动态调用上料校验
                        setStatus(Status.SUCCESS, getString(R.string.status_success)
                                + "（" + def.getInterfaceName() + "）");
                    }
                } else {
                    String msg = body != null && body.getMessage() != null
                            ? body.getMessage()
                            : ("HTTP " + response.code());
                    setStatus(Status.ERROR, getString(R.string.status_failed, msg));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponses.ApiResult<ApiResponses.InterfaceDefinition>> call,
                                  @NonNull Throwable t) {
                setSubmitting(false);
                if (call.isCanceled()) {
                    return;
                }
                setStatus(Status.ERROR, getString(R.string.status_failed, t.getMessage()));
            }
        });
    }

    private void setSubmitting(boolean submitting) {
        progressSubmit.setVisibility(submitting ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!submitting);
        btnSubmit.setText(submitting ? R.string.submit_loading : R.string.action_submit);
        etWorkOrder.setEnabled(!submitting);
        etMaterial.setEnabled(!submitting);
        btnScanWorkOrder.setEnabled(!submitting);
        btnScanMaterial.setEnabled(!submitting);
    }

    // ===== 状态显示 =====

    private enum Status { READY, INFO, LOADING, SUCCESS, WARNING, ERROR }

    private void setStatus(Status status, String message) {
        tvStatus.setText(message);
        int color;
        switch (status) {
            case SUCCESS:
                imgStatus.setImageResource(R.drawable.ic_check_circle);
                color = getColor(R.color.success);
                break;
            case ERROR:
                imgStatus.setImageResource(R.drawable.ic_check_circle);
                color = getColor(R.color.error);
                break;
            case WARNING:
                imgStatus.setImageResource(R.drawable.ic_check_circle);
                color = getColor(R.color.warning);
                break;
            case LOADING:
                imgStatus.setImageResource(R.drawable.ic_check_circle);
                color = getColor(R.color.purple_primary);
                break;
            case READY:
            case INFO:
            default:
                imgStatus.setImageResource(R.drawable.ic_check_circle);
                color = getColor(R.color.text_secondary);
                break;
        }
        imgStatus.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        tvStatus.setTextColor(color);
    }

    // ===== 退出登录 =====

    private void confirmLogout() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.action_logout)
                .setMessage("确定要退出登录吗？")
                .setPositiveButton(R.string.action_logout, (d, w) -> doLogout())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void doLogout() {
        if (lookupCall != null && !lookupCall.isCanceled()) {
            lookupCall.cancel();
        }
        session.logout();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @androidx.annotation.NonNull
    private static String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lookupCall != null && !lookupCall.isCanceled()) {
            lookupCall.cancel();
        }
    }
}
