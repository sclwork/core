package com.scliang.core.base;

import com.google.gson.Gson;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/12/25.
 */
public class DataConverterFactory extends Converter.Factory {

    public static DataConverterFactory create() {
        return create(new Gson());
    }

    public static DataConverterFactory create(Gson gson) {
        return new DataConverterFactory(gson);
    }

    private final Gson gson;

    private DataConverterFactory(Gson gson) {
        if (gson == null) {
            throw new NullPointerException("Gson == null");
        }
        this.gson = gson;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(
            Type type, Annotation[] annotations, Retrofit retrofit) {
        return new DataConverter<>(gson, type);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(
            Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations,
            Retrofit retrofit) {
        return new DataConverter<>(gson, type);
    }
}
