package com.tqk114514.craftmusic.client.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.tqk114514.craftmusic.client.widget.Breadcrumb;

import javax.annotation.Nonnull;

import com.tqk114514.craftmusic.client.ClientConfig;
import com.tqk114514.craftmusic.client.settings.lyrics.FloatingLyricsSettingsScreen;

public class LyricsSettingsScreen extends Screen {
    private final Screen parent;

    public LyricsSettingsScreen(Screen parent) {
        super(Component.translatable("craftmusic.ui.lyrics.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height / 6 + 20;

        // 歌词效果 开/关
        addRenderableWidget(Button.builder(buildLyricEffectsLabel(), btn -> {
            boolean v = !ClientConfig.isLyricEffects();
            ClientConfig.setLyricEffects(v);
            btn.setMessage(buildLyricEffectsLabel());
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        y += 24;
        // 悬浮歌词渲染（GLOBAL/WORLD）
        addRenderableWidget(Button.builder(buildFloatingLyricsRenderLabel(), btn -> {
            String cur = ClientConfig.getFloatingLyricsRender();
            String next = "GLOBAL".equals(cur) ? "WORLD" : "GLOBAL";
            ClientConfig.setFloatingLyricsRender(next);
            btn.setMessage(buildFloatingLyricsRenderLabel());
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        y += 24;
        // 悬浮歌词设置（位置/颜色等）
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.floating_lyrics.settings"), btn -> {
            Minecraft.getInstance().setScreen(new FloatingLyricsSettingsScreen(this));
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        // 关闭
        int closeY = this.height - 30;
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.close"), btn -> Minecraft.getInstance().setScreen(parent))
                .bounds(this.width / 2 - 100, closeY, 200, 20).build());
    }

    @Nonnull
    private Component buildLyricEffectsLabel() {
        String key = ClientConfig.isLyricEffects() ? "craftmusic.ui.lyric_effects.on" : "craftmusic.ui.lyric_effects.off";
        return Component.translatable(key);
    }

    @Nonnull
    private Component buildFloatingLyricsRenderLabel() {
        String scope = ClientConfig.getFloatingLyricsRender();
        String key = "GLOBAL".equals(scope) ? "craftmusic.ui.floating_lyrics.render.global" : "craftmusic.ui.floating_lyrics.render.world";
        return Component.translatable(key);
    }

    @Override
    public void extractRenderState(@Nonnull net.minecraft.client.gui.GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        Breadcrumb.draw(gfx, this.font, this.width,
                "craftmusic.ui.settings", "craftmusic.ui.lyrics.settings");
    }
}
