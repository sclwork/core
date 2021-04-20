package com.scliang.core.demo.api;

import com.scliang.core.demo.result.TestResult;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * SCore Demo
 * Created by ShangChuanliang
 * on 2017/10/11.
 */
public interface DemoApi {

    @GET("/holiday/test/")
    Call<TestResult> test(@Query("id") String id);

    /* **********************************
        登录
     * **********************************/
    @FormUrlEncoded
    @POST("/login")
    Call<TestResult> login
    (@Field("c") String c,
     @Field("b") String b,
     @Field("sign") String s);

}
