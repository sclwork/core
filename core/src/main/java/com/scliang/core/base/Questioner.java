package com.scliang.core.base;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/11.
 */
public interface Questioner {

    String giveRequestId();
    boolean responseCallbackable();

    // 执行被ShotOff、Banned、Reject时的操作
    void onQuestionerError(int code, String msg);

    // 判断是否启用onQuestionerResponseSuccess方法
    boolean questionerResponsable();
    void onQuestionerResponseSuccess();

}
