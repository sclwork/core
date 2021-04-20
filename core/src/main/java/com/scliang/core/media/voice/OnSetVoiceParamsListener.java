package com.scliang.core.media.voice;

import java.util.Map;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/5/11.
 */
public interface OnSetVoiceParamsListener {
    Map<String, String> onSetVoiceParams(VoiceConfig voiceConfig);
}
