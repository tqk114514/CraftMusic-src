package com.tqk114514.craftmusic.client.widget;

import com.tqk114514.craftmusic.audio.MiniaudioPlayer;
import com.tqk114514.craftmusic.client.ClientConfig;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 底部播放进度条，含拖动交互。
 *
 * 拖动只更新"待跳转位置"用于即时反馈（把手会跟着走），真正的 seek 推迟到松手时执行 ——
 * 避免拖动过程中不断打断解码。状态保留在本组件内，不再挂在 Screen 上。
 */
public final class SeekBarView {
    private static final int HEIGHT = 6;
    private static final int HIT_PADDING = 4;   // 命中区域上下各放宽 4px，方便点中
    private static final int MARGIN_LEFT = 2;   // 与列表等元素对齐
    private static final int MARGIN_RIGHT = 10;
    private static final int KNOB_HALF = 2;

    private int x;
    private int y;
    private int width;
    private boolean dragging;
    private int pendingSeekMs = -1;

    /** 按屏幕宽度计算几何；y 由调用方给出（通常贴着底部按钮上方）。 */
    public void layout(int screenWidth, int y) {
        this.x = MARGIN_LEFT;
        this.width = screenWidth - MARGIN_LEFT - MARGIN_RIGHT;
        this.y = y;
    }

    public int getY() {
        return y;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean containsPoint(double mouseX, double mouseY) {
        return mouseY >= y - HIT_PADDING && mouseY <= y + HEIGHT + HIT_PADDING
                && mouseX >= x && mouseX <= x + width;
    }

    public void extract(GuiGraphicsExtractor gfx, Font font, MiniaudioPlayer player) {
        int x0 = x;
        int x1 = x + width;
        gfx.fill(x0, y, x1, y + HEIGHT, 0x80000000);

        int len = (player != null) ? player.getLengthMs() : 0;
        int pos = (player != null) ? player.getPositionMs() : 0;
        if (dragging && pendingSeekMs >= 0) {
            pos = pendingSeekMs;
        }
        float pct = (len > 0) ? Math.min(1f, Math.max(0f, pos / (float) len)) : 0f;
        int knobX = x0 + Math.round(pct * width);
        gfx.fill(x0, y, knobX, y + HEIGHT, 0xFF00AAFF);
        gfx.fill(knobX - KNOB_HALF, y - KNOB_HALF, knobX + KNOB_HALF, y + HEIGHT + KNOB_HALF, 0xFFFFFFFF);

        String timeStr = formatTime(pos) + " / " + formatTime(len);
        gfx.text(font, timeStr, x1 - Math.max(60, font.width(timeStr)), y - 10, 0xFFFFFFFF, false);
    }

    public void beginDrag(int mouseX, int lengthMs) {
        dragging = true;
        update(mouseX, lengthMs);
    }

    public void updateDrag(int mouseX, int lengthMs) {
        if (dragging) {
            update(mouseX, lengthMs);
        }
    }

    /** 结束拖动并返回待跳转的毫秒数；若未产生有效值返回 -1。 */
    public int endDrag() {
        dragging = false;
        int value = pendingSeekMs;
        pendingSeekMs = -1;
        return value;
    }

    private void update(int mouseX, int lengthMs) {
        int rel = Math.max(0, Math.min(width, mouseX - x));
        pendingSeekMs = (lengthMs > 0) ? (int) ((rel / (float) width) * lengthMs) : 0;
    }

    private static String formatTime(int ms) {
        int totalSec = Math.max(0, ms / 1000);
        int m = totalSec / 60;
        int s = totalSec % 60;
        return String.format("%d:%02d", m, s);
    }
}
