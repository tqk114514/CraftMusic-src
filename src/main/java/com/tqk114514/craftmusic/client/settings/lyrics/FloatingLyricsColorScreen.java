package com.tqk114514.craftmusic.client.settings.lyrics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

import com.tqk114514.craftmusic.client.ClientConfig;

public class FloatingLyricsColorScreen extends Screen {
    private final Screen parent;
    private OpacitySlider opacitySlider;
    private FloatingLyricsSettingsScreen.ChannelSlider redSlider;
    private FloatingLyricsSettingsScreen.ChannelSlider greenSlider;
    private FloatingLyricsSettingsScreen.ChannelSlider blueSlider;

    public FloatingLyricsColorScreen(Screen parent) {
        super(Component.translatable("craftmusic.ui.floating_lyrics.color.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height / 6 + 20;
        int color = ClientConfig.getFloatingLyricsColor();
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = (color) & 0xFF;
        // 不透明度（百分比 0-100）
        int pct = Math.round(a * 100f / 255f);
        opacitySlider = new OpacitySlider(this.width / 2 - 100, y, 200, 20, pct);
        addRenderableWidget(opacitySlider);
        y += 24;
        redSlider = new FloatingLyricsSettingsScreen.ChannelSlider(this.width / 2 - 100, y, 200, 20, "craftmusic.ui.color.red", r);
        addRenderableWidget(redSlider);
        y += 24;
        greenSlider = new FloatingLyricsSettingsScreen.ChannelSlider(this.width / 2 - 100, y, 200, 20, "craftmusic.ui.color.green", g);
        addRenderableWidget(greenSlider);
        y += 24;
        blueSlider = new FloatingLyricsSettingsScreen.ChannelSlider(this.width / 2 - 100, y, 200, 20, "craftmusic.ui.color.blue", b);
        addRenderableWidget(blueSlider);
        y += 24;

        // 预设按钮
        int rowX = this.width / 2 - 100;
        int btnW = 36;
        int gap = 4;
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.color.preset.white"), btn -> applyPreset(0xFFFFFFFF))
                .bounds(rowX + 0 * (btnW + gap), y, btnW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.color.preset.yellow"), btn -> applyPreset((getAlphaFromPercent() << 24) | 0x00FFFF00))
                .bounds(rowX + 1 * (btnW + gap), y, btnW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.color.preset.cyan"), btn -> applyPreset((getAlphaFromPercent() << 24) | 0x0000FFFF))
                .bounds(rowX + 2 * (btnW + gap), y, btnW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.color.preset.lime"), btn -> applyPreset((getAlphaFromPercent() << 24) | 0x0000FF00))
                .bounds(rowX + 3 * (btnW + gap), y, btnW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.color.preset.pink"), btn -> applyPreset((getAlphaFromPercent() << 24) | 0x00FF66CC))
                .bounds(rowX + 4 * (btnW + gap), y, btnW, 20).build());

        // 关闭
        int closeY = this.height - 30;
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.close"), btn -> Minecraft.getInstance().setScreen(parent))
                .bounds(this.width / 2 - 100, closeY, 200, 20).build());
    }

    private void applyPreset(int argb) {
        int a = getAlphaFromPercent();
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = (argb) & 0xFF;
        if (opacitySlider != null) opacitySlider.setPercent(Math.round(a * 100f / 255f));
        if (redSlider != null) redSlider.setIntValue(r);
        if (greenSlider != null) greenSlider.setIntValue(g);
        if (blueSlider != null) blueSlider.setIntValue(b);
        saveColorFromSliders();
    }

    void saveColorFromSliders() {
        int a = getAlphaFromPercent();
        int r = redSlider.getIntValue();
        int g = greenSlider.getIntValue();
        int b = blueSlider.getIntValue();
        int argb = (a << 24) | (r << 16) | (g << 8) | b;
        ClientConfig.setFloatingLyricsColor(argb);
    }

    private int getAlphaFromPercent() {
        int pct = (opacitySlider != null) ? opacitySlider.getPercent() : 100;
        if (pct <= 0) return 0;
        if (pct >= 100) return 255;
        // 提升低端可见度的非线性映射（伽马校正）
        float gamma = 0.6f;
        float t = (float)Math.pow(pct / 100f, gamma);
        int a = Math.round(t * 255f);
        if (a < 1) a = 1; // 非零百分比至少有 1 的 alpha
        return a;
    }

    @Override
    public void extractRenderState(@Nonnull net.minecraft.client.gui.GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(gfx, mouseX, mouseY, partialTick);
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        String breadcrumb = Component.translatable("craftmusic.ui.settings").getString()
                + " > " + Component.translatable("craftmusic.ui.lyrics.settings").getString()
                + " > " + Component.translatable("craftmusic.ui.floating_lyrics.settings").getString()
                + " > " + Component.translatable("craftmusic.ui.floating_lyrics.color.settings").getString();
        int xbc = (this.width - this.font.width(breadcrumb)) / 2;
        gfx.text(this.font, breadcrumb, xbc, 8, 0xFFFFFF, false);
        // 示例文本
        String sample = Component.translatable("craftmusic.ui.floating_lyrics.sample").getString();
        int color = ClientConfig.getFloatingLyricsColor();
        int y = this.height / 6 + 20 + 24 * 5 + 6;
        int x = this.width / 2 - this.font.width(sample) / 2;
        if (ClientConfig.isFloatingLyricsOutline()) {
            int shadow = 0xFF000000;
            gfx.text(this.font, sample, x + 1, y, shadow, false);
            gfx.text(this.font, sample, x - 1, y, shadow, false);
            gfx.text(this.font, sample, x, y + 1, shadow, false);
            gfx.text(this.font, sample, x, y - 1, shadow, false);
        }
        gfx.text(this.font, sample, x, y, color, false);
    }
}

class OpacitySlider extends AbstractSliderButton {
    private int percent; // 0..100
    OpacitySlider(int x, int y, int w, int h, int initialPercent) {
        super(x, y, w, h, Component.literal(""), Math.max(0.0, Math.min(1.0, initialPercent / 100.0)));
        this.percent = initialPercent;
        updateMessage();
    }
    int getPercent() { return percent; }
    void setPercent(int p) { this.percent = Math.max(0, Math.min(100, p)); this.value = this.percent / 100.0; updateMessage(); }
    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal(Component.translatable("craftmusic.ui.color.opacity").getString() + ": " + percent + "%"));
    }
    @Override
    protected void applyValue() {
        this.percent = (int)Math.round(this.value * 100.0);
        updateMessage();
        if (Minecraft.getInstance().screen instanceof FloatingLyricsColorScreen s) { s.saveColorFromSliders(); }
    }
}


