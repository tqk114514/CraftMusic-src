package com.tqk114514.craftmusic.client.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import javax.annotation.Nonnull;

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
        // 频谱可视化 - Minecraft 1.21.2+ 暂不支持（GUI渲染变化）
        var spectrumBtn = Button.builder(buildSpectrumLabel(), b -> {
            // Minecraft 1.21.2+ 版本禁用
        }).bounds(this.width / 2 - 100, y, 200, 20).build();
        spectrumBtn.active = false; // 禁用按钮
        spectrumBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("craftmusic.ui.spectrum.disabled")
        ));
        addRenderableWidget(spectrumBtn);

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
        // 1.21.2 强制显示为关闭状态
        return Component.translatable("craftmusic.ui.spectrum.off");
    }

    @Override
    public void render(@Nonnull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        String breadcrumb = Component.translatable("craftmusic.ui.settings").getString();
        int x = (this.width - this.font.width(breadcrumb)) / 2;
        gfx.drawString(this.font, breadcrumb, x, 8, 0xFFFFFF, false);
    }
}

