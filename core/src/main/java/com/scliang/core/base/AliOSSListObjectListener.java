package com.scliang.core.base;

import com.alibaba.sdk.android.oss.model.OSSObjectSummary;

import java.util.List;

/**
 * Score
 * Created by ShangChuanliang
 * on 2020/01/14.
 */
public interface AliOSSListObjectListener {
  void onAliOSSListObjects(List<OSSObjectSummary> objectSummaries);
}
