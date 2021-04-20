////
//// Created by scliang on 2017/10/6.
////
//#ifndef WEBRTC_ANDROID
//#define WEBRTC_ANDROID
//#endif
//
//#include <stdio.h>
//#include <jni.h>
//#include <assert.h>
//#include <android/log.h>
//#include <cmath>
//#include <cstdlib>
//#include <cstring>
//#ifdef WEBRTC_ANDROID
//#include "webrtc/modules/audio_processing/include/audio_processing.h"
//#include "webrtc/modules/include/module_common_types.h"
//#include "webrtc/common_audio/channel_buffer.h"
//#endif
//#include "speex.h"
//
//#define LOG_TAG "Media-Native"
//#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
//
//#ifdef WEBRTC_ANDROID
//using namespace webrtc;
//#endif
//
//
//// webrtc
//class ApmWrapper;
//static ApmWrapper *__apm;
//
//static void set_ctx(JNIEnv *env, jobject thiz, void *ctx) {
////    jclass cls = env->GetObjectClass(thiz);
////    jfieldID fid = env->GetFieldID(cls, "objData", "J");
////    env->SetLongField(thiz, fid, (jlong)ctx);
//    __apm = (ApmWrapper *) ctx;
//}
//
//static void *get_ctx(JNIEnv *env, jobject thiz) {
////    jclass cls = env->GetObjectClass(thiz);
////    jfieldID fid = env->GetFieldID(cls, "objData", "J");
////    return (void*)env->GetLongField(thiz, fid);
//    return __apm;
//}
//
//class ApmWrapper {
//
//#ifdef WEBRTC_ANDROID
//    const int sample_rate_hz = AudioProcessing::kSampleRate16kHz;
//    const int num_input_channels = 1;
//
//    const int reverse_sample_rate_hz = AudioProcessing::kSampleRate16kHz;
//    const int num_reverse_channels = 1;
//#endif
//
//public:
//    ApmWrapper(bool aecExtendFilter, bool speechIntelligibilityEnhance,
//               bool delayAgnostic, bool beamforming, bool nextGenerationAec,
//               bool experimentalNs, bool experimentalAgc){
//#ifdef WEBRTC_ANDROID
//        _beamForming = beamforming;
//
//        Config config;
//        config.Set<ExtendedFilter>(new ExtendedFilter(aecExtendFilter));
//        config.Set<Intelligibility>(new Intelligibility(speechIntelligibilityEnhance));
//        config.Set<DelayAgnostic>(new DelayAgnostic(delayAgnostic));
//
//        /*
//        MockNonlinearBeamformer* beamFormer = nullptr;
//        if(beamforming) {
//            std::vector<webrtc::Point> geometry;
//            geometry.push_back(webrtc::Point(0.f, 0.f, 0.f));
//            geometry.push_back(webrtc::Point(0.05f, 0.f, 0.f));
//            config.Set<Beamforming>(new Beamforming(beamforming, geometry));
//            beamFormer = new MockNonlinearBeamformer(geometry);
//        }*/
//
//        config.Set<NextGenerationAec>(new NextGenerationAec(nextGenerationAec));
//        config.Set<ExperimentalNs>(new ExperimentalNs(experimentalNs));
//        config.Set<ExperimentalAgc>(new ExperimentalAgc(experimentalAgc));
//
//        _apm.reset(AudioProcessing::Create(config));
//
//        /*
//        if(beamforming) {
//            _apm.reset(AudioProcessing::Create(config, beamFormer));
//        }else {
//            _apm.reset(AudioProcessing::Create(config));
//        }*/
//
//        _frame = new AudioFrame();
//        _reverseFrame = new AudioFrame();
//
//        SetContainerFormat(sample_rate_hz, num_input_channels, _frame, &_float_cb);
//
//        SetContainerFormat(reverse_sample_rate_hz, num_reverse_channels, _reverseFrame,
//                           &_revfloat_cb);
//
//        _apm->Initialize({{{_frame->sample_rate_hz_, _frame->num_channels_},
//                                  {_frame->sample_rate_hz_, _frame->num_channels_},
//                                  {_reverseFrame->sample_rate_hz_, _reverseFrame->num_channels_},
//                                  {_reverseFrame->sample_rate_hz_, _reverseFrame->num_channels_}}});
//#endif
//    }
//
//    ~ApmWrapper(){
//#ifdef WEBRTC_ANDROID
//        delete _frame;
//        delete _reverseFrame;
//#endif
//    }
//
//    int ProcessStream(int16_t* data){
//#ifdef WEBRTC_ANDROID
//        std::copy(data, data + _frame->samples_per_channel_, _frame->data_);
////        ConvertToFloat(*_frame, _float_cb.get());
//        int ret = _apm->ProcessStream(_frame);
//        std::copy(_frame->data_, _frame->data_ + _frame->samples_per_channel_, data);
//        return ret;
//#else
//        return 0;
//#endif
//    }
//
//    int ProcessReverseStream(int16_t* data){
//#ifdef WEBRTC_ANDROID
//        std::copy(data, data + _reverseFrame->samples_per_channel_, _reverseFrame->data_);
////        ConvertToFloat(*_reverseFrame, _revfloat_cb.get());
//        int ret = _apm->ProcessReverseStream(_reverseFrame);
//        if(_beamForming){
//            std::copy(_reverseFrame->data_, _reverseFrame->data_ + _reverseFrame->samples_per_channel_, data);
//        }
//        return ret;
////        return _apm->AnalyzeReverseStream(_reverseFrame);
//#else
//        return 0;
//#endif
//    }
//
//#ifdef WEBRTC_ANDROID
//public:
//    std::unique_ptr<AudioProcessing> _apm;
//
//private:
//    template <typename T>
//    void SetContainerFormat(int sample_rate_hz,
//                            size_t num_channels,
//                            AudioFrame* frame,
//                            std::unique_ptr<ChannelBuffer<T> >* cb) {
//        SetFrameSampleRate(frame, sample_rate_hz);
//        frame->num_channels_ = num_channels;
//        cb->reset(new ChannelBuffer<T>(frame->samples_per_channel_, num_channels));
//    }
//
//    void SetFrameSampleRate(AudioFrame* frame,
//                            int sample_rate_hz) {
//        frame->sample_rate_hz_ = sample_rate_hz;
//        frame->samples_per_channel_ = AudioProcessing::kChunkSizeMs *
//                                      sample_rate_hz / 1000;
//    }
//
//    void ConvertToFloat(const int16_t* int_data, ChannelBuffer<float>* cb) {
//        ChannelBuffer<int16_t> cb_int(cb->num_frames(),
//                                      cb->num_channels());
//        Deinterleave(int_data,
//                     cb->num_frames(),
//                     cb->num_channels(),
//                     cb_int.channels());
//        for (size_t i = 0; i < cb->num_channels(); ++i) {
//            S16ToFloat(cb_int.channels()[i],
//                       cb->num_frames(),
//                       cb->channels()[i]);
//        }
//    }
//
//    void ConvertToFloat(const AudioFrame& frame, ChannelBuffer<float>* cb) {
//        ConvertToFloat(frame.data_, cb);
//    }
//
//private:
//    AudioFrame *_frame;
//    AudioFrame *_reverseFrame;
//
//    std::unique_ptr<ChannelBuffer<float>> _float_cb;
//    std::unique_ptr<ChannelBuffer<float>> _revfloat_cb;
//
//    bool _beamForming = false;
//#endif
//};
//
//#ifdef __cplusplus
//extern "C" {
//#endif
//
//jboolean _createApm(JNIEnv *env, jobject thiz,
//                    jboolean aecExtendFilter, jboolean speechIntelligibilityEnhance,
//                    jboolean delayAgnostic, jboolean beamforming,
//                    jboolean nextGenerationAec, jboolean experimentalNs,
//                    jboolean experimentalAgc) {
//    ApmWrapper *apm = new ApmWrapper(aecExtendFilter,
//                                     speechIntelligibilityEnhance,
//                                     delayAgnostic, beamforming,
//                                     nextGenerationAec,
//                                     experimentalNs,
//                                     experimentalAgc);
//    set_ctx(env, thiz, apm);
//    LOGD("Apm created");
//    return JNI_TRUE;
//}
//
//void _freeApm(JNIEnv *env, jobject thiz) {
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    delete apm;
//    set_ctx(env, thiz, 0);
//    LOGD("Apm destroyed");
//}
//
//jint _highPassFilterEnable(JNIEnv *env, jobject thiz, jboolean enable) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->high_pass_filter()->Enable(enable);
//#else
//    return 0;
//#endif
//}
//
//jint _aecEnable(JNIEnv *env, jobject thiz, jboolean enable) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->echo_cancellation()->Enable(enable);
//#else
//    return 0;
//#endif
//}
//
//jint _aecClockDriftCompensationEnable(JNIEnv *env, jobject thiz, jboolean enable) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->echo_cancellation()->enable_drift_compensation(enable);
//#else
//    return 0;
//#endif
//}
//
//jint _aecSetSuppressionLevel(JNIEnv *env, jobject thiz, jint level) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    if (level < EchoCancellation::kLowSuppression) {
//        level = EchoCancellation::kLowSuppression;
//    } else if (level > EchoCancellation::kHighSuppression) {
//        level = EchoCancellation::kHighSuppression;
//    }
//
//    return apm->_apm->echo_cancellation()->set_suppression_level(
//            (EchoCancellation::SuppressionLevel) level);
//#else
//    return 0;
//#endif
//}
//
//jint _aecmEnable(JNIEnv *env, jobject thiz, jboolean enable) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->echo_control_mobile()->Enable(enable);
//#else
//    return 0;
//#endif
//}
//
//jint _aecmSetSuppressionLevel(JNIEnv *env, jobject thiz, jint level) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    if (level < EchoControlMobile::kQuietEarpieceOrHeadset) {
//        level = EchoControlMobile::kQuietEarpieceOrHeadset;
//    } else if (level > EchoControlMobile::kLoudSpeakerphone) {
//        level = EchoControlMobile::kLoudSpeakerphone;
//    }
//
//    return apm->_apm->echo_control_mobile()->set_routing_mode(
//            (EchoControlMobile::RoutingMode) level);
//#else
//    return 0;
//#endif
//}
//
//jint _nsSetLevel(JNIEnv *env, jobject thiz, jint level) {
//#ifdef WEBRTC_ANDROID
//    if (level < NoiseSuppression::kLow) {
//        level = NoiseSuppression::kLow;
//    } else if (level > NoiseSuppression::kVeryHigh) {
//        level = NoiseSuppression::kVeryHigh;
//    }
//
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->noise_suppression()->set_level((NoiseSuppression::Level) level);
//#else
//    return 0;
//#endif
//}
//
//jint _nsSetHighLevel(JNIEnv *env, jobject thiz) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->noise_suppression()->set_level(NoiseSuppression::kHigh);
//#else
//    return 0;
//#endif
//}
//
//jint _nsEnable(JNIEnv *env, jobject thiz, jboolean enable) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->noise_suppression()->Enable(enable);
//#else
//    return 0;
//#endif
//}
//
//jint _agcSetAnalogLevelLimits(JNIEnv *env, jobject thiz, jint minimum, jint maximum) {
//#ifdef WEBRTC_ANDROID
//    if (minimum < 0) {
//        minimum = 0;
//    } else if (minimum > 65535) {
//        minimum = 65535;
//    }
//
//    if (maximum < 0) {
//        maximum = 0;
//    } else if (maximum > 65535) {
//        maximum = 65535;
//    }
//
//    if (minimum > maximum) {
//        int temp = minimum;
//        minimum = maximum;
//        maximum = temp;
//    }
//
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->gain_control()->set_analog_level_limits(minimum, maximum);
//#else
//    return 0;
//#endif
//}
//
//jint _agcSetMode(JNIEnv *env, jobject thiz, jint mode) {
//#ifdef WEBRTC_ANDROID
//    if (mode < GainControl::Mode::kAdaptiveAnalog) {
//        mode = GainControl::Mode::kAdaptiveAnalog;
//    } else if (mode > GainControl::Mode::kFixedDigital) {
//        mode = GainControl::Mode::kFixedDigital;
//    }
//
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->gain_control()->set_mode((GainControl::Mode) mode);
//#else
//    return 0;
//#endif
//}
//
//jint _agcSetModeFixedDigital(JNIEnv *env, jobject thiz) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->gain_control()->set_mode((GainControl::Mode) GainControl::Mode::kFixedDigital);
//#else
//    return 0;
//#endif
//}
//
//jint _agcSetTargetLevelDbfs(JNIEnv *env, jobject thiz, jint level) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->gain_control()->set_target_level_dbfs(level);
//#else
//    return 0;
//#endif
//}
//
//jint _agcSetCompressionGainDb(JNIEnv *env, jobject thiz, jint gain) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->gain_control()->set_compression_gain_db(gain);
//#else
//    return 0;
//#endif
//}
//
//jint _agcEnableLimiter(JNIEnv *env, jobject thiz, jboolean enable) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->gain_control()->enable_limiter(enable);
//#else
//    return 0;
//#endif
//}
//
//jint _agcEnable(JNIEnv *env, jobject thiz, jboolean enable) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->gain_control()->Enable(enable);
//#else
//    return 0;
//#endif
//}
//
//jint _agcSetStreamAnalogLevel(JNIEnv *env, jobject thiz, jint level) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->gain_control()->set_stream_analog_level(level);
//#else
//    return 0;
//#endif
//}
//
//jint _agcStreamAnalogLevel(JNIEnv *env, jobject thiz) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->gain_control()->stream_analog_level();
//#else
//    return 0;
//#endif
//}
//
//jint _vadEnable(JNIEnv *env, jobject thiz, jboolean enable) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->voice_detection()->Enable(enable);
//#else
//    return 0;
//#endif
//}
//
//jint _vadSetLikelihood(JNIEnv *env, jobject thiz, jint likelihood) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    if (likelihood < VoiceDetection::kVeryLowLikelihood) {
//        likelihood = VoiceDetection::kVeryLowLikelihood;
//    } else if (likelihood > VoiceDetection::kHighLikelihood) {
//        likelihood = VoiceDetection::kHighLikelihood;
//    }
//
//    return apm->_apm->voice_detection()->set_likelihood(
//            (VoiceDetection::Likelihood) likelihood);
//#else
//    return 0;
//#endif
//}
//
//jboolean _vadStreamHasVoice(JNIEnv *env, jobject thiz) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return 0;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->voice_detection()->stream_has_voice();
//#else
//    return 0;
//#endif
//}
//
//jint _setStreamDelayMs(JNIEnv *env, jobject thiz, jint delay) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    return apm->_apm->set_stream_delay_ms(delay);
//#else
//    return 0;
//#endif
//}
//
//jint _processStream(JNIEnv *env, jobject thiz, jshortArray input) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    short *buffer = env->GetShortArrayElements(input, nullptr);
//    int ret = apm->ProcessStream(buffer);
//
//    env->ReleaseShortArrayElements(input, buffer, 0);
//    return ret;
//#else
//    return -9999;
//#endif
//}
//
//jint _processReverseStream(JNIEnv *env, jobject thiz, jshortArray input) {
//#ifdef WEBRTC_ANDROID
//    void *ctx = get_ctx(env, thiz);
//    if (!ctx)
//        return -9999;
//
//    ApmWrapper *apm = (ApmWrapper *) ctx;
//    short *buffer = env->GetShortArrayElements(input, nullptr);
//    int ret = apm->ProcessReverseStream(buffer);
//
//    env->ReleaseShortArrayElements(input, buffer, 0);
//    return ret;
//#else
//    return 0;
//#endif
//}
//
//#ifdef __cplusplus
//}
//#endif
//
//
//
//
//// speex
//
//#ifdef __cplusplus
//extern "C" {
//#endif
//
//static int codec_open = 0;
//static int dec_frame_size;
//static int enc_frame_size;
//static SpeexBits ebits, dbits;
//void *enc_state;
//void *dec_state;
//
//jint _speex_open(JNIEnv *env, jobject obj, jint compression) {
//    int tmp;
//    if (codec_open++ != 0) {
//        return (jint) 0;
//    }
//
//    speex_bits_init(&ebits);
//    speex_bits_init(&dbits);
//
//    enc_state = speex_encoder_init(&speex_nb_mode);
//    dec_state = speex_decoder_init(&speex_nb_mode);
//    tmp = compression;
//    speex_encoder_ctl(enc_state, SPEEX_SET_QUALITY, &tmp);
//    speex_encoder_ctl(enc_state, SPEEX_GET_FRAME_SIZE, &enc_frame_size);
//    speex_decoder_ctl(dec_state, SPEEX_GET_FRAME_SIZE, &dec_frame_size);
//
//    return (jint) 0;
//}
//
//jint _speex_encode(JNIEnv *env, jobject obj,
//                   jshortArray lin, jint offset, jbyteArray encoded, jint size) {
//
//    jshort buffer[enc_frame_size];
//    jbyte output_buffer[enc_frame_size];
//    int nsamples = (size - 1) / enc_frame_size + 1;
//    int i, tot_bytes = 0;
//
//    if (!codec_open) {
//        return 0;
//    }
//
//    speex_bits_reset(&ebits);
//
//    for (i = 0; i < nsamples; i++) {
//        env->GetShortArrayRegion(lin, offset + i * enc_frame_size,
//                                 enc_frame_size, buffer);
//        speex_encode_int(enc_state, buffer, &ebits);
//    }
//    //env->GetShortArrayRegion(lin, offset, enc_frame_size, buffer);
//    //speex_encode_int(enc_state, buffer, &ebits);
//
//    tot_bytes = speex_bits_write(&ebits, (char *) output_buffer, enc_frame_size);
//    env->SetByteArrayRegion(encoded, 0, tot_bytes, output_buffer);
//
//    return (jint) tot_bytes;
//}
//
//jint _speex_decode(JNIEnv *env,
//                   jobject obj, jbyteArray encoded, jshortArray lin, jint size) {
//
//    jbyte buffer[dec_frame_size];
//    jshort output_buffer[dec_frame_size];
//    jsize encoded_length = size;
//
//    if (!codec_open) {
//        return 0;
//    }
//
//    env->GetByteArrayRegion(encoded, 0, encoded_length, buffer);
//    speex_bits_read_from(&dbits, (char *) buffer, encoded_length);
//    speex_decode_int(dec_state, &dbits, output_buffer);
//    env->SetShortArrayRegion(lin, 0, dec_frame_size, output_buffer);
//
//    return (jint) dec_frame_size;
//}
//
//jint _speex_getFrameSize(JNIEnv *env, jobject obj) {
//    if (!codec_open) {
//        return 0;
//    }
//    return (jint) enc_frame_size;
//}
//
//void _speex_close(JNIEnv *env, jobject obj) {
//    if (--codec_open != 0) {
//        return;
//    }
//    speex_bits_destroy(&ebits);
//    speex_bits_destroy(&dbits);
//    speex_decoder_destroy(dec_state);
//    speex_encoder_destroy(enc_state);
//}
//
//
//#ifdef __cplusplus
//}
//#endif
//
//
//
//
//
//
//static JNINativeMethod methods[] = {
//
//        // webrtc
//        {"webrtc_createApm", "(ZZZZZZZ)Z", (void*)_createApm},
//        {"webrtc_freeApm", "()V", (void*)_freeApm},
//        {"webrtc_highPassFilterEnable", "(Z)I", (void*)_highPassFilterEnable},
//        {"webrtc_aecEnable", "(Z)I", (void*)_aecEnable},
//        {"webrtc_aecClockDriftCompensationEnable", "(Z)I", (void*)_aecClockDriftCompensationEnable},
//        {"webrtc_aecSetSuppressionLevel", "(I)I", (void*)_aecSetSuppressionLevel},
//        {"webrtc_aecmEnable", "(Z)I", (void*)_aecmEnable},
//        {"webrtc_aecmSetSuppressionLevel", "(I)I", (void*)_aecmSetSuppressionLevel},
//        {"webrtc_nsSetLevel", "(I)I", (void*)_nsSetLevel},
//        {"webrtc_nsSetHighLevel", "()I", (void*)_nsSetHighLevel},
//        {"webrtc_nsEnable", "(Z)I", (void*)_nsEnable},
//        {"webrtc_agcSetAnalogLevelLimits", "(II)I", (void*)_agcSetAnalogLevelLimits},
//        {"webrtc_agcSetMode", "(I)I", (void*)_agcSetMode},
//        {"webrtc_agcSetModeFixedDigital", "()I", (void*)_agcSetModeFixedDigital},
//        {"webrtc_agcSetTargetLevelDbfs", "(I)I", (void*)_agcSetTargetLevelDbfs},
//        {"webrtc_agcSetCompressionGainDb", "(I)I", (void*)_agcSetCompressionGainDb},
//        {"webrtc_agcEnableLimiter", "(Z)I", (void*)_agcEnableLimiter},
//        {"webrtc_agcEnable", "(Z)I", (void*)_agcEnable},
//        {"webrtc_agcSetStreamAnalogLevel", "(I)I", (void*)_agcSetStreamAnalogLevel},
//        {"webrtc_agcStreamAnalogLevel", "()I", (void*)_agcStreamAnalogLevel},
//        {"webrtc_vadEnable", "(Z)I", (void*)_vadEnable},
//        {"webrtc_vadSetLikelihood", "(I)I", (void*)_vadSetLikelihood},
//        {"webrtc_vadStreamHasVoice", "()Z", (void*)_vadStreamHasVoice},
//        {"webrtc_setStreamDelayMs", "(I)I", (void*)_setStreamDelayMs},
//        {"webrtc_processStream", "([S)I", (void*)_processStream},
//        {"webrtc_processReverseStream", "([S)I", (void*)_processReverseStream},
//
//        // speex
//        {"speex_open", "(I)I", (void*)_speex_open},
//        {"speex_encode", "([SI[BI)I", (void*)_speex_encode},
//        {"speex_decode", "([B[SI)I", (void*)_speex_decode},
//        {"speex_getFrameSize", "()I", (void*)_speex_getFrameSize},
//        {"speex_close", "()V", (void*)_speex_close},
//};
//
//static int registerNativeMethods(JNIEnv* env) {
//    const char* className = "com/scliang/core/media/MediaNUtils2";
//    jclass clazz = env->FindClass(className);
//    if (clazz == NULL)
//    {
//        return JNI_FALSE;
//    }
//    int numMethods;
//    numMethods = sizeof(methods) / sizeof(methods[0]);
//    if (env->RegisterNatives(clazz, methods, numMethods) < 0)
//    {
//        return JNI_FALSE;
//    }
//    return JNI_TRUE;
//}
//
//JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
//{
//    JNIEnv* env = NULL;
//    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK)
//    {
//        return -1;
//    }
//    assert(env != NULL);
//
//    if (!registerNativeMethods(env))
//    {
//        return -1;
//    }
//
//    LOGD("------ JNI OnLoad OK ------\n");
//    return JNI_VERSION_1_6;
//}
//
//JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved)
//{
//    LOGD("------ JNI OnUnload OK ------\n");
//}
