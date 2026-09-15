package com.tqk114514.craftmusic.client.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import javax.annotation.Nonnull;

import com.tqk114514.craftmusic.client.ClientConfig;
import com.tqk114514.craftmusic.client.settings.feedback.FeedbackMenuScreen;

public class SettingsScreen extends Screen {
    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("craftmusic.ui.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height / 6 + 20;
        // 歌词设置（二级菜单）
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.lyrics.settings"), b -> {
            Minecraft.getInstance().setScreen(new LyricsSettingsScreen(this));
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        y += 24;
        // 频谱可视化 开/关（保留）
        addRenderableWidget(Button.builder(buildSpectrumLabel(), b -> {
            boolean v = !ClientConfig.isSpectrumEnabled();
            ClientConfig.setSpectrumEnabled(v);
            b.setMessage(buildSpectrumLabel());
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        y += 24;
        // 反馈（二级菜单）
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.feedback"), b -> {
            Minecraft.getInstance().setScreen(new FeedbackMenuScreen(this));
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        // 底部关闭按钮
        int closeY = this.height - 30;
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.close"), b -> Minecraft.getInstance().setScreen(parent))
                .bounds(this.width / 2 - 100, closeY, 200, 20).build());
    }

    // 移除原“位置设置”标签方法

    private Component buildSpectrumLabel() {
        return Component.translatable(ClientConfig.isSpectrumEnabled() ? "craftmusic.ui.spectrum.on" : "craftmusic.ui.spectrum.off");
    }

    @Override
    public void extractRenderState(@Nonnull GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        String breadcrumb = Component.translatable("craftmusic.ui.settings").getString();
        int x = (this.width - this.font.width(breadcrumb)) / 2;
        gfx.text(this.font, breadcrumb, x, 8, 0xFFFFFF, false);
    }
}

