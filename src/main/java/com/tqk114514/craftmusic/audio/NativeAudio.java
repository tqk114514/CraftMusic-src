package com.tqk114514.craftmusic.audio;

import java.util.concurrent.atomic.AtomicBoolean;

public final class NativeAudio {
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);
    private static final String LIB_NAME = "craftmusic_audio";

    private NativeAudio() {}

    public static void ensureLoaded() {
        if (LOADED.get()) return;
        synchronized (NativeAudio.class) {
            if (LOADED.get()) return;
            try {
                NativeLoader.loadFromJarOrSystem(LIB_NAME);
            } catch (UnsatisfiedLinkError e) {
                // 再次兜底尝试常规搜索，便于外部 PATH/java.library.path 提供
                System.loadLibrary(LIB_NAME);
            }
            LOADED.set(true);
        }
    }
}

