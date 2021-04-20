package com.scliang.core.base;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.scliang.core.base.result.BaseResult;
import com.scliang.core.base.result.BasicResult;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/12/25.
 */
public class DataConverter<T> implements Converter<ResponseBody, T> {
    private final Gson gson;
    private final Type type;

    DataConverter(Gson gson, Type type){
        this.gson = gson;
        this.type = type;
    }

    @Override
    public T convert(@NonNull ResponseBody value) throws IOException {
        try {
            String response = value.string();
            BasicResult basicResult = gson.fromJson(response, BasicResult.class);
            if (basicResult.isSuccess()) {
                try {
                    return gson.fromJson(response, type);
                } catch (JsonSyntaxException e) {
                    return basicToT(basicResult);
                }
            } else {
                return basicToT(basicResult);
            }
        } catch (Throwable e) {
            return null;
        }
    }

    private T basicToT(BaseResult base) {
        try {
            String name = type.toString().replaceAll("class ", "");
            Class clz = Class.forName(name);
            if (clz != null) {
                Object obj = clz.newInstance();
                Field fCode = getField(clz, "code");
                fCode.setAccessible(true);
                fCode.set(obj, base.getCode());
                Field fMsg = getField(clz, "msg");
                fMsg.setAccessible(true);
                fMsg.set(obj, base.getMsg());
                return (T) obj;
            } else {
                return null;
            }
        } catch (InstantiationException |
                IllegalAccessException |
                ClassNotFoundException e) {
            return null;
        }
    }

    private Field getField(Class clz, String name) {
        if (Object.class.getSimpleName().equals(clz.getSimpleName())) {
            return null;
        }
        try {
            return clz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return getField(clz.getSuperclass(), name);
        }
    }
}
