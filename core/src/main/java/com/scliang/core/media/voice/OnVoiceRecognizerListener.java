package com.scliang.core.media.voice;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/1/22.
 */
public interface OnVoiceRecognizerListener {
    void onRecogStart(VoiceRecognizer recognizer);
    void onRecognition(VoiceRecognizer recognizer, String word);
    void onRecogStop(VoiceRecognizer recognizer);
    void onRecogError(VoiceRecognizer recognizer, int code, String message);
    void onRecogLast(VoiceRecognizer recognizer);
    void onRecogLog(VoiceRecognizer recognizer, String log);
}
