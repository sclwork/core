package com.scliang.core.base;

public interface OnDownloadFileListener {
  void onDownloadFileStart(String targetFile);
  void onDownloadFileProgress(String targetFile, long totalSize, long downloadSize);
  void onDownloadFileCompleted(String targetFile);
  void onDownloadFileError(String targetFile, Exception e);
}
