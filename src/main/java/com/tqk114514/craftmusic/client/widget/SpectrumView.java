package com.tqk114514.craftmusic.client.widget;

import com.tqk114514.craftmusic.audio.MiniaudioPlayer;
import com.tqk114514.craftmusic.client.ClientConfig;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 屏幕底缘的频谱柱状图。
 *
 * 频谱数据按约 30FPS 的实际采样率获取（而非每帧都取），其余帧复用上一次结果 ——
 * native 侧每取一次要做一次 FFT，逐帧取纯属浪费。
 */
public final class SpectrumView {
    private static final int BANDS = 64;
    private static final long FETCH_INTERVAL_MS = 33; // ~30FPS
    private static final int MARGIN_LEFT = 2;   // 与列表等元素对齐
    private static final int MARGIN_RIGHT = 10;
    private static final int MARGIN_BOTTOM = 4;

    private final float[] buffer = new float[BANDS];
    private long lastFetchMs = 0L;

    /**
     * 绘制频谱。振幅随当前音量缩放，但不改变最大高度。
     */
    public void extract(GuiGraphicsExtractor gfx, MiniaudioPlayer player, int screenWidth, int screenHeight) {
        long now = System.currentTimeMillis();
        if (now - lastFetchMs >= FETCH_INTERVAL_MS) {
            if (player != null) {
                try {
                    player.getSpectrum(buffer, BANDS);
                } catch (Throwable ignored) {
                    // 频谱是纯装饰，取不到就沿用上一次的数据
                }
            }
            lastFetchMs = now;
        }

        int x0 = MARGIN_LEFT;
        int x1 = screenWidth - MARGIN_RIGHT;
        int yBottom = screenHeight - MARGIN_BOTTOM;
        int width = x1 - x0;
        int barGap = Math.max(1, width / (BANDS * 8));
        int barW = Math.max(1, (width - (BANDS - 1) * barGap) / BANDS);

        float volScale = (player != null)
                ? Math.max(0f, Math.min(1f, player.getVolume()))
                : ClientConfig.getVolume();

        for (int i = 0; i < BANDS; i++) {
            float v = buffer[i] * volScale;
            if (v < 0f) v = 0f;
            if (v > 1f) v = 1f;
            int h = (int) (v * (yBottom - 4));
            int bx = x0 + i * (barW + barGap);
            gfx.fill(bx, yBottom - h, bx + barW, yBottom, 0xFFFFFFFF);
        }
    }
}
