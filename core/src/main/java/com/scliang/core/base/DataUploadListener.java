package com.scliang.core.base;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/12/8.
 */
public interface DataUploadListener {

    void onUploadStarted(String fileName);
    void onUploadProgress(String fileName, long currentSize, long totalSize);
    void onUploadCompleted(String fileName, String fileKey, String url);
    void onUploadError(String fileName, Exception e);

}
