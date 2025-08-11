package com.tqk114514.craftmusic.audio;

import com.tqk114514.craftmusic.CraftMusic;

public final class MiniaudioPlayer implements AutoCloseable {
    private volatile boolean outputReady;
    private volatile boolean initialized;
    private volatile boolean playing;
    private volatile String lastPlayedAbsolutePath;
    private volatile boolean paused;
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
            // 恢复最近一次音量
            try { nSetVolume(this.volume); } catch (Throwable ignored) {}
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
        this.volume = Math.max(0f, Math.min(1f, v));
        if (!outputReady) return;
        nSetVolume(this.volume);
    }

    public float getVolume() {
        if (!outputReady) return this.volume;
        try { this.volume = nGetVolume(); } catch (Throwable ignored) {}
        return this.volume;
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
}

