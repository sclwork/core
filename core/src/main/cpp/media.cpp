//
// Created by scliang on 2017/10/6.
//
#include <cstdio>
#include <jni.h>
#include <cassert>
#include <android/log.h>
#include <cmath>
#include <cstdlib>
#include <cstring>
#include "lamemp3/lame.h"

#define LOG_TAG "Media-Native"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)


extern "C" {

// lamemp3

static lame_global_flags *glf = nullptr;

JNIEXPORT void JNICALL
Java_com_scliang_core_media_MediaNUtils_lamemp3Init(
    JNIEnv *env, jclass type, jint inSampleRate, jint outChannel,
    jint outSampleRate, jint outBitrate, jint quality) {
    if(glf != nullptr){
        lame_close(glf);
        glf = nullptr;
    }
    glf = lame_init();
    lame_set_in_samplerate(glf, inSampleRate);
    lame_set_num_channels(glf, outChannel);
    lame_set_out_samplerate(glf, outSampleRate);
    lame_set_brate(glf, outBitrate);
    lame_set_quality(glf, quality);
    lame_init_params(glf);
}

JNIEXPORT jint JNICALL
Java_com_scliang_core_media_MediaNUtils_lamemp3Encode(
    JNIEnv *env, jclass type, jshortArray buffer_l_,
             jshortArray buffer_r_, jint samples, jbyteArray mp3buf_) {
    jshort *buffer_l = env->GetShortArrayElements(buffer_l_, nullptr);
    jshort *buffer_r = env->GetShortArrayElements(buffer_r_, nullptr);
    jbyte *mp3buf = env->GetByteArrayElements(mp3buf_, nullptr);

    const jsize mp3buf_size = env->GetArrayLength(mp3buf_);

    int result = lame_encode_buffer(glf, buffer_l, buffer_r,
        samples, (u_char*)mp3buf, mp3buf_size);

    env->ReleaseShortArrayElements(buffer_l_, buffer_l, 0);
    env->ReleaseShortArrayElements(buffer_r_, buffer_r, 0);
    env->ReleaseByteArrayElements(mp3buf_, mp3buf, 0);

    return result;
}

JNIEXPORT jint JNICALL
Java_com_scliang_core_media_MediaNUtils_lamemp3Flush(
    JNIEnv *env, jclass type, jbyteArray mp3buf_) {
    jbyte *mp3buf = env->GetByteArrayElements(mp3buf_, nullptr);
    const jsize  mp3buf_size = env->GetArrayLength(mp3buf_);
    int result = lame_encode_flush(glf, (u_char*)mp3buf, mp3buf_size);
    env->ReleaseByteArrayElements(mp3buf_, mp3buf, 0);
    return result;
}

JNIEXPORT void JNICALL
Java_com_scliang_core_media_MediaNUtils_lamemp3Close(
    JNIEnv *env, jclass type){
    lame_close(glf);
    glf = nullptr;
}




#ifndef clamp
#define clamp(a,min,max) (((a)>(max))?(max):(((a)<(min))?(min):(a)))
#endif




// pcm
JNIEXPORT void JNICALL
Java_com_scliang_core_media_MediaNUtils_pcmAdjustVolume(
    JNIEnv *env, jclass obj,
    jshortArray array, jint size, jfloat factor) {
    int i = 0;
    jshort rawSample = 0;

    jshort *buffer = env->GetShortArrayElements(array, nullptr);
    for (i = 0; i < size; i++) {
        rawSample = (jshort)(buffer[i] * factor);
        rawSample = clamp(rawSample, (jshort)-32768, (jshort)32767);
        buffer[i] = rawSample;
    }
    env->SetShortArrayRegion(array, 0, size, buffer);
    env->ReleaseShortArrayElements(array, buffer, 0);
}

JNIEXPORT jdouble JNICALL
Java_com_scliang_core_media_MediaNUtils_pcmDB(
    JNIEnv *env, jclass obj,
    jshortArray array, jint size) {
    int i = 0;
    double sum = 0;
    double sample = 0;
    jshort rawSample = 0;

    jshort *buffer = env->GetShortArrayElements(array, nullptr);
    for (i = 0; i < size; i++) {
        rawSample = buffer[i];
        sample = rawSample / 32767.0;
        sum += sample * sample;
    }

    double rms = sqrt(sum / size);
    jdouble db = 20 * log10(rms);

    env->ReleaseShortArrayElements(array, buffer, 0);
    return db;
}

}
