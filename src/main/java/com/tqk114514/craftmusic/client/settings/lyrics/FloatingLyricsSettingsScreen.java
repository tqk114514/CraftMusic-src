package com.tqk114514.craftmusic.client.settings.lyrics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import javax.annotation.Nonnull;

import com.tqk114514.craftmusic.client.ClientConfig;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;

public class FloatingLyricsSettingsScreen extends Screen {
    private final Screen parent;
    private AbstractSliderButton fontScaleSlider;

    public FloatingLyricsSettingsScreen(Screen parent) {
        super(Component.translatable("craftmusic.ui.floating_lyrics.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height / 6 + 20;
        // 字号滑动条（原版样式），范围 0.5x - 3.0x，实时保存
        float cur = ClientConfig.getFloatingLyricsFontScale();
        double min = 0.5;
        double max = 3.0;
        double norm = (cur - min) / (max - min);
        if (norm < 0) norm = 0; if (norm > 1) norm = 1;
        fontScaleSlider = new AbstractSliderButton(this.width / 2 - 100, y, 200, 20, Component.literal(""), norm) {
            private void updateLabel() {
                double v = min + this.value * (max - min);
                String txt = String.format(Locale.ROOT, "%s: %.2f", Component.translatable("craftmusic.ui.floating_lyrics.font_scale").getString(), v);
                this.setMessage(Component.literal(txt));
            }
            @Override
            protected void updateMessage() { updateLabel(); }
            @Override
            protected void applyValue() {
                double v = min + this.value * (max - min);
                ClientConfig.setFloatingLyricsFontScale((float)v);
                updateLabel();
            }
        };
        // 初始标签
        fontScaleSlider.setMessage(Component.literal(String.format(Locale.ROOT, "%s: %.2f", Component.translatable("craftmusic.ui.floating_lyrics.font_scale").getString(), cur)));
        addRenderableWidget(fontScaleSlider);

        y += 24;
        // 描边开关
        addRenderableWidget(Button.builder(buildOutlineLabel(), btn -> {
            ClientConfig.setFloatingLyricsOutline(!ClientConfig.isFloatingLyricsOutline());
            btn.setMessage(buildOutlineLabel());
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        y += 24;
        // 颜色设置（二级菜单）
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.floating_lyrics.color.settings"), btn -> {
            Minecraft.getInstance().setScreen(new FloatingLyricsColorScreen(this));
        }).bounds(this.width / 2 - 100, y, 200, 20).build());
        y += 24;
        // 位置设置（三级菜单：可视化拖拽）
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.floating_lyrics.position.settings"), btn -> {
            Minecraft.getInstance().setScreen(new FloatingLyricsPositionScreen(this));
        }).bounds(this.width / 2 - 100, y, 200, 20).build());

        // 底部关闭（保存已实时进行）
        int closeY = this.height - 30;
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.close"), btn -> Minecraft.getInstance().setScreen(parent))
                .bounds(this.width / 2 - 100, closeY, 200, 20).build());
    }

    private Component buildOutlineLabel() {
        return Component.translatable(ClientConfig.isFloatingLyricsOutline() ? "craftmusic.ui.floating_lyrics.outline.on" : "craftmusic.ui.floating_lyrics.outline.off");
    }

    static class ChannelSlider extends AbstractSliderButton {
        private final String labelKey;
        ChannelSlider(int x, int y, int w, int h, String labelKey, int initial) {
            super(x, y, w, h, Component.literal(""), Math.max(0.0, Math.min(1.0, initial / 255.0)));
            this.labelKey = labelKey;
            updateMessage();
        }
        int getIntValue() { return (int)Math.round(this.value * 255); }
        void setIntValue(int v) { this.value = Math.max(0.0, Math.min(1.0, v / 255.0)); updateMessage(); }
        @Override
        protected void updateMessage() {
            int v = getIntValue();
            this.setMessage(Component.literal(Component.translatable(labelKey).getString() + ": " + v));
        }
        @Override
        protected void applyValue() {
            // 保存交由外部持有者在 onRelease 或下一帧调用，这里直接不做
        }
        @Override
        public void onRelease(net.minecraft.client.input.MouseButtonEvent event) {
            // 交由父类处理，随后由外层读取值保存
            super.onRelease(event);
            if (Minecraft.getInstance().screen instanceof FloatingLyricsColorScreen s) { s.saveColorFromSliders(); }
        }
        @Override
        public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            super.onClick(event, doubleClick);
            if (Minecraft.getInstance().screen instanceof FloatingLyricsColorScreen s) { s.saveColorFromSliders(); }
        }
        @Override
        public void onDrag(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
            super.onDrag(event, dx, dy);
            if (Minecraft.getInstance().screen instanceof FloatingLyricsColorScreen s) { s.saveColorFromSliders(); }
        }
    }

    @Override
    public void extractRenderState(@Nonnull net.minecraft.client.gui.GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        String breadcrumb = Component.translatable("craftmusic.ui.settings").getString()
                + " > " + Component.translatable("craftmusic.ui.lyrics.settings").getString()
                + " > " + Component.translatable("craftmusic.ui.floating_lyrics.settings").getString();
        int x = (this.width - this.font.width(breadcrumb)) / 2;
        gfx.text(this.font, breadcrumb, x, 8, 0xFFFFFF, false);
    }
}

