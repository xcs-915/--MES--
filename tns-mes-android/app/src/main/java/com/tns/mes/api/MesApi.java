package com.tns.mes.api;

import com.tns.mes.model.LoginRequest;
import com.tns.mes.model.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * MES 后端 REST API 定义（Retrofit 接口）。
 *
 * <p>基地址：{@link ApiClient#BASE_URL}</p>
 *
 * <p>已实现接口：
 * <ul>
 *   <li>POST api/v1/auth/login —— 登录，返回 {token, user}</li>
 *   <li>POST api/v1/interfaces/definitions —— 按接口编码查询接口定义</li>
 *   <li>GET  api/v1/interfaces/definitions/{code} —— 按编码获取接口定义</li>
 * </ul>
 *
 * <p>上料校验接口待后端确定后补充（见 {@link #submitLoadingCheck} 注释）。</p>
 */
public interface MesApi {

    /**
     * 登录。
     *
     * @param request {username, password}
     * @return {token, user}
     */
    @POST("api/v1/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    /**
     * 按接口编码查询接口定义（POST 方式，body 传 interfaceCode）。
     *
     * @param request {interfaceCode: "..."}
     * @return 统一返回体，data 为接口定义
     */
    @POST("api/v1/interfaces/definitions")
    Call<ApiResponses.ApiResult<ApiResponses.InterfaceDefinition>> lookupInterface(
            @Body ApiResponses.InterfaceRequest request);

    /**
     * 按编码获取接口定义（GET 方式）。
     */
    @GET("api/v1/interfaces/definitions/{code}")
    Call<ApiResponses.ApiResult<ApiResponses.InterfaceDefinition>> getInterfaceByCode(
            @Path("code") String code);

    /**
     * 上料校验（占位）。
     *
     * <p>上料校验端点将在后端确定后配置：
     * <ol>
     *   <li>通过 {@link #lookupInterface(ApiResponses.InterfaceRequest)} 查询编码为
     *       {@code LOADING_CHECK} 的接口定义，获取真实 URL 与请求方法；</li>
     *   <li>按定义动态调用上料校验接口。</li>
     * </ol>
     * 在真实接口确定前，提交按钮会先执行本地校验并尝试接口定义查询。</p>
     */
    // @POST("api/v1/loading/check")
    // Call<ApiResponses.ApiResult<ApiResponses.LoadingCheckResult>> submitLoadingCheck(
    //         @Body ApiResponses.LoadingCheckRequest request);
}
