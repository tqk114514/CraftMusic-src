package com.tqk114514.craftmusic.client.widget;

import com.tqk114514.craftmusic.client.MusicLibrary;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

import java.nio.file.Path;
import java.util.List;

/**
 * 曲目列表。
 *
 * 选中索引归本组件所有 —— 原先它同时存在于 Screen 和列表里，两边靠手工同步，
 * 是状态不一致的温床。现在 Screen 需要时通过 {@link #getSelectedIndex()} 查询。
 * 双击检测状态也一并移入。
 */
public class TrackListWidget extends ObjectSelectionList<TrackListWidget.Entry> {
    private static final int MARGIN_LEFT = 2;   // 稍微留边距，避免文字超出
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int ROW_PADDING = 8;   // 给滚动条留空间
    private static final int TEXT_PADDING = 6;
    private static final long DOUBLE_CLICK_MS = 350;

    /** 列表与外界的唯一通信方式，避免顶层组件反向依赖 Screen。 */
    public interface Listener {
        void onSelectionChanged(int index);

        void onPlay(Path path, int index);
    }

    private final int listWidth;
    private final Font font;
    private final Listener listener;
    private int selectedIndex = -1;
    private Path lastClickedPath;
    private long lastClickMs = 0L;

    public TrackListWidget(Minecraft mc, Font font, int width, int height, int top, int itemHeight, Listener listener) {
        super(mc, width, height, top, itemHeight);
        this.listWidth = width;
        this.font = font;
        this.listener = listener;
        reloadInfos(MusicLibrary.getTrackInfos());
    }

    @Override
    public int getRowLeft() {
        return MARGIN_LEFT;
    }

    @Override
    protected int scrollBarX() {
        return MARGIN_LEFT + listWidth - SCROLLBAR_WIDTH;
    }

    @Override
    public int getRowWidth() {
        return listWidth - ROW_PADDING;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    /** 重新加载（无歌词信息时使用）。 */
    public void reload(List<Path> paths) {
        clearEntries();
        int i = 0;
        for (Path p : paths) {
            addEntry(new Entry(p, i++));
        }
        restoreSelection();
    }

    /** 重新加载并保留当前选中项。 */
    public void reloadInfos(List<MusicLibrary.TrackInfo> infos) {
        clearEntries();
        int i = 0;
        for (MusicLibrary.TrackInfo info : infos) {
            addEntry(new Entry(info.getAudioPath(), i++, info.hasLyrics()));
        }
        restoreSelection();
    }

    /** 重新加载（搜索过滤后使用），选中项重置到第一条。 */
    public void reloadFilteredInfos(List<MusicLibrary.TrackInfo> infos) {
        clearEntries();
        int i = 0;
        for (MusicLibrary.TrackInfo info : infos) {
            addEntry(new Entry(info.getAudioPath(), i++, info.hasLyrics()));
        }
        if (!infos.isEmpty()) {
            selectedIndex = 0;
            setSelected(children().get(0));
        } else {
            selectedIndex = -1;
        }
        notifySelectionChanged();
    }

    public void selectIndex(int index) {
        if (index >= 0 && index < getItemCount()) {
            selectedIndex = index;
            setSelected(children().get(index));
        }
        notifySelectionChanged();
    }

    private void restoreSelection() {
        if (selectedIndex >= 0 && selectedIndex < getItemCount()) {
            setSelected(children().get(selectedIndex));
        } else {
            selectedIndex = -1;
        }
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (listener != null) {
            listener.onSelectionChanged(selectedIndex);
        }
    }

    public class Entry extends ObjectSelectionList.Entry<Entry> {
        private final Path path;
        private final int index;
        private final boolean hasLyrics;

        Entry(Path path, int index) {
            this(path, index, false);
        }

        Entry(Path path, int index, boolean hasLyrics) {
            this.path = path;
            this.index = index;
            this.hasLyrics = hasLyrics;
        }

        public Path getPath() {
            return path;
        }

        public int getIndex() {
            return index;
        }

        private String suffix() {
            return hasLyrics
                    ? Component.translatable("craftmusic.ui.has_lyrics").getString()
                    : Component.translatable("craftmusic.ui.no_lyrics").getString();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int fh = font.lineHeight;
            // 26.1 起条目位置不再作为参数传入，改由 LayoutElement 的几何信息取
            int textY = getY() + Math.max(0, (getHeight() - fh) / 2);
            int textX = getX() + TEXT_PADDING;
            gfx.text(font, path.getFileName() + "  [" + suffix() + "]", textX, textY, 0xFFFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != 0) {
                return false;
            }
            setSelected(this);
            selectedIndex = this.index;

            long now = System.currentTimeMillis();
            boolean isDoubleClick = lastClickedPath != null
                    && lastClickedPath.equals(this.path)
                    && (now - lastClickMs) <= DOUBLE_CLICK_MS;
            lastClickedPath = this.path;
            lastClickMs = now;

            notifySelectionChanged();
            if (isDoubleClick && listener != null) {
                listener.onPlay(this.path, this.index);
            }
            return true;
        }

        @Override
        public @Nonnull Component getNarration() {
            return Component.literal(path.getFileName() + " [" + suffix() + "]");
        }
    }
}
