package com.scliang.core.base.result;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/12/11.
 */
public abstract class BaseResult {
    protected boolean mCache;
    // 接口返回Code
    protected int    code; // ！！ 这里的code如果需要修改名字，也要在DataConverter里修改相应的反射名称
    // 接口返回提示语
    protected String msg;  // ！！ 这里的msg如果需要修改名字，也要在DataConverter里修改相应的反射名称

    public void setCache(boolean cache) {
        mCache = cache;
    }

    public boolean isCache() {
        return mCache;
    }

    // 接口返回Code
    public int getCode() { // ！！ 这里的getCode如果需要修改名字，也要在Data里修改相应的反射名称
        return code;
    }

    // 接口返回提示语
    public String getMsg() { // ！！ 这里的getMsg如果需要修改名字，也要在Data里修改相应的反射名称
        return msg;
    }

    // 接口是否成功，具体错误码由子类具体决定
    public boolean isSuccess() { // ！！ 这里的isSuccess如果需要修改名字，也要在Data里修改相应的反射名称
        return true;
    }

    // 接口是否有错误，具体错误码由子类具体决定
    public boolean isError() { // ！！ 这里的isError如果需要修改名字，也要在Data里修改相应的反射名称
        return false;
    }
}
