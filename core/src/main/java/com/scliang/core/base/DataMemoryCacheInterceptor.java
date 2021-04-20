package com.scliang.core.base;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/11/12.
 */
public class DataMemoryCacheInterceptor implements Interceptor {
    private static Map<String, Cache> sMemCache = new HashMap<>();
    private static long sTimeout;

    DataMemoryCacheInterceptor(long timeout) {
        sTimeout = timeout;
    }

    static Response getCacheResponse(Request request) {
        Cache cache = sMemCache == null ? null : sMemCache.get(Data.getRequestKey(request));
        return cache == null ? null : (cache.timeout() ? null : cache.getCacheResponse(request));
    }

    private static Cache setCacheResponse(Request request, Response response) {
        Cache newCache = new Cache();
        newCache.time = System.currentTimeMillis();
        newCache.response = response;
        ResponseBody responseBody = newCache.response.body();
        newCache.type = responseBody == null ? null : responseBody.contentType();
        try {
            if (responseBody == null) {
                newCache.body = "";
            } else {
                BufferedSource source = responseBody.source();
                if (source.isOpen()) {
                    source.request(Long.MAX_VALUE); // Buffer the entire body.
                    Buffer buffer = source.buffer();
                    newCache.body = buffer.clone().readString(StandardCharsets.UTF_8);
                } else {
                    newCache.body = "";
                }
            }
            sMemCache.put(Data.getRequestKey(request), newCache);
        } catch (Exception ignored) { }
        return newCache;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        // 检查是否有缓存
        Request request = chain.request();
        Cache cache = sMemCache.get(Data.getRequestKey(request));

        // 没有缓存或缓存过期，重新请求远端数据，并缓存下来
        if (cache == null || cache.timeout() || TextUtils.isEmpty(cache.body)) {
            Cache newCache = setCacheResponse(request, chain.proceed(chain.request()));
            return newCache.response;
        }

        return cache.getCacheResponse(request);
    }

    private static class Cache {
        public long time;
        public Response response;
        public MediaType type;
        public String body;

        public boolean timeout() {
            return System.currentTimeMillis() - time > sTimeout;
        }

        Response getCacheResponse(Request request) {
            ResponseBody resBody = ResponseBody.create(type, body);
            return response.newBuilder()
                    .addHeader("MEMCACHE", "MEMCACHE")
                    .request(request)
                    .body(resBody)
                    .build();
        }
    }
}
