//
// Created by scliang on 2020/7/11.
//

#include <cstdio>
#include <jni.h>
#include <cassert>
#include <android/log.h>
#include <cmath>
#include <cstdlib>
#include <cstring>
#include <thread>

#include "lamemp3/lame.h"
#include "ns/noise_suppression.h"


#define DR_WAV_IMPLEMENTATION
#include "ns/dr_wav.h"

#define DR_MP3_IMPLEMENTATION
#include "ns/dr_mp3.h"


#define LOG_TAG "Media-Native"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)


#ifndef min
#define min(a,b) (((a)<(b))?(a):(b))
#endif


// Allow for modes: 0, 1, 2, 3.
//enum NSLevel {
//  kLow,
//  kModerate,
//  kHigh,
//  kVeryHigh


JavaVM *gVM;
class ApmWrapper;
static ApmWrapper *__apm;
static lame_global_flags *glf_ns = nullptr;

static void set_ctx(JNIEnv *env, jobject obj, void *ctx) {
  __apm = (ApmWrapper *) ctx;
}

static void *get_ctx(JNIEnv *env, jobject obj) {
  return __apm;
}




class ApmWrapper {
public:
  ApmWrapper() {
    // create webrtc NS
    pNsHandle = WebRtcNs_Create();
  }
  ~ApmWrapper(){
    freeNs();
  }

public:
  int isInit() {
    return pNsHandle == nullptr ? -1 : 0;
  }

  void freeNs() {
    if (isInit() == 0) {
      WebRtcNs_Free(pNsHandle);
      pNsHandle = nullptr;
    }
  }

  // Allow for modes: 0, 1, 2, 3.
  int setupNs(int sample, int mode) {
    if (isInit() == 0) {
      if (WebRtcNs_Init(pNsHandle, sample) != 0) {
        return -1;
      }
      if (WebRtcNs_set_policy(pNsHandle, mode) != 0) {
        return -1;
      }
      // setup success
      return 0;
    } else {
      return -1;
    }
  }

  int processStream(int16_t* data, int length) {
    if (isInit() == 0) {
      if (length <= 0) {
        return -2;
      }

      for (int i = 0; i < length; i += 160) {
        for (int j = 0; j < 160; j++) {
          sIn[j] = data[i + j];
        }

        auto* p16KIn = reinterpret_cast<short *>(&sIn);
        auto* p16Out = reinterpret_cast<short *>(&sOut);
        const short* const* speechFrame = &p16KIn;
        short* const* outFrame = &p16Out;
        WebRtcNs_Analyze(pNsHandle, p16KIn);
        WebRtcNs_Process(pNsHandle, speechFrame, 1, outFrame);

        for (int j = 0; j < 160; j++) {
          data[i + j] = (short)(sOut[j]);
          sOut[j] = 0;
        }
      }

      return 0;
    } else {
      return -1;
    }
  }

  // Allow for modes: 0, 1, 2, 3.
  void processWavFile(const char *inName, const char *outName, int mode) {
    if (isInit() == 0) {
      //音频采样率
      uint32_t sampleRate = 0;
      //总音频采样数
      uint64_t sampleCount = 0;
      // 声道数
      unsigned int channels = 0;

      // 读取文件内容
      LOGD("Start Read Wav file : %s", inName);
      int16_t *buffer = drwav_open_file_and_read_pcm_frames_s16(
          inName, &channels, &sampleRate, &sampleCount, nullptr);
      LOGD("Read Wav file Completed : sampleRate=%d, channels=%d, count=%lld",
           sampleRate, channels, sampleCount);

      // 判断是否读取成功
      if (buffer == nullptr) {
        return;
      }

      // 配置降噪参数
      setupNs(sampleRate, mode);
      LOGD("Read Wav file sampleRate: %d", sampleRate);

      // 降噪处理
      processStream(buffer, sampleCount);
      LOGD("NS Wav file Completed: %lld", sampleCount);

      // 降噪处理完成，写入文件
      _writeDataToWavFile(sampleRate, channels, buffer, sampleCount, outName);
    }
  }

  // Allow for modes: 0, 1, 2, 3.
  void processMp3File(const char *inName, const char *outName, int mode) {
    if (isInit() == 0) {
      drmp3 mp3;
      if (!drmp3_init_file(&mp3, inName, nullptr)) {
        LOGD("NS Mp3 file init fail");
        return;
      }

      int32_t bitrate = 128; // mp3.frameInfo.bitrate_kbps;
      drmp3_uint32 channels = mp3.channels;
      drmp3_uint32 sampleRate = mp3.sampleRate;
      LOGD("NS Mp3 file read info: channels:%d, sampleRate:%d, bitrate:%d",
          channels, sampleRate, bitrate);

      drmp3_uint64 sampleCount = 0;
      drmp3_int16 *buffer = drmp3__full_read_and_close_s16(&mp3, nullptr, &sampleCount);
      drmp3_uint64 length = sampleCount;
      LOGD("NS Mp3 file read size: %lld", length);

      // 判断是否读取成功
      if (buffer == nullptr) {
        return;
      }

      if (channels == 1) {
        // 配置降噪参数
        setupNs(sampleRate, mode);
        // 降噪处理
        processStream(buffer, length);
        LOGD("NS Mp3 file processStream completed: %lld", length);

        // 降噪处理完成，写入文件
        _writeDataToMp3File(sampleRate, channels, bitrate, buffer, length, outName);

      } else if (channels == 2) {
        auto *lBuf = static_cast<drmp3_int16 *>(malloc(
            length * channels * sizeof(drmp3_int16)));
        for (int i = 0; i < length; i++) {
          lBuf[i] = buffer[i * channels];
        }
        LOGD("NS Mp3 file LBuf copy completed: %lld", length);

        // 配置降噪参数
        setupNs(sampleRate, mode);
        // 降噪处理
        processStream(lBuf, length);
        LOGD("NS Mp3 file processStream completed: %lld", length);

        // 降噪处理完成，写入文件
        _writeDataToMp3File(sampleRate, channels, bitrate, lBuf, length, outName);

        free(lBuf);
      }

      free(buffer);
    }
  }

  static uint64_t _writeDataToWavFile(uint32_t sampleRate, uint32_t channels,
      const int16_t *buffer, uint64_t sampleCount, const char *outName) {
    LOGD("Start Write Wav file : %s", outName);
    drwav_data_format format = {};
    format.container = drwav_container_riff; // <-- drwav_container_riff = normal WAV files, drwav_container_w64 = Sony Wave64.
    format.format = DR_WAVE_FORMAT_PCM;      // <-- Any of the DR_WAVE_FORMAT_* codes.
    format.channels = channels;
    format.sampleRate = (drwav_uint32)sampleRate;
    format.bitsPerSample = 16;
    drwav wav;
    drwav_init_file_write(&wav, outName, &format, nullptr);
    LOGD("Write Wav file sampleRate: %d", sampleRate);
    drwav_uint64 written = drwav_write_pcm_frames(&wav, sampleCount, buffer);
    drwav_uninit(&wav);
    if (written != sampleCount) {
      LOGD("Write Wav file fail.");
      return 0;
    } else {
      LOGD("Write Wav file success: %lld", written);
      return written;
    }
  }

  static void _writeDataToMp3File(uint32_t sampleRate, uint32_t channels, int32_t outBitrate,
      const int16_t *buffer, uint64_t length, const char *outName) {
    FILE *outfile = fopen(outName, "w+");

    LOGD("NS Write Mp3 file: %s", outName);

    glf_ns = lame_init();
    lame_set_in_samplerate(glf_ns, sampleRate);
    lame_set_num_channels(glf_ns, channels);
    lame_set_out_samplerate(glf_ns, sampleRate);
    lame_set_brate(glf_ns, outBitrate);
    lame_set_quality(glf_ns, 7);
    lame_init_params(glf_ns);
    LOGD("NS Write Mp3 file lame_init completed ...");

    LOGD("NS Write Mp3 file start ...");
    uint32_t mp3Size = (int) (7200 + (length * channels * 1.25));
    auto *mp3Buf = static_cast<u_char *>(malloc(mp3Size * sizeof(u_char)));

    LOGD("NS Encode Mp3 file start encode");
    int result = lame_encode_buffer(glf_ns,
        buffer, buffer, length,
        (u_char *) mp3Buf, mp3Size);
    LOGD("NS Encode Mp3 file count: %d", result);

    fwrite(mp3Buf, sizeof(u_char), result, outfile);
    result = lame_encode_flush(glf_ns, (u_char *) mp3Buf, mp3Size);
    LOGD("NS Encode Mp3 file flush: %d", result);

    lame_close(glf_ns);
    glf_ns = nullptr;
    LOGD("NS Encode Mp3 file lame_close");

    fwrite(mp3Buf, sizeof(u_char), result, outfile);
    free(mp3Buf);

    fclose(outfile);
    LOGD("NS Encode Mp3 file close: %s", outName);
  }

private:
  NsHandle *pNsHandle = nullptr;
  short sIn[160]{};
  short sOut[160]{};
};




#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_com_scliang_core_media_MediaNUtils4_createWavFileAndWritePCMData(
    JNIEnv *env, jobject obj, jstring file,
    jint sampleRate, jbyteArray data, int size) {
  const char *fileName = env->GetStringUTFChars(file, nullptr);
  jsize stLen = size / 2;
  jbyte *byBuf = env->GetByteArrayElements(data, nullptr);
  jshortArray stArr = env->NewShortArray(stLen);
  jshort *stBuf = env->GetShortArrayElements(stArr, nullptr);
  for (int i = 0; i < stLen; i++) {
    stBuf[i] = ((jshort)(byBuf[i * 2]) & 0xff) +
              (((jshort)(byBuf[i * 2 + 1]) & 0xff) << 8);
  }
  int res = ApmWrapper::_writeDataToWavFile(sampleRate, 1, stBuf, stLen, fileName);
  env->ReleaseByteArrayElements(data, byBuf, 0);
  env->ReleaseShortArrayElements(stArr, stBuf, 0);
  return res;
}

JNIEXPORT jint JNICALL
Java_com_scliang_core_media_MediaNUtils4_createMp3FileAndWritePCMData(
        JNIEnv *env, jobject obj, jstring file,
        jint sampleRate, jbyteArray data, int size) {
    const char *fileName = env->GetStringUTFChars(file, nullptr);
    jsize stLen = size / 2;
    jbyte *byBuf = env->GetByteArrayElements(data, nullptr);
    jshortArray stArr = env->NewShortArray(stLen);
    jshort *stBuf = env->GetShortArrayElements(stArr, nullptr);
    for (int i = 0; i < stLen; i++) {
        stBuf[i] = ((jshort)(byBuf[i * 2]) & 0xff) +
                   (((jshort)(byBuf[i * 2 + 1]) & 0xff) << 8);
    }
    ApmWrapper::_writeDataToMp3File(sampleRate, 2, 128, stBuf, stLen, fileName);
    env->ReleaseByteArrayElements(data, byBuf, 0);
    env->ReleaseShortArrayElements(stArr, stBuf, 0);
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_scliang_core_media_MediaNUtils4_webrtcCreateApm(
    JNIEnv *env, jobject obj) {
  env->GetJavaVM(&gVM);
  auto *apm = new ApmWrapper();
  set_ctx(env, obj, apm);
  LOGD("Apm created");
  return apm->isInit() == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_scliang_core_media_MediaNUtils4_webrtcFreeApm(
    JNIEnv *env, jobject obj) {
  void *ctx = get_ctx(env, obj);
  __apm = nullptr;
  if (!ctx) return;
  auto *apm = (ApmWrapper *) ctx;
  apm->freeNs();
  delete apm;
  LOGD("Apm release");
}

JNIEXPORT jboolean JNICALL
Java_com_scliang_core_media_MediaNUtils4_webrtcSetupApm16K(
    JNIEnv *env, jobject obj, jint mode) {
  void *ctx = get_ctx(env, obj);
  if (!ctx) return JNI_FALSE;
  auto *apm = (ApmWrapper *) ctx;
  return apm->setupNs(16000, mode) == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_scliang_core_media_MediaNUtils4_webrtcSetupApm441K(
    JNIEnv *env, jobject obj, jint mode) {
  void *ctx = get_ctx(env, obj);
  if (!ctx) return JNI_FALSE;
  auto *apm = (ApmWrapper *) ctx;
  return apm->setupNs(44100, mode) == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_scliang_core_media_MediaNUtils4_webrtcProcessStream(
    JNIEnv *env, jobject obj, jshortArray data) {
  void *ctx = get_ctx(env, obj);
  if (!ctx) return -9999;
  auto *apm = (ApmWrapper *) ctx;
  jsize length = env->GetArrayLength(data);
  short *buffer = env->GetShortArrayElements(data, nullptr);
  int ret = apm->processStream(buffer, length);
  env->ReleaseShortArrayElements(data, buffer, 0);
  return ret;
}

JNIEXPORT void JNICALL
Java_com_scliang_core_media_MediaNUtils4_webrtcFileProcess(
    JNIEnv *env, jobject obj,
    jstring in_file, jstring out_file, jint mode) {
  void *ctx = get_ctx(env, obj);
  if (!ctx) return;
  auto *apm = (ApmWrapper *) ctx;

  const char *wav = ".wav";
  const char *mp3 = ".mp3";

  const char *in_name = env->GetStringUTFChars(in_file, nullptr);
  const char *out_name = env->GetStringUTFChars(out_file, nullptr);
  // check wav
  if (strstr(in_name, wav) != nullptr) {
    apm->processWavFile(in_name, out_name, mode);
  }
  // check mp3
  else if (strstr(in_name, mp3) != nullptr) {
    apm->processMp3File(in_name, out_name, mode);
  }
}

#ifdef __cplusplus
}
#endif
