package com.tqk114514.craftmusic.client.settings.feedback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

public class FeedbackMenuScreen extends Screen {
    private final Screen parent;

    public FeedbackMenuScreen(Screen parent) {
        super(Component.translatable("craftmusic.ui.feedback.menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height / 6 + 20;
        // 提交 BUG -> 说明页面
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.feedback.bug"), btn -> {
            Minecraft.getInstance().setScreen(new FeedbackInfoScreen(this, "craftmusic.ui.feedback.bug"));
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        y += 24;
        // 提交建议（同流程，使用同一 Issues 页面）
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.feedback.suggest"), btn -> {
            Minecraft.getInstance().setScreen(new FeedbackInfoScreen(this, "craftmusic.ui.feedback.suggest"));
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        // 关闭
        int closeY = this.height - 30;
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.close"), btn -> Minecraft.getInstance().setScreen(parent))
                .bounds(this.width / 2 - 100, closeY, 200, 20).build());
    }

    @Override
    public void extractRenderState(@Nonnull net.minecraft.client.gui.GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        String breadcrumb = Component.translatable("craftmusic.ui.settings").getString()
                + " > " + Component.translatable("craftmusic.ui.feedback.menu").getString();
        int x = (this.width - this.font.width(breadcrumb)) / 2;
        gfx.text(this.font, breadcrumb, x, 8, 0xFFFFFF, false);
    }
}


