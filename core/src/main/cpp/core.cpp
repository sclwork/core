//
// Created by scliang on 2017/9/28.
//
#include <jni.h>
#include <string>
#include <vector>
#include <assert.h>
#include <android/log.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/epoll.h>
#include <inttypes.h>
#include <sys/ioctl.h>
#include <pthread.h>
#include <mutex>

#define  LOG_TAG "Core-Native"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using namespace std;

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_scliang_core_base_NUtils_getNativeVersion(JNIEnv *env, jobject obj)
{
    string ver = "0.0.1";
    return env->NewStringUTF(ver.c_str());
}

}
