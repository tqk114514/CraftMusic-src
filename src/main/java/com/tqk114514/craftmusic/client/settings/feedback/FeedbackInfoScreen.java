package com.tqk114514.craftmusic.client.settings.feedback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.tqk114514.craftmusic.client.widget.Breadcrumb;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

import javax.annotation.Nonnull;
import java.util.List;

public class FeedbackInfoScreen extends Screen {
    private final Screen parent;
    private static final String URL = "https://github.com/tqk114514/CraftMusic/issues/";
    private int contentLeft;
    private int contentWidth;
    private final String tailKey; // 面包屑末尾键："craftmusic.ui.feedback.bug" 或 "craftmusic.ui.feedback.suggest"

    public FeedbackInfoScreen(Screen parent, String tailKey) {
        super(Component.translatable("craftmusic.ui.feedback.info_title"));
        this.parent = parent;
        this.tailKey = tailKey;
    }

    @Override
    protected void init() {
        // 内容区域设定：限制最大宽度并水平居中，避免左重右轻
        contentWidth = Math.min(680, Math.max(260, this.width - 60));
        contentLeft = (this.width - contentWidth) / 2;
        int btnY = this.height - 30;

        int gap = 10;
        int btnW = 95;
        int total = btnW * 2 + gap;
        int left = contentLeft + (contentWidth - total) / 2;

        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.back"), b -> {
            Minecraft.getInstance().setScreen(parent);
        }).bounds(left, btnY, btnW, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.feedback.proceed"), b -> {
            Minecraft.getInstance().setScreen(new ConfirmLinkScreen(open -> {
                if (open) {
                    try { Util.getPlatform().openUri(URL); } catch (Throwable ignored) {}
                }
                Minecraft.getInstance().setScreen(parent);
            }, URL, true));
        }).bounds(left + btnW + gap, btnY, btnW, 20).build());
    }

    @Override
    public void extractRenderState(@Nonnull GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        // 面包屑：设置 > 反馈 > （提交BUG/提交建议）
        Breadcrumb.draw(gfx, this.font, this.width,
                "craftmusic.ui.settings", "craftmusic.ui.feedback.menu", tailKey);

        // 正文：居中面板+居中对齐
        int panelPad = 10;
        int textTop = this.height / 6 + 10;
        Component body = Component.translatable("craftmusic.ui.feedback_and_suggest.info");
        List<FormattedCharSequence> lines = this.font.split(body, contentWidth - panelPad * 2);
        int bodyHeight = lines.size() * (this.font.lineHeight + 2);
        int panelTop = textTop - panelPad;
        int panelBottom = textTop + bodyHeight + panelPad;
        int panelLeft = contentLeft - panelPad;
        int panelRight = contentLeft + contentWidth + panelPad;
        gfx.fill(panelLeft, panelTop, panelRight, panelBottom, 0x90000000);

        int y = textTop;
        for (FormattedCharSequence seq : lines) {
            int lineW = this.font.width(seq);
            int x = contentLeft + (contentWidth - lineW) / 2;
            gfx.text(this.font, seq, x, y, 0xDDDDDD, false);
            y += this.font.lineHeight + 2;
        }
    }
}


