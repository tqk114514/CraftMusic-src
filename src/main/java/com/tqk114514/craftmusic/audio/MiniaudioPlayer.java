package com.tqk114514.craftmusic.audio;

import com.tqk114514.craftmusic.CraftMusic;

public final class MiniaudioPlayer implements AutoCloseable {
    private volatile boolean outputReady;
    private volatile boolean initialized;
    private volatile boolean playing;
    private volatile String lastPlayedAbsolutePath;
    private volatile boolean paused;
    /** 滑块值（线性 0..1），不是实际输出增益 —— 实际增益见 {@link #toOutputGain}。 */
    private volatile float volume = 1.0f;

    public MiniaudioPlayer(int sampleRate, int channels) {
        try {
            NativeAudio.ensureLoaded();
            this.outputReady = nInit(sampleRate, channels);
            this.initialized = true;
            this.playing = false;
            this.lastPlayedAbsolutePath = null;
            this.paused = false;
        } catch (Throwable t) {
            CraftMusic.LOGGER.warn("Failed to load native audio library: {}", t.toString());
            this.outputReady = false;
            this.initialized = false;
            this.playing = false;
            this.lastPlayedAbsolutePath = null;
            this.paused = false;
        }
    }

    public boolean isOutputReady() {
        return outputReady;
    }

    public int play(String absolutePath) {
        if (!outputReady) return -1;
        if (absolutePath == null || absolutePath.isBlank()) return -2;
        int rc = nPlayFile(absolutePath);
        this.playing = (rc == 0);
        if (rc == 0) {
            this.lastPlayedAbsolutePath = absolutePath;
            this.paused = false;
            // 确保与配置音量一致（避免首次播放为 100%）
            try {
                setSliderVolume(com.tqk114514.craftmusic.client.ClientConfig.getVolume());
            } catch (Throwable ignored) {}
        }
        return rc;
    }

    public void stop() {
        if (!outputReady) return;
        nStop();
        this.playing = false;
        this.paused = false;
    }

    public void playTone(int hz, int durationMs) {
        if (!outputReady) return;
        nPlayTone(hz, durationMs);
        this.playing = true;
        this.lastPlayedAbsolutePath = null;
        this.paused = false;
        // 同步配置音量
        try {
            setSliderVolume(com.tqk114514.craftmusic.client.ClientConfig.getVolume());
        } catch (Throwable ignored) {}
    }

    @Override
    public void close() {
        if (!initialized) return;
        try {
            nDispose();
        } finally {
            initialized = false;
            outputReady = false;
            playing = false;
            lastPlayedAbsolutePath = null;
            paused = false;
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public String getLastPlayedAbsolutePath() {
        return lastPlayedAbsolutePath;
    }

    public int getLengthMs() {
        if (!outputReady) return 0;
        return nGetLengthMs();
    }

    public int getPositionMs() {
        if (!outputReady) return 0;
        return nGetPositionMs();
    }

    public void seekToMs(int ms) {
        if (!outputReady) return;
        nSeekMs(ms);
    }

    public void pause() {
        if (!outputReady) return;
        nPause();
        this.playing = false;
        this.paused = true;
    }

    public void resume() {
        if (!outputReady) return;
        nResume();
        this.playing = true;
        this.paused = false;
    }

    public boolean isPaused() { return paused; }

    public void setVolume(float v) {
        setSliderVolume(v);
    }

    /**
     * 返回滑块值（线性 0..1），供 UI 显示与配置比对使用。
     *
     * 注意不要改用 nGetVolume()：native 侧存的是经 {@link #toOutputGain} 映射后的
     * 输出增益，把它直接当滑块值会造成显示错乱，并让外层的音量守护逻辑误判为
     * “实际音量与配置不符”而每帧重新下发。
     */
    public float getVolume() {
        return this.volume;
    }

    // 读取频谱能量，返回有效段数。bands 建议 32 或 64。
    public int getSpectrum(float[] out, int bands) {
        if (!outputReady || out == null || bands <= 0) return 0;
        return nGetSpectrum(out, bands);
    }

    private void setSliderVolume(float sliderValue) {
        this.volume = Math.max(0f, Math.min(1f, sliderValue));
        if (!outputReady) return;
        nSetVolume(toOutputGain(this.volume));
    }

    /**
     * 滑块值 → 实际输出增益。
     *
     * 人耳对音量的感知接近对数，直接用线性值会导致滑块前半段几乎无效：
     * 拖到 50% 听起来仍有约 70% 那么响。这里用平方做近似，
     * 50% → 0.25（-12 dB），30% → 0.09（-21 dB），手感接近系统音量滑块。
     */
    private static float toOutputGain(float sliderValue) {
        return sliderValue * sliderValue;
    }

    private static native boolean nInit(int sampleRate, int channels);
    private static native int nPlayFile(String absolutePath);
    private static native void nStop();
    private static native void nDispose();
    private static native void nPlayTone(int hz, int durationMs);
    private static native int nGetLengthMs();
    private static native int nGetPositionMs();
    private static native void nSeekMs(int ms);
    private static native void nPause();
    private static native void nResume();
    private static native void nSetVolume(float v);
    private static native float nGetVolume();
    private static native int nGetSpectrum(float[] out, int bands);
}
