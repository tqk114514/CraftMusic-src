package com.tqk114514.craftmusic.client.widget;

import com.tqk114514.craftmusic.audio.MiniaudioPlayer;
import com.tqk114514.craftmusic.client.ClientConfig;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * 底部音量条，含拖动交互。
 *
 * 与进度条不同：音量拖动时**立即生效**（改的是播放增益并写回配置），
 * 因为用户需要边拖边听到变化；进度条则要把 seek 推迟到松手。
 */
public final class VolumeBarView {
    private static final int HEIGHT = 6;
    private static final int HIT_PADDING = 4;   // 命中区域上下各放宽 4px
    private static final int KNOB_HALF = 2;

    private int x;
    private int y;
    private int width;
    private boolean dragging;

    public void layout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean containsPoint(double mouseX, double mouseY) {
        return mouseY >= y - HIT_PADDING && mouseY <= y + HEIGHT + HIT_PADDING
                && mouseX >= x && mouseX <= x + width;
    }

    public void extract(GuiGraphicsExtractor gfx, Font font) {
        int x0 = x;
        int x1 = x + width;
        gfx.fill(x0, y, x1, y + HEIGHT, 0x80000000);

        float vol = ClientConfig.getVolume();
        int filled = x0 + Math.round(vol * width);
        gfx.fill(x0, y, filled, y + HEIGHT, 0xFFFFB000);
        gfx.fill(filled - KNOB_HALF, y - KNOB_HALF, filled + KNOB_HALF, y + HEIGHT + KNOB_HALF, 0xFFFFFFFF);

        // 左侧标签，右侧百分比
        String label = Component.translatable("craftmusic.ui.volume").getString();
        String percent = Math.round(vol * 100) + "%";
        int textY = y - 9;
        gfx.text(font, label, x0, textY, 0xFFFFFFFF, false);
        gfx.text(font, percent, x1 - font.width(percent), textY, 0xFFFFFFFF, false);
    }

    public void beginDrag(int mouseX, MiniaudioPlayer player) {
        dragging = true;
        update(mouseX, player);
    }

    public void updateDrag(int mouseX, MiniaudioPlayer player) {
        if (dragging) {
            update(mouseX, player);
        }
    }

    public void endDrag() {
        dragging = false;
    }

    private void update(int mouseX, MiniaudioPlayer player) {
        int rel = Math.max(0, Math.min(width, mouseX - x));
        float v = rel / (float) width;
        if (player != null && player.isOutputReady()) {
            player.setVolume(v);
        }
        ClientConfig.setVolume(v);
    }
}
