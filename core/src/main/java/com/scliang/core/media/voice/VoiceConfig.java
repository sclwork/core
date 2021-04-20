package com.scliang.core.media.voice;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2019/4/22.
 */
public class VoiceConfig {

  /**
   * 科大讯飞语音听写配置
   * @param kdxfAppId 科大讯飞平台语音听写AppId
   */
  public static VoiceConfig KDXF(String kdxfAppId) {
    VoiceConfig config = new VoiceConfig();
    config.mVoiceRecognizer = VoiceRecognizer.KDXF;
    config.mKDXFAppId = kdxfAppId;
    return config;
  }

//  /**
//   * 百度语音听写配置
//   */
//  public static VoiceConfig Baidu() {
//    VoiceConfig config = new VoiceConfig();
//    config.mVoiceRecognizer = VoiceRecognizer.BAIDU;
//    return config;
//  }

  public VoiceRecognizer getVoiceRecognizer() {
    return mVoiceRecognizer;
  }

  public String getKDXFAppId() {
    return mKDXFAppId;
  }

  private VoiceRecognizer mVoiceRecognizer;
  private String mKDXFAppId;

  private VoiceConfig() {
  }
}
