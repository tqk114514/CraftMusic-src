package com.tqk114514.craftmusic.client.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

import com.tqk114514.craftmusic.client.ClientConfig;
import com.tqk114514.craftmusic.client.settings.lyrics.FloatingLyricsSettingsScreen;
import com.tqk114514.craftmusic.util.NullSafetyUtils;

public class LyricsSettingsScreen extends Screen {
    private final Screen parent;

    public LyricsSettingsScreen(Screen parent) {
        super(NullSafetyUtils.safeTranslatable("craftmusic.ui.lyrics.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height / 6 + 20;

        // 歌词效果 开/关
        addRenderableWidget(NullSafetyUtils.safeButton(Button.builder(buildLyricEffectsLabel(), btn -> {
            boolean v = !ClientConfig.isLyricEffects();
            ClientConfig.setLyricEffects(v);
            btn.setMessage(buildLyricEffectsLabel());
        }).bounds(this.width / 2 - 100, y, 200, 20)));

        y += 24;
        // 悬浮歌词渲染（GLOBAL/WORLD）
        addRenderableWidget(NullSafetyUtils.safeButton(Button.builder(buildFloatingLyricsRenderLabel(), btn -> {
            String cur = ClientConfig.getFloatingLyricsRender();
            String next = "GLOBAL".equals(cur) ? "WORLD" : "GLOBAL";
            ClientConfig.setFloatingLyricsRender(next);
            btn.setMessage(buildFloatingLyricsRenderLabel());
        }).bounds(this.width / 2 - 100, y, 200, 20)));

        y += 24;
        // 悬浮歌词设置（位置/颜色等）
        addRenderableWidget(NullSafetyUtils.safeButton(Button.builder(NullSafetyUtils.safeTranslatable("craftmusic.ui.floating_lyrics.settings"), btn -> {
            Minecraft.getInstance().setScreen(new FloatingLyricsSettingsScreen(this));
        }).bounds(this.width / 2 - 100, y, 200, 20)));

        // 关闭
        int closeY = this.height - 30;
        addRenderableWidget(NullSafetyUtils.safeButton(Button.builder(NullSafetyUtils.safeTranslatable("craftmusic.ui.close"), btn -> Minecraft.getInstance().setScreen(parent))
                .bounds(this.width / 2 - 100, closeY, 200, 20)));
    }

    @Nonnull
    private Component buildLyricEffectsLabel() {
        String key = ClientConfig.isLyricEffects() ? "craftmusic.ui.lyric_effects.on" : "craftmusic.ui.lyric_effects.off";
        return NullSafetyUtils.safeTranslatable(key);
    }

    @Nonnull
    private Component buildFloatingLyricsRenderLabel() {
        String scope = ClientConfig.getFloatingLyricsRender();
        String key = "GLOBAL".equals(scope) ? "craftmusic.ui.floating_lyrics.render.global" : "craftmusic.ui.floating_lyrics.render.world";
        return NullSafetyUtils.safeTranslatable(key);
    }

    @Override
    public void render(@Nonnull net.minecraft.client.gui.GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        String breadcrumb = Component.translatable("craftmusic.ui.settings").getString()
                + " > " + Component.translatable("craftmusic.ui.lyrics.settings").getString();
        int x = (this.width - this.font.width(breadcrumb)) / 2;
        gfx.drawString(this.font, breadcrumb, x, 8, 0xFFFFFF, false);
    }
}


