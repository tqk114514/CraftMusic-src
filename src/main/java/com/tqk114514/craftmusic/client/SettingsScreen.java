package com.tqk114514.craftmusic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {
    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("craftmusic.ui.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height / 6 + 20;
        // 歌词效果
        addRenderableWidget(Button.builder(buildLyricEffectsLabel(), b -> {
            boolean v = !ClientConfig.isLyricEffects();
            ClientConfig.setLyricEffects(v);
            b.setMessage(buildLyricEffectsLabel());
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        y += 24;
        // 悬浮歌词渲染范围
        addRenderableWidget(Button.builder(buildFloatingLyricsRenderLabel(), b -> {
            String cur = ClientConfig.getFloatingLyricsRender();
            String next = "GLOBAL".equals(cur) ? "WORLD" : "GLOBAL";
            ClientConfig.setFloatingLyricsRender(next);
            b.setMessage(buildFloatingLyricsRenderLabel());
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        y += 24;
        // 悬浮歌词位置
        addRenderableWidget(Button.builder(buildFloatingLyricsPositionLabel(), b -> {
            String cur = ClientConfig.getFloatingLyricsPosition();
            String next = switch (cur) {
                case "TOP_LEFT" -> "TOP_RIGHT";
                case "TOP_RIGHT" -> "BOTTOM_RIGHT";
                case "BOTTOM_RIGHT" -> "BOTTOM_LEFT";
                default -> "TOP_LEFT";
            };
            ClientConfig.setFloatingLyricsPosition(next);
            b.setMessage(buildFloatingLyricsPositionLabel());
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        // 底部关闭按钮
        int closeY = this.height - 30;
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.close"), b -> Minecraft.getInstance().setScreen(parent))
                .bounds(this.width / 2 - 100, closeY, 200, 20).build());
    }

    private Component buildLyricEffectsLabel() {
        String key = ClientConfig.isLyricEffects() ? "craftmusic.ui.lyric_effects.on" : "craftmusic.ui.lyric_effects.off";
        return Component.translatable(key);
    }

    private Component buildFloatingLyricsRenderLabel() {
        String scope = ClientConfig.getFloatingLyricsRender();
        String key = "GLOBAL".equals(scope) ? "craftmusic.ui.floating_lyrics.render.global" : "craftmusic.ui.floating_lyrics.render.world";
        return Component.translatable(key);
    }

    private Component buildFloatingLyricsPositionLabel() {
        String pos = ClientConfig.getFloatingLyricsPosition();
        String key = switch (pos) {
            case "BOTTOM_LEFT" -> "craftmusic.ui.floating_lyrics.position.bottom_left";
            case "TOP_RIGHT" -> "craftmusic.ui.floating_lyrics.position.top_right";
            case "BOTTOM_RIGHT" -> "craftmusic.ui.floating_lyrics.position.bottom_right";
            default -> "craftmusic.ui.floating_lyrics.position.top_left";
        };
        return Component.translatable(key);
    }
}

