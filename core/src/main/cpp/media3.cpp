//
// Created by scliang on 2020/7/11.
//

#include <stdio.h>
#include <jni.h>
#include <assert.h>
#include <android/log.h>
#include <cmath>
#include <cstdlib>
#include <cstring>
#include "webrtc/modules/audio_processing/include/audio_processing.h"
#include "webrtc/modules/include/module_common_types.h"
#include "webrtc/common_audio/channel_buffer.h"
#include "webrtc/modules/audio_processing/ns/noise_suppression.h"
#include "webrtc/modules/audio_processing/ns/noise_suppression_x.h"

#define LOG_TAG "Media-Native"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using namespace webrtc;




class ApmWrapper;
static ApmWrapper *__apm;

static void set_ctx(JNIEnv *env, jobject thiz, void *ctx) {
  __apm = (ApmWrapper *) ctx;
}

static void *get_ctx(JNIEnv *env, jobject thiz) {
  return __apm;
}




class ApmWrapper {
public:
  ApmWrapper() {
    // create webrtc NS
    pNsHandle = WebRtcNsx_Create();
  }
  ~ApmWrapper(){
    freeNs();
  }

public:
  int isInit() {
    return pNsHandle == NULL ? -1 : 0;
  }

  void freeNs() {
    if (isInit() == 0) {
      WebRtcNsx_Free(pNsHandle);
      pNsHandle = NULL;
    }
  }

  // mode : 0: Mild, 1: Medium , 2: Aggressive
  int setupNs(int sample, int mode) {
    if (isInit() == 0) {
      if (WebRtcNsx_Init(pNsHandle, sample) != 0) {
        return -1;
      }
      // mode : 0: Mild, 1: Medium , 2: Aggressive
      if (WebRtcNsx_set_policy(pNsHandle, mode) != 0) {
        return -1;
      }
      // setup success
      return 0;
    } else {
      return -1;
    }
  }

  int ProcessStream16K(int16_t* data, int length) {
    if (isInit() == 0) {
      if (length <= 0) {
        return -2;
      }

      for (int i = 0; i < length; i += 160) {
        for (int j = 0; j < 160; j++) {
          s16KIn[j] = data[i + j];
        }

        short* p16KIn = reinterpret_cast<short *>(&s16KIn);
        short* p16Out = reinterpret_cast<short *>(&s16KOut);
        const short* const* speechFrame = &p16KIn;
        short* const* outFrame = &p16Out;
        WebRtcNsx_Process(pNsHandle, speechFrame, 1, outFrame);

        for (int j = 0; j < 160; j++) {
          data[i + j] = (short)(s16KOut[j]);
          s16KOut[j] = 0;
        }
      }

      return 0;
    } else {
      return -1;
    }
  }

  int ProcessStream441K(int16_t* data, int length) {
    if (isInit() == 0) {
      if (length <= 0) {
        return -2;
      }

      for (int i = 0; i < length; i += 441) {
        for (int j = 0; j < 441; j++) {
          s441KIn[j] = data[i + j];
        }

        short* p441In = reinterpret_cast<short *>(&s441KIn);
        short* p441Out = reinterpret_cast<short *>(&s441KOut);
        const short* const* speechFrame = &p441In;
        short* const* outFrame = &p441Out;
        WebRtcNsx_Process(pNsHandle, speechFrame, 1, outFrame);

        for (int j = 0; j < 441; j++) {
          data[i + j] = (short)(p441Out[j]);
          p441Out[j] = 0;
        }
      }

      return 0;
    } else {
      return -1;
    }
  }

private:
  NsxHandle *pNsHandle = NULL;
  short s16KIn[160];
  short s16KOut[160];
  short s441KIn[441];
  short s441KOut[441];
};




#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL
Java_com_scliang_core_media_MediaNUtils3_webrtcCreateApm(
    JNIEnv *env, jobject thiz) {
  ApmWrapper *apm = new ApmWrapper();
  set_ctx(env, thiz, apm);
  LOGD("Apm created");
  return apm->isInit() == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_scliang_core_media_MediaNUtils3_webrtcFreeApm(
    JNIEnv *env, jobject thiz) {
  void *ctx = get_ctx(env, thiz);
  __apm = NULL;
  if (!ctx) return;
  ApmWrapper *apm = (ApmWrapper *) ctx;
  apm->freeNs();
  delete apm;
}

JNIEXPORT jboolean JNICALL
Java_com_scliang_core_media_MediaNUtils3_webrtcSetupApm16K(
    JNIEnv *env, jobject thiz) {
  void *ctx = get_ctx(env, thiz);
  if (!ctx) return JNI_FALSE;
  ApmWrapper *apm = (ApmWrapper *) ctx;
  // mode : 0: Mild, 1: Medium , 2: Aggressive
  return apm->setupNs(16000, 2) == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_scliang_core_media_MediaNUtils3_webrtcSetupApm441K(
    JNIEnv *env, jobject thiz) {
  void *ctx = get_ctx(env, thiz);
  if (!ctx) return JNI_FALSE;
  ApmWrapper *apm = (ApmWrapper *) ctx;
  // mode : 0: Mild, 1: Medium , 2: Aggressive
  return apm->setupNs(44100, 2) == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_scliang_core_media_MediaNUtils3_webrtcProcessStream16K(
    JNIEnv *env, jobject thiz, jshortArray data) {
  void *ctx = get_ctx(env, thiz);
  if (!ctx) return -9999;
  ApmWrapper *apm = (ApmWrapper *) ctx;
  jsize length = env->GetArrayLength(data);
  short *buffer = env->GetShortArrayElements(data, nullptr);
  int ret = apm->ProcessStream16K(buffer, length);
  env->ReleaseShortArrayElements(data, buffer, 0);
  return ret;
}

JNIEXPORT jint JNICALL
Java_com_scliang_core_media_MediaNUtils3_webrtcProcessStream441K(
    JNIEnv *env, jobject thiz, jshortArray data) {
  void *ctx = get_ctx(env, thiz);
  if (!ctx) return -9999;
  ApmWrapper *apm = (ApmWrapper *) ctx;
  jsize length = env->GetArrayLength(data);
  short *buffer = env->GetShortArrayElements(data, nullptr);
  int ret = apm->ProcessStream441K(buffer, length);
  env->ReleaseShortArrayElements(data, buffer, 0);
  return ret;
}

#ifdef __cplusplus
}
#endif
