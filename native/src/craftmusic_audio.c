// CraftMusic - JNI miniaudio backend (最小实现)
// 请将 miniaudio.h 放到 native/include/miniaudio.h 或同目录，并按下方命令编译为 craftmusic_audio.dll

#include <jni.h>
#include <string.h>
#include <stdint.h>
#define _USE_MATH_DEFINES
#include <math.h>
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
#if defined(_WIN32)
#include <windows.h>
#endif

#define MINIAUDIO_IMPLEMENTATION
#define MA_ENABLE_DECODING
#define MA_NO_ENCODING
#define MA_ENABLE_VORBIS
#define MA_ENABLE_FLAC
#define MA_ENABLE_MP3
#include "miniaudio.h"

// 使用 ma_engine + ma_sound 的高层播放器
static ma_engine g_engine;
static ma_sound g_sound;
static int g_initialized = 0;
static int g_soundInited = 0;
// 纯音数据源
static ma_waveform g_waveform;
static int g_waveformInited = 0;
// 自实现暂停/恢复所需状态
static int g_isPaused = 0;
static ma_uint64 g_pausedFrame = 0;

// 频谱可视化：独立解码器用于抓取PCM窗口（不影响播放）
static ma_decoder g_visDecoder;
static int g_visInited = 0;

// FFT 固定长度（MSVC 不支持 VLA，使用宏常量）
#define FFT_N 2048

// 简易 FFT（基2 Cooley-Tukey），长度需为2的幂。仅用于可视化，性能足够。
typedef struct { float r; float i; } cmplx;
static void fft_radix2(cmplx* a, int n) {
    // 位逆序
    int j = 0;
    for (int i = 1; i < n; i++) {
        int bit = n >> 1;
        for (; j & bit; bit >>= 1) j ^= bit;
        j ^= bit;
        if (i < j) { cmplx t = a[i]; a[i] = a[j]; a[j] = t; }
    }
    // 蝶形运算
    for (int len = 2; len <= n; len <<= 1) {
        float ang = -2.0f * (float)M_PI / (float)len;
        float wlen_r = cosf(ang), wlen_i = sinf(ang);
        for (int i = 0; i < n; i += len) {
            float wr = 1.0f, wi = 0.0f;
            for (int k = 0; k < len/2; k++) {
                int u = i + k;
                int v = i + k + len/2;
                float vr = a[v].r * wr - a[v].i * wi;
                float vi = a[v].r * wi + a[v].i * wr;
                float ur = a[u].r;
                float ui = a[u].i;
                a[u].r = ur + vr; a[u].i = ui + vi;
                a[v].r = ur - vr; a[v].i = ui - vi;
                // 更新旋转因子
                float nwr = wr * wlen_r - wi * wlen_i;
                wi = wr * wlen_i + wi * wlen_r;
                wr = nwr;
            }
        }
    }
}

JNIEXPORT jboolean JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nInit(JNIEnv* env, jclass cls, jint sampleRate, jint channels) {
    (void)env; (void)cls;
    if (g_initialized) return JNI_TRUE;

    ma_engine_config ecfg = ma_engine_config_init();
    ecfg.channels   = (ma_uint32)(channels > 0 ? channels : 2);
    ecfg.sampleRate = (ma_uint32)(sampleRate > 0 ? sampleRate : 48000);
    if (ma_engine_init(&ecfg, &g_engine) != MA_SUCCESS) {
        return JNI_FALSE;
    }
    g_initialized = 1;
    g_soundInited = 0;
    g_isPaused = 0;
    g_pausedFrame = 0;
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nPlayFile(JNIEnv* env, jclass cls, jstring jpath) {
    (void)cls;
    if (!g_initialized || jpath == NULL) return -100; // not initialized or bad arg

    // 停止并卸载旧 sound
    if (g_soundInited) {
        ma_sound_uninit(&g_sound);
        g_soundInited = 0;
    }
    if (g_visInited) {
        ma_decoder_uninit(&g_visDecoder);
        g_visInited = 0;
    }

    // Windows: 直接使用 UTF-16 宽字符接口，避免多字节转换导致的中文名失败
    ma_result r = MA_ERROR;
#if defined(_WIN32)
    const jchar* wstr = (*env)->GetStringChars(env, jpath, NULL);
    if (wstr != NULL) {
        DWORD attrs = GetFileAttributesW((LPCWSTR)wstr);
        if (attrs == INVALID_FILE_ATTRIBUTES) {
            fprintf(stderr, "[CraftMusic/native] file not found (UTF-16): path does not exist\n");
            (*env)->ReleaseStringChars(env, jpath, wstr);
            return (jint)MA_DOES_NOT_EXIST;
        }
        r = ma_sound_init_from_file_w(&g_engine, (const wchar_t*)wstr, MA_SOUND_FLAG_STREAM, NULL, NULL, &g_sound);
        if (r != MA_SUCCESS) {
            fprintf(stderr, "[CraftMusic/native] sound init failed (wide path): %d\n", (int)r);
        }
        // 同一路径打开一个解码器，供可视化读取PCM
        if (r == MA_SUCCESS) {
            ma_decoder_config cfg = ma_decoder_config_init(ma_format_f32,
                ma_engine_get_channels(&g_engine),
                ma_engine_get_sample_rate(&g_engine));
            ma_result vr = ma_decoder_init_file_w((const wchar_t*)wstr, &cfg, &g_visDecoder);
            if (vr == MA_SUCCESS) g_visInited = 1; else g_visInited = 0;
        }
        (*env)->ReleaseStringChars(env, jpath, wstr);
    }
#else
    const char* cstr = (*env)->GetStringUTFChars(env, jpath, NULL);
    if (cstr != NULL) {
        r = ma_sound_init_from_file(&g_engine, cstr, MA_SOUND_FLAG_STREAM, NULL, NULL, &g_sound);
        if (r != MA_SUCCESS) {
            fprintf(stderr, "[CraftMusic/native] sound init failed (utf8 non-win): %d\n", (int)r);
        }
        if (r == MA_SUCCESS) {
            ma_decoder_config cfg = ma_decoder_config_init(ma_format_f32,
                ma_engine_get_channels(&g_engine),
                ma_engine_get_sample_rate(&g_engine));
            ma_result vr = ma_decoder_init_file(cstr, &cfg, &g_visDecoder);
            if (vr == MA_SUCCESS) g_visInited = 1; else g_visInited = 0;
        }
        (*env)->ReleaseStringUTFChars(env, jpath, cstr);
    }
#endif

    if (r != MA_SUCCESS) {
        fprintf(stderr, "[CraftMusic/native] sound init failed: %d\n", (int)r);
        return (jint)r;
    }
    g_soundInited = 1;
    g_isPaused = 0;
    g_pausedFrame = 0;
    ma_sound_set_fade_in_milliseconds(&g_sound, 0.0f, 1.0f, 300); // 300ms 淡入
    ma_sound_start(&g_sound);
    return 0;
}

JNIEXPORT void JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nStop(JNIEnv* env, jclass cls) {
    (void)env; (void)cls;
    if (g_soundInited) {
        ma_sound_set_fade_in_milliseconds(&g_sound, 1.0f, 0.0f, 200); // 200ms 淡出
        ma_sound_stop(&g_sound);
        ma_sound_uninit(&g_sound);
        g_soundInited = 0;
    }
    if (g_visInited) {
        ma_decoder_uninit(&g_visDecoder);
        g_visInited = 0;
    }
    g_isPaused = 0;
    g_pausedFrame = 0;
}

JNIEXPORT void JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nDispose(JNIEnv* env, jclass cls) {
    (void)env; (void)cls;
    if (g_soundInited) {
        ma_sound_stop(&g_sound);
        ma_sound_uninit(&g_sound);
        g_soundInited = 0;
    }
    if (g_visInited) {
        ma_decoder_uninit(&g_visDecoder);
        g_visInited = 0;
    }
    if (g_initialized) {
        ma_engine_uninit(&g_engine);
        g_initialized = 0;
    }
    g_isPaused = 0;
    g_pausedFrame = 0;
}

JNIEXPORT void JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nPlayTone(JNIEnv* env, jclass cls, jint hz, jint durationMs) {
    (void)env; (void)cls;
    if (!g_initialized) return;
    if (hz <= 0) hz = 440;
    if (durationMs <= 0) durationMs = 1000;

    // 使用 engine 的 waveform 播放纯音（淡入淡出）
    if (g_soundInited) {
        ma_sound_stop(&g_sound);
        ma_sound_uninit(&g_sound);
        g_soundInited = 0;
    }

    ma_waveform_config wcfg = ma_waveform_config_init(ma_format_f32, ma_engine_get_channels(&g_engine), ma_engine_get_sample_rate(&g_engine), ma_waveform_type_sine, 0.25, (double)hz);
    if (g_waveformInited) {
        ma_waveform_uninit(&g_waveform);
        g_waveformInited = 0;
    }
    if (ma_waveform_init(&wcfg, &g_waveform) != MA_SUCCESS) return;
    g_waveformInited = 1;
    ma_result r = ma_sound_init_from_data_source(&g_engine, (ma_data_source*)&g_waveform, MA_SOUND_FLAG_STREAM, NULL, &g_sound);
    if (r != MA_SUCCESS) return;
    g_soundInited = 1;
    g_isPaused = 0;
    g_pausedFrame = 0;
    ma_sound_set_fade_in_milliseconds(&g_sound, 0.0f, 1.0f, 100);
    ma_sound_start(&g_sound);
    // 简单的duration控制：启动一个淡出计时（这里留给后续改进，如独立线程/计时器）。
}

// ----- 进度、拖动、暂停/恢复 -----
JNIEXPORT jint JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nGetLengthMs(JNIEnv* env, jclass cls) {
    (void)env; (void)cls;
    if (!g_initialized || !g_soundInited) return 0;
    ma_uint64 frames = 0;
    if (ma_sound_get_length_in_pcm_frames(&g_sound, &frames) != MA_SUCCESS) return 0;
    ma_uint32 sr = ma_engine_get_sample_rate(&g_engine);
    if (sr == 0) return 0;
    double ms = ((double)frames * 1000.0) / (double)sr;
    if (ms < 0.0) ms = 0.0;
    if (ms > 2147483647.0) ms = 2147483647.0;
    return (jint)ms;
}

JNIEXPORT jint JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nGetPositionMs(JNIEnv* env, jclass cls) {
    (void)env; (void)cls;
    if (!g_initialized || !g_soundInited) return 0;
    // 当前播放位置（帧）- 你的头文件为“返回帧数”的单参版本
    ma_uint64 frames = ma_sound_get_time_in_pcm_frames(&g_sound);
    ma_uint32 sr = ma_engine_get_sample_rate(&g_engine);
    if (sr == 0) return 0;
    double ms = ((double)frames * 1000.0) / (double)sr;
    if (ms < 0.0) ms = 0.0;
    if (ms > 2147483647.0) ms = 2147483647.0;
    return (jint)ms;
}

// 读取频谱：返回 N 段能量（线性幅度0..1）。
// Java 侧应传入一个 float[] 接收；count 建议 32 或 64。
JNIEXPORT jint JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nGetSpectrum(JNIEnv* env, jclass cls, jfloatArray jOut, jint bands) {
    (void)cls;
    if (!g_initialized || !g_visInited || jOut == NULL || bands <= 0) return 0;
    jsize outLen = (*env)->GetArrayLength(env, jOut);
    if (outLen < bands) return 0;

    const int N = FFT_N; // FFT 长度
    const int channels = (int)ma_engine_get_channels(&g_engine);
    float window[FFT_N];
    static float hann[FFT_N];
    static int hannReady = 0;
    if (!hannReady) {
        for (int i = 0; i < N; i++) hann[i] = 0.5f * (1.0f - cosf(2.0f * (float)M_PI * i / (N - 1)));
        hannReady = 1;
    }

    // 对齐到当前播放帧（暂停时使用 g_pausedFrame），保证谱图与播放状态同步
    ma_uint64 curFrame = g_isPaused ? g_pausedFrame : ma_sound_get_time_in_pcm_frames(&g_sound);
    ma_decoder_seek_to_pcm_frame(&g_visDecoder, curFrame);
    // 尝试从该位置读取N帧
    float tmp[FFT_N * 2];
    ma_uint64 framesRead = 0;
    ma_decoder_read_pcm_frames(&g_visDecoder, tmp, N, &framesRead);
    // 若不足，剩余补零
    if (framesRead == 0) {
        // 输出清零
        jfloat* out = (*env)->GetFloatArrayElements(env, jOut, NULL);
        if (out) { for (int i = 0; i < bands; i++) out[i] = 0.0f; (*env)->ReleaseFloatArrayElements(env, jOut, out, 0); }
        return 0;
    }

    // 混合到单声道并加窗
    for (int i = 0; i < N; i++) {
        float s = 0.0f;
        for (int c = 0; c < channels; c++) s += tmp[i * channels + c];
        s /= (float)channels;
        window[i] = s * hann[i];
    }
    // 复数缓冲
    cmplx a[FFT_N];
    for (int i = 0; i < N; i++) { a[i].r = window[i]; a[i].i = 0.0f; }
    fft_radix2(a, N);

    // 频率分桶：20Hz-20kHz 对数等比，将 N/2 频谱能量映射到 bands 段（使用RMS+归一化，减少高频顶满）
    int sr = (int)ma_engine_get_sample_rate(&g_engine);
    float fmin = 40.0f, fmax = (float)(sr * 0.5);
    if (fmax < fmin) fmax = fmin + 1.0f;
    float logMin = logf(fmin), logMax = logf(fmax);
    float outBuf[256]; if (bands > 256) bands = 256;
    const float normFactor = 2.0f / (float)N; // 单边谱幅值归一化
    int lastK1 = 0;
    for (int b = 0; b < bands; b++) {
        float frac0 = (float)b / (float)bands;
        float frac1 = (float)(b + 1) / (float)bands;
        float f0 = expf(logMin + (logMax - logMin) * frac0);
        float f1 = expf(logMin + (logMax - logMin) * frac1);
        int k0 = (int)(f0 * N / sr);
        int k1 = (int)(f1 * N / sr);
        if (k0 < 1) k0 = 1;
        if (k1 >= N/2) k1 = N/2 - 1;
        // 强制边界严格单调，避免相邻多个段落在同一bin
        if (k0 <= lastK1) k0 = lastK1 + 1;
        if (k1 <= k0) k1 = k0 + 1;
        if (k1 >= N/2) k1 = N/2 - 1;
        float sumPow = 0.0f; int count = 0;
        for (int k = k0; k <= k1; k++) {
            float re = a[k].r, im = a[k].i;
            float pow = re*re + im*im;
            sumPow += pow; count++;
        }
        float rms = (count > 0) ? sqrtf(sumPow / (float)count) * normFactor : 0.0f;
        // dB 压缩到 0..1，扩展动态范围到 [-80,0]
        float db = 20.0f * log10f(rms + 1e-9f);
        const float dbRange = 80.0f; // -80..0 映射
        float norm = (db + dbRange) / dbRange;
        if (norm < 0.0f) norm = 0.0f; if (norm > 1.0f) norm = 1.0f;
        outBuf[b] = norm;
        lastK1 = k1;
    }
    jfloat* out = (*env)->GetFloatArrayElements(env, jOut, NULL);
    if (out) {
        for (int i = 0; i < bands; i++) out[i] = outBuf[i];
        (*env)->ReleaseFloatArrayElements(env, jOut, out, 0);
    }
    // 不推进解码器光标，让谱图严格跟随播放帧。下一次调用会再次按播放位置对齐。
    return bands;
}

JNIEXPORT void JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nSeekMs(JNIEnv* env, jclass cls, jint ms) {
    (void)env; (void)cls;
    if (!g_initialized || !g_soundInited) return;
    if (ms < 0) ms = 0;
    ma_uint32 sr = ma_engine_get_sample_rate(&g_engine);
    ma_uint64 frame = (ma_uint64)(((double)ms / 1000.0) * (double)sr);
    ma_sound_seek_to_pcm_frame(&g_sound, frame);
    // 若当前处于暂停状态，更新暂停帧，使得随后恢复播放会从新位置继续
    if (g_isPaused) {
        g_pausedFrame = frame;
    }
}

JNIEXPORT void JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nPause(JNIEnv* env, jclass cls) {
    (void)env; (void)cls;
    if (!g_initialized || !g_soundInited) return;
    // 记录当前位置并停止（不卸载），实现“暂停”效果
    g_pausedFrame = ma_sound_get_time_in_pcm_frames(&g_sound);
    ma_sound_stop(&g_sound);
    g_isPaused = 1;
}

JNIEXPORT void JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nResume(JNIEnv* env, jclass cls) {
    (void)env; (void)cls;
    if (!g_initialized || !g_soundInited) return;
    if (!g_isPaused) return;
    ma_sound_seek_to_pcm_frame(&g_sound, g_pausedFrame);
    ma_sound_start(&g_sound);
    g_isPaused = 0;
}

JNIEXPORT void JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nSetVolume(JNIEnv* env, jclass cls, jfloat v) {
    (void)env; (void)cls;
    if (!g_initialized || !g_soundInited) return;
    float vol = v;
    if (vol < 0.0f) vol = 0.0f;
    if (vol > 1.0f) vol = 1.0f;
    ma_sound_set_volume(&g_sound, vol);
}

JNIEXPORT jfloat JNICALL
Java_com_tqk114514_craftmusic_audio_MiniaudioPlayer_nGetVolume(JNIEnv* env, jclass cls) {
    (void)env; (void)cls;
    if (!g_initialized || !g_soundInited) return 1.0f;
    return ma_sound_get_volume(&g_sound);
}
