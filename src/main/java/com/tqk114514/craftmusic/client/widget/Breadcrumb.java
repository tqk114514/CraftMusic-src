package com.tqk114514.craftmusic.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * 设置页顶部居中的层级路径提示（如"设置 > 歌词 > 悬浮歌词"）。
 *
 * 原先每个设置页都手抄三行：拼字符串、算居中 x、画文字。抽出来后各页面只需声明层级。
 */
public final class Breadcrumb {
    private static final int Y = 8;
    private static final String SEPARATOR = " > ";

    private Breadcrumb() {
    }

    /** 把若干翻译键拼成层级路径文本。 */
    public static String of(String... translationKeys) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < translationKeys.length; i++) {
            if (i > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(Component.translatable(translationKeys[i]).getString());
        }
        return sb.toString();
    }

    /** 居中绘制层级路径。 */
    public static void draw(GuiGraphicsExtractor gfx, Font font, int screenWidth, String... translationKeys) {
        String text = of(translationKeys);
        gfx.text(font, text, (screenWidth - font.width(text)) / 2, Y, 0xFFFFFFFF, false);
    }
}
