package com.tqk114514.craftmusic.client.settings.lyrics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import javax.annotation.Nonnull;

import com.tqk114514.craftmusic.client.ClientConfig;

public class FloatingLyricsPositionScreen extends Screen {
    private final Screen parent;
    private float posX;
    private float posY;
    private boolean dragging = false;
    private boolean positionDirty = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public FloatingLyricsPositionScreen(Screen parent) {
        super(Component.translatable("craftmusic.ui.floating_lyrics.position.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        posX = ClientConfig.getFloatingLyricsPosX();
        posY = ClientConfig.getFloatingLyricsPosY();
        int closeY = this.height - 30;
        // 完成按钮
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.done"), b -> {
            ClientConfig.setFloatingLyricsPos(posX, posY);
            Minecraft.getInstance().setScreen(parent);
        }).bounds(this.width / 2 - 100, closeY, 95, 20).build());
        // 吸附开关按钮
        addRenderableWidget(Button.builder(buildSnapLabel(), b -> {
            ClientConfig.setFloatingLyricsSnap(!ClientConfig.isFloatingLyricsSnap());
            b.setMessage(buildSnapLabel());
        }).bounds(this.width / 2 + 5, closeY, 95, 20).build());
    }

    private Component buildSnapLabel() {
        return Component.translatable(ClientConfig.isFloatingLyricsSnap() ? "craftmusic.ui.floating_lyrics.snap.on" : "craftmusic.ui.floating_lyrics.snap.off");
    }

    @Override
    public void extractRenderState(@Nonnull GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // 先绘制UI，最后绘制预览与边框，避免被后续模糊影响
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        float scale = ClientConfig.getFloatingLyricsFontScale();
        boolean outline = ClientConfig.isFloatingLyricsOutline();
        int color = ClientConfig.getFloatingLyricsColor();
        String rawText = com.tqk114514.craftmusic.CraftMusicClient.getCurrentFloatingLyricText();
        // 位置设置界面不受"悬浮歌词开关"影响：若当前有歌词则显示歌词；若无歌词则显示占位文本
        @Nonnull final String text;
        if (rawText == null || rawText.isBlank()) {
            Component sampleComponent = Component.translatable("craftmusic.ui.floating_lyrics.sample");
            String sampleText = sampleComponent != null ? sampleComponent.getString() : null;
            text = (sampleText != null && !sampleText.isBlank()) ? sampleText : "Sample Lyrics";
        } else {
            text = rawText;
        }
        int baseX = (int)(posX * this.width);
        int baseY = (int)(posY * this.height);
        // 吸附与边界：根据配置决定是否吸附
        int snap = 12;
        boolean useSnap = ClientConfig.isFloatingLyricsSnap();
        
        if (useSnap && Math.abs(baseX - 0) < snap) baseX = 0;
        if (useSnap && Math.abs(baseY - 0) < snap) baseY = 0;
        // 使用屏幕右边缘进行吸附判断，保持与实际渲染一致
        if (useSnap && Math.abs(baseX - this.width) < snap) baseX = this.width;
        if (useSnap && Math.abs(baseY - this.height) < snap) baseY = this.height;
        // 限制锚点在屏幕范围内
        if (baseX < 0) baseX = 0; if (baseX > this.width) baseX = this.width;
        if (baseY < 0) baseY = 0; if (baseY > this.height) baseY = this.height;
        // 使用默认字体进行预览
        var lyricsFont = this.font;
        int textW = (int)Math.ceil(lyricsFont.width(text) * scale);
        int textH = (int)Math.ceil(lyricsFont.lineHeight * scale);
        // 对齐：仅在开启吸附时根据靠边阈值采用左/右/底对齐；关闭时使用水平居中、顶对齐
        boolean nearLeft = useSnap && Math.abs(baseX - 0) < snap;
        boolean nearRight = useSnap && Math.abs(baseX - this.width) < snap;
        boolean nearTop = useSnap && Math.abs(baseY - 0) < snap;
        boolean nearBottom = useSnap && Math.abs(baseY - this.height) < snap;
        int drawX = baseX - (nearLeft ? 0 : (nearRight ? textW : textW / 2));
        int drawY = baseY - (nearTop ? 0 : (nearBottom ? textH : 0));
        // 位置设置界面总是渲染预览文本；全局渲染器已在该界面下被显式跳过，避免重复
        if (true) {
            var pose = gfx.pose();
            pose.pushMatrix();
            pose.translate(drawX, drawY);
            pose.scale(scale, scale);
            int a = (color >>> 24) & 0xFF;
            if (a <= 0) { pose.popMatrix(); return; }
            if (outline) {
                int outlineColor = (a << 24);
                gfx.text(lyricsFont, text, 1, 0, outlineColor, false);
                gfx.text(lyricsFont, text, -1, 0, outlineColor, false);
                gfx.text(lyricsFont, text, 0, 1, outlineColor, false);
                gfx.text(lyricsFont, text, 0, -1, outlineColor, false);
            }
            gfx.text(lyricsFont, text, 0, 0, color, false);
            pose.popMatrix();
        }
        // 可点区域描边（置于最上层）
        gfx.fill(drawX - 1, drawY - 1, drawX + textW + 1, drawY, 0x40FFFFFF);
        gfx.fill(drawX - 1, drawY + textH, drawX + textW + 1, drawY + textH + 1, 0x40FFFFFF);
        gfx.fill(drawX - 1, drawY, drawX, drawY + textH, 0x40FFFFFF);
        gfx.fill(drawX + textW, drawY, drawX + textW + 1, drawY + textH, 0x40FFFFFF);
        // 不再调用 super.render，已在顶部调用，避免边框被后续层级覆盖
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        // 先让子组件（按钮）处理点击；若未处理，再进入拖拽
        if (super.mouseClicked(event, doubleClick)) return true;
        if (button == 0) {
            float scale = ClientConfig.getFloatingLyricsFontScale();
            String text = com.tqk114514.craftmusic.CraftMusicClient.getCurrentFloatingLyricText();
            if (text == null || text.isBlank()) text = Component.translatable("craftmusic.ui.floating_lyrics.sample").getString();
            int baseX = (int)(posX * this.width);
            int baseY = (int)(posY * this.height);
            int w = (int)Math.ceil(this.font.width(text) * scale);
            int h = (int)Math.ceil(this.font.lineHeight * scale);
            
            int snap = 12;
            boolean useSnap = ClientConfig.isFloatingLyricsSnap();
            // 对锚点进行与渲染一致的吸附与边界限制（含上下）
            if (useSnap && Math.abs(baseX - 0) < snap) baseX = 0;
            if (useSnap && Math.abs(baseX - this.width) < snap) baseX = this.width;
            if (useSnap && Math.abs(baseY - 0) < snap) baseY = 0;
            if (useSnap && Math.abs(baseY - this.height) < snap) baseY = this.height;
            if (baseX < 0) baseX = 0; if (baseX > this.width) baseX = this.width;
            if (baseY < 0) baseY = 0; if (baseY > this.height) baseY = this.height;
            boolean nearLeft = useSnap && Math.abs(baseX - 0) < snap;
            boolean nearRight = useSnap && Math.abs(baseX - this.width) < snap;
            boolean nearTop = useSnap && Math.abs(baseY - 0) < snap;
            boolean nearBottom = useSnap && Math.abs(baseY - this.height) < snap;
            int drawX = baseX - (nearLeft ? 0 : (nearRight ? w : w / 2));
            int drawY = baseY - (nearTop ? 0 : (nearBottom ? h : 0));

            if (mouseX >= drawX && mouseX <= drawX + w && mouseY >= drawY && mouseY <= drawY + h) {
                dragging = true;
                dragOffsetX = (int)mouseX - drawX;
                dragOffsetY = (int)mouseY - drawY;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        if (dragging && button == 0) {
            int x = (int)mouseX - dragOffsetX;
            int y = (int)mouseY - dragOffsetY;
            float scale = ClientConfig.getFloatingLyricsFontScale();
            String text = com.tqk114514.craftmusic.CraftMusicClient.getCurrentFloatingLyricText();
            if (text == null || text.isBlank()) text = Component.translatable("craftmusic.ui.floating_lyrics.sample").getString();
            int w = (int)Math.ceil(this.font.width(text) * scale);
            int h = (int)Math.ceil(this.font.lineHeight * scale);
            int maxX = Math.max(0, this.width - w);
            int maxY = Math.max(0, this.height - h);
            int snap = 12;
            boolean useSnap = ClientConfig.isFloatingLyricsSnap();
            // 先对可拖拽矩形左上角做吸附与边界
            if (useSnap && Math.abs(x - 0) < snap) x = 0;
            if (useSnap && Math.abs(y - 0) < snap) y = 0;
            // 右/下吸附基于屏幕边缘（保持对齐判断一致）
            if (useSnap && Math.abs((this.width - w) - x) < snap) x = this.width - w;
            if (useSnap && Math.abs((this.height - h) - y) < snap) y = this.height - h;
            if (x < 0) x = 0; if (x > maxX) x = maxX;
            if (y < 0) y = 0; if (y > maxY) y = maxY;

            // 由最终矩形位置反推锚点（与实际渲染一致，按靠边阈值判断左右/上下对齐方式）
            int anchorX;
            if (useSnap && Math.abs((x + w) - this.width) < snap) {
                anchorX = this.width;           // 右吸附：锚点在屏幕最右
            } else if (useSnap && Math.abs(x - 0) < snap) {
                anchorX = 0;                    // 左吸附：锚点在屏幕最左
            } else {
                anchorX = x + w / 2;            // 其他：水平居中
            }
            int anchorY;
            if (useSnap && Math.abs((y + h) - this.height) < snap) {
                anchorY = this.height;          // 底部吸附：锚点在屏幕最下
            } else if (useSnap && Math.abs(y - 0) < snap) {
                anchorY = 0;                    // 顶部吸附：锚点在屏幕最上
            } else {
                anchorY = y;                    // 其他：顶对齐（与渲染一致）
            }
            posX = clamp((float)anchorX / this.width);
            posY = clamp((float)anchorY / this.height);
            // 只更新内存，落盘推迟到松手：本界面内全局悬浮歌词渲染已被跳过，
            // 拖拽途中写文件没有任何可见效果，却会每帧写一次磁盘。
            positionDirty = true;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        if (dragging && button == 0) {
            dragging = false;
            if (positionDirty) {
                positionDirty = false;
                ClientConfig.setFloatingLyricsPos(posX, posY);
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
