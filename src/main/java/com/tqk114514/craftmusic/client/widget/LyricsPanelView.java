package com.tqk114514.craftmusic.client.widget;

import com.tqk114514.craftmusic.audio.MiniaudioPlayer;
import com.tqk114514.craftmusic.client.Lyrics;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

/**
 * 右侧歌词面板，含滚动与缩放两套动画状态机。
 *
 * 状态原先散落在 Screen 上（scrollPos / scrollVel / scaleCurrentIndex / noLyricsActive …），
 * 现在全部归本组件：换曲时由 {@link #setLyrics} 统一重置，避免遗漏某个字段导致动画残留在上一首的状态。
 *
 * 滚动用临界阻尼弹簧逼近目标行，跳跃超过 3 行时直接对齐（避免长距离拖尾）。
 */
public final class LyricsPanelView {
    private static final float SCROLL_SPRING_K = 60f;                                        // 弹性系数
    private static final float SCROLL_DAMP_C = (float) (2.0 * Math.sqrt(SCROLL_SPRING_K));   // 临界阻尼
    private static final float MAX_DT = 0.05f;      // 防止卡顿帧跳跃
    private static final int SCALE_ANIM_MS = 180;
    private static final float SCALE_BOOST = 0.08f; // 当前行放大量
    private static final int PADDING = 8;           // 上下内边距
    private static final int GAP = 10;              // 列表与歌词面板的间隔
    private static final int RIGHT_PADDING = 10;
    private static final int MAX_JUMP_LINES = 3;

    private int top;
    private int bottom;
    private Lyrics lyrics = Lyrics.empty();

    private float scrollPos = -1f;
    private float scrollVel = 0f;
    private long lastUpdateMs = 0L;
    private int scaleCurrentIndex = -1;
    private int scalePrevIndex = -1;
    private long scaleAnimStartMs = 0L;
    private boolean noLyricsActive = false;
    private long noLyricsAnimStartMs = 0L;

    public void layout(int top, int height) {
        this.top = top;
        this.bottom = top + height;
    }

    /** 切换曲目时调用，会一并重置所有动画状态。 */
    public void setLyrics(Lyrics newLyrics) {
        this.lyrics = (newLyrics != null) ? newLyrics : Lyrics.empty();
        scrollPos = -1f;
        scrollVel = 0f;
        lastUpdateMs = 0L;
        scaleCurrentIndex = -1;
        scalePrevIndex = -1;
        noLyricsActive = false;
    }

    public void extract(GuiGraphicsExtractor gfx, Font font, MiniaudioPlayer player, int screenWidth, boolean effectsEnabled) {
        int leftWidth = (screenWidth - RIGHT_PADDING - GAP) / 2;
        int x0 = leftWidth + GAP;
        int x1 = screenWidth - RIGHT_PADDING;
        gfx.fill(x0, top, x1, bottom, 0x90000000);

        List<Lyrics.Line> lines = (lyrics != null) ? lyrics.getLines() : Collections.emptyList();
        int midX = (x0 + x1) / 2;
        int centerY = top + PADDING + (bottom - top - 2 * PADDING) / 2 - font.lineHeight / 2;

        if (lines.isEmpty()) {
            drawNoLyrics(gfx, font, midX, centerY, effectsEnabled);
            return;
        }
        noLyricsActive = false;

        int curMs = (player != null) ? player.getPositionMs() : 0;
        int curIdx = Lyrics.findLineIndexAt(lines, curMs);
        long now = System.currentTimeMillis();

        updateScroll(now, curIdx, lines.size(), effectsEnabled);
        updateScale(now, curIdx, effectsEnabled);

        float drawCenterIndex = (scrollPos < 0f) ? (curIdx < 0 ? 0f : curIdx) : scrollPos;
        int lineH = font.lineHeight + 2;
        int visiblePx = Math.max(0, bottom - top - 2 * PADDING);
        int half = Math.max(1, visiblePx / lineH + 1) / 2;
        int firstIdx = Math.max(0, (int) Math.floor(drawCenterIndex) - half);
        int lastIdx = Math.min(lines.size() - 1, (int) Math.ceil(drawCenterIndex) + half);
        int textTopBound = top + PADDING;
        int textBottomBound = bottom - PADDING - font.lineHeight;

        for (int i = firstIdx; i <= lastIdx; i++) {
            int y = Math.round(centerY + (i - drawCenterIndex) * lineH);
            if (y < textTopBound || y > textBottomBound) {
                continue;
            }
            String text = lines.get(i).text;
            int color = (i == curIdx) ? 0xFFFFFFFF : 0xFFAAAAAA;

            if (!effectsEnabled) {
                gfx.text(font, text, midX - font.width(text) / 2, y, color, false);
                continue;
            }
            float eased = easeInOutCubic(progress(now, scaleAnimStartMs));
            float scale;
            if (i == scaleCurrentIndex) {
                scale = 1.0f + SCALE_BOOST * eased;
            } else if (i == scalePrevIndex) {
                scale = 1.0f + SCALE_BOOST - SCALE_BOOST * eased;
            } else {
                scale = 1.0f;
            }
            drawCenteredScaledString(gfx, font, text, midX, y, scale, color);
        }
    }

    private void drawNoLyrics(GuiGraphicsExtractor gfx, Font font, int midX, int centerY, boolean effectsEnabled) {
        String txt = Component.translatable("craftmusic.ui.no_lyrics_or_instrumental").getString();
        if (!effectsEnabled) {
            noLyricsActive = false;
            gfx.text(font, txt, midX - font.width(txt) / 2, centerY, 0xFFFFFFFF, false);
            return;
        }
        long now = System.currentTimeMillis();
        if (!noLyricsActive) {
            noLyricsActive = true;
            noLyricsAnimStartMs = now;
        }
        float scale = 1.0f + SCALE_BOOST * easeInOutCubic(progress(now, noLyricsAnimStartMs));
        drawCenteredScaledString(gfx, font, txt, midX, centerY, scale, 0xFFFFFFFF);
    }

    private void updateScroll(long now, int curIdx, int lineCount, boolean effectsEnabled) {
        float target = (curIdx < 0) ? 0f : Math.min(curIdx, lineCount - 1);
        if (!effectsEnabled) {
            scrollPos = target;
            scrollVel = 0f;
            lastUpdateMs = now;
            return;
        }
        if (lastUpdateMs == 0L || scrollPos < 0f) {
            scrollPos = target;
            scrollVel = 0f;
            lastUpdateMs = now;
            return;
        }
        if (Math.abs(target - scrollPos) > MAX_JUMP_LINES) {
            scrollPos = target;
            scrollVel = 0f;
        } else {
            float dt = Math.min(MAX_DT, (now - lastUpdateMs) / 1000f);
            float accel = SCROLL_SPRING_K * (target - scrollPos) - SCROLL_DAMP_C * scrollVel;
            scrollVel += accel * dt;
            scrollPos += scrollVel * dt;
        }
        lastUpdateMs = now;
    }

    private void updateScale(long now, int curIdx, boolean effectsEnabled) {
        if (effectsEnabled) {
            if (scaleCurrentIndex != curIdx) {
                scalePrevIndex = scaleCurrentIndex;
                scaleCurrentIndex = curIdx;
                scaleAnimStartMs = now;
            }
        } else {
            scaleCurrentIndex = -1;
            scalePrevIndex = -1;
        }
    }

    private static float progress(long now, long startMs) {
        return Math.max(0f, Math.min(1f, (now - startMs) / (float) SCALE_ANIM_MS));
    }

    /** easeInOutCubic */
    private static float easeInOutCubic(float t) {
        return (t < 0.5f) ? (4f * t * t * t) : (1f - (float) Math.pow(-2f * t + 2f, 3f) / 2f);
    }

    private static void drawCenteredScaledString(GuiGraphicsExtractor gfx, Font font, String text, int midX, int y, float scale, int argb) {
        if (scale <= 0f) {
            return;
        }
        if (Math.abs(scale - 1f) < 0.001f) {
            gfx.text(font, text, midX - font.width(text) / 2, y, argb, false);
            return;
        }
        var pose = gfx.pose();
        pose.pushMatrix();
        float tx = midX - font.width(text) * scale / 2f;
        pose.translate(tx, y);
        pose.scale(scale, scale);
        gfx.text(font, text, 0, 0, argb, false);
        pose.popMatrix();
    }
}
