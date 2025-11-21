package com.tqk114514.craftmusic.client;

import com.tqk114514.craftmusic.CraftMusic;
import com.tqk114514.craftmusic.audio.MiniaudioPlayer;
import com.tqk114514.craftmusic.client.settings.SettingsScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuickPlayScreen extends Screen {
    private final MiniaudioPlayer player;
    private TrackList trackList;
    private EditBox searchBox;   // 搜索框
    private Button prevBtn;      // 保留引用以更新启用状态
    private Button playToggleBtn; // 更新按钮文本
    private Button nextBtn;      // 保留引用以更新启用状态
    private Button modeBtn;      // 播放模式切换
    private Button lyricsBtn;    // 悬浮歌词开关
    private Button openFolderBtn; // 打开文件夹
    private Button settingsBtn;   // 设置（保留占位）
    private List<MusicLibrary.TrackInfo> filteredTracks = new ArrayList<>(); // 过滤后的歌曲列表

    private int selectedIndex = -1;
    private boolean isPlaying = false;
    private Path currentPath = null; // 预留：后续可用于高亮/元数据展示
    private long lastClickMs = 0L;
    private Path lastClickedPath = null;
    private static final int DOUBLE_CLICK_MS = 350;
    private int seekBarX = 2;  // 与其他元素对齐
    private int seekBarY;
    private int seekBarW;
    private int seekBarH = 6;
    private boolean dragging = false;
    private int pendingSeekMs = -1;
    // 保留字段移除，逻辑由后台控制器处理
    // 音量条
    private int volBarX;
    private int volBarY;
    private int volBarW = 110;
    private int volBarH = 6;
    private boolean draggingVol = false;
    // 播放模式改为由全局控制器管理
    // 固定显示歌词（按钮不再控制）
    private Lyrics currentLyrics = Lyrics.empty();
    private String lyricsLoadedForPath = null;
    // 歌词滚动（物理）
    private float lyricScrollPos = -1f;
    private float lyricScrollVel = 0f;
    private long lyricLastUpdateMs = 0L;
    private static final float SCROLL_SPRING_K = 60f; // 弹性系数
    private static final float SCROLL_DAMP_C = (float)(2.0 * Math.sqrt(SCROLL_SPRING_K)); // 临界阻尼
    private static final float MAX_DT = 0.05f; // 防止卡顿帧跳跃
    // 当前行缩放动画（+8%）
    private int scaleCurrentIndex = -1;
    private int scalePrevIndex = -1;
    private long scaleAnimStartMs = 0L;
    private static final int SCALE_ANIM_MS = 180;
    // 无歌词时提示的缩放动画
    private boolean noLyricsActive = false;
    private long noLyricsAnimStartMs = 0L;
    // 歌词面板位置
    private int lyricsTopY;    // 歌词面板顶部位置
    private int lyricsBottomY; // 歌词面板底部位置

    public QuickPlayScreen(MiniaudioPlayer player) {
        super(Component.literal("CraftMusic Quick Play"));
        this.player = player;
    }

    @Override
    protected void init() {
        // 进入界面时刷新库
        MusicLibrary.scan();

        // 添加搜索框，调整左侧列表位置
        int searchBoxHeight = 20;
        int searchBoxY = 40;
        int searchBoxGap = 5;
        int listTop = searchBoxY + searchBoxHeight + searchBoxGap; // 左侧列表往下移动
        int controlsHeight = 28; // 底部控制区高度
        int listBottomPadding = controlsHeight + 36; // 额外上移 12px（原为 +24）
        int listHeight = Math.max(20, this.height - listTop - listBottomPadding);
        
        // 右侧歌词面板保持原来的高度，不受搜索框影响
        int lyricsTop = 40; // 原始高度
        int lyricsHeight = Math.max(20, this.height - lyricsTop - listBottomPadding);
        this.lyricsTopY = lyricsTop;
        this.lyricsBottomY = lyricsTop + lyricsHeight;
        
        // 左右分栏
        int midGap = 10;
        int rightPadding = 10; // 只保留右边距
        int leftWidth = (this.width - rightPadding - midGap) / 2; // 左边贴边，右边10px边距
        
        // 创建搜索框（与列表对齐）
        searchBox = new EditBox(this.font, 2, searchBoxY, leftWidth - 2, searchBoxHeight, Component.translatable("craftmusic.ui.search"));
        searchBox.setHint(Component.translatable("craftmusic.ui.search.hint"));
        searchBox.setResponder(text -> {
            filterTracks(text);
        });
        addRenderableWidget(searchBox);
        
        // 创建列表
        trackList = new TrackList(minecraft, leftWidth - 2, listHeight, listTop, 20);  // 宽度减2补偿左边距
        addRenderableWidget(trackList);
        
        // 初始化过滤列表
        filteredTracks = new ArrayList<>(MusicLibrary.getTrackInfos());

        // 顶部按钮：刷新 / 歌词 / 打开文件夹 / 设置 / 关闭
        int topY = 10;
        int leftPad = 10;
        int rightPad = 10;
        int gap = 5;
        int btnCount = 5;
        int availableW = Math.max(50, this.width - leftPad - rightPad - gap * (btnCount - 1));
        int eachW = Math.max(70, availableW / btnCount);
        int x = leftPad;

        // 刷新
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.refresh"), b -> {
            MusicLibrary.scan();
            filteredTracks = new ArrayList<>(MusicLibrary.getTrackInfos());
            searchBox.setValue("");
            trackList.reloadInfos(filteredTracks);
        }).bounds(x, topY, eachW, 20).build());
        x += eachW + gap;

        // 悬浮歌词按钮（读写配置）
        boolean floating = ClientConfig.isFloatingLyrics();
        lyricsBtn = addRenderableWidget(Button.builder(Component.translatable(floating ? "craftmusic.ui.floating_lyrics.on" : "craftmusic.ui.floating_lyrics.off"), b -> {
            boolean cur = ClientConfig.isFloatingLyrics();
            ClientConfig.setFloatingLyrics(!cur);
            // 更新按钮文本
            boolean now = !cur;
            lyricsBtn.setMessage(Component.translatable(now ? "craftmusic.ui.floating_lyrics.on" : "craftmusic.ui.floating_lyrics.off"));
        }).bounds(x, topY, eachW, 20).build());
        x += eachW + gap;

        // 打开文件夹
        openFolderBtn = addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.open_folder"), b -> openLibraryFolder())
                .bounds(x, topY, eachW, 20).build());
        x += eachW + gap;

        // 设置（占位）
        settingsBtn = addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.settings"), b -> Minecraft.getInstance().setScreen(new SettingsScreen(this)))
                .bounds(x, topY, eachW, 20).build());
        x += eachW + gap;

        // 关闭
        addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.close"), b -> Minecraft.getInstance().setScreen(null))
                .bounds(x, topY, eachW, 20)
                .build());
        // 固定显示“歌词：开”

        // 底部控制条：上一首 / 启停 / 下一首 / 音量条 / 模式（自适应）
        int bottomY = this.height - controlsHeight;
        int leftPadB = 2;  // 与列表对齐，留2px边距
        int rightPadB = 10;
        int gapB = 5;
        int btnCountB = 5; // prev, play, next, vol, mode
        int gapsTotalB = gapB * (btnCountB - 1);
        int availableB = Math.max(100, this.width - leftPadB - rightPadB - gapsTotalB);
        int minVolW = 120;
        int btnW = Math.max(60, (availableB - minVolW) / 4);
        int modeExtra = 20; // 模式按钮额外加宽
        int modeW = btnW + modeExtra;
        int computedVolW = availableB - (btnW * 3 + modeW); // 剩余给音量条
        if (computedVolW < 60) {
            int deficit = 60 - computedVolW;
            modeW = Math.max(60, modeW - deficit);
            computedVolW = 60;
        }
        int xb = leftPadB;

        prevBtn = addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.prev"), b -> playPrev())
                .bounds(xb, bottomY, btnW, 20).build());
        xb += btnW + gapB;

        playToggleBtn = addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.play"), b -> togglePlayPause())
                .bounds(xb, bottomY, btnW, 20).build());
        xb += btnW + gapB;

        nextBtn = addRenderableWidget(Button.builder(Component.translatable("craftmusic.ui.next"), b -> playNext())
                .bounds(xb, bottomY, btnW, 20).build());
        xb += btnW + gapB;

        // 音量条占位与几何
        volBarY = bottomY + 7;
        volBarX = xb;
        volBarW = Math.max(60, computedVolW);
        xb += volBarW + gapB;

        modeBtn = addRenderableWidget(Button.builder(Component.literal("") , b -> cycleMode())
                .bounds(xb, bottomY, modeW, 20).build());
        updateModeButtonLabel();

        // 进度条区域
        seekBarY = bottomY - 12;
        seekBarW = this.width - 2 - 10; // 左边2px，右边10px边距

        // 初始选择第一项（若有）
        if (!filteredTracks.isEmpty()) {
            selectedIndex = 0;
            trackList.selectIndex(0);
        }
        updateControlsEnabled();
        // 确保播放器音量与配置一致（避免重进游戏后实际为 100%）
        try {
            if (player != null && player.isOutputReady()) {
                player.setVolume(ClientConfig.getVolume());
            }
        } catch (Throwable ignored) {}
        // 同步播放状态（避免重进界面按钮文本不符）
        if (player != null && player.isOutputReady()) {
            isPlaying = player.isPlaying();
            // 若正在播放，尽量回填当前曲目（用于显示“正在播放”）
            String last = player.getLastPlayedAbsolutePath();
            if (last != null && !last.isBlank()) {
                try {
                    currentPath = java.nio.file.Paths.get(last);
                    // 同步选中项
                    var ls = MusicLibrary.getTracks();
                    if (ls != null) {
                        for (int i = 0; i < ls.size(); i++) {
                            if (ls.get(i).toAbsolutePath().toString().equalsIgnoreCase(last)) {
                                selectedIndex = i;
                                if (trackList != null) trackList.selectIndex(i);
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            updatePlayButtonLabel();
        }
    }

    @Override
    public void render(@Nonnull net.minecraft.client.gui.GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        // 频谱作为“背景”先绘制，避免覆盖按钮/列表等UI
        if (ClientConfig.isSpectrumEnabled() && player != null && player.isOutputReady()) {
            drawSpectrumBar(gfx);
        }
        super.render(gfx, mouseX, mouseY, partialTick);
        gfx.drawString(this.font, Component.translatable("craftmusic.ui.title"), 2, 10 - 9, 0xFFFFFF, false);
        if (currentPath != null && (isPlaying || (player != null && player.isPaused()))) {
            String name = currentPath.getFileName().toString();
            int textX = 2;  // 与其他元素对齐
            int textY = seekBarY - 12; // 显示在进度条上方，避免与底部按钮冲突
            gfx.drawString(this.font, Component.literal(name), textX, textY, 0xFFFFFF, false);
        }

        // 绘制进度条
        drawSeekBar(gfx, mouseX);
        drawVolumeBar(gfx, mouseX);
        // 同步显示与索引（全局控制器可能已切歌）
        syncFromPlayer();
        // 绘制右侧歌词
        drawLyricsPanel(gfx);
    }

    private final float[] spectrumBuf = new float[64];
    private long lastSpectrumFetchMs = 0L;
    private void drawSpectrumBar(net.minecraft.client.gui.GuiGraphics gfx) {
        int bands = 64;
        long now = System.currentTimeMillis();
        if (now - lastSpectrumFetchMs >= 33) { // ~30FPS
            try { player.getSpectrum(spectrumBuf, bands); } catch (Throwable ignored) {}
            lastSpectrumFetchMs = now;
        }
        int x0 = 2;  // 与列表等元素对齐，留2px边距
        int x1 = this.width - 10;  // 右边保留10px边距
        int yBottom = this.height - 4; // 靠近底缘
        int barAreaHeight = yBottom; // 以屏幕高度为可用范围，不再额外限制
        int width = x1 - x0;
        int barGap = Math.max(1, width / (bands * 8));
        int barW = Math.max(1, (width - (bands - 1) * barGap) / bands);
        // 去除背景框，仅绘制柱状
        float volScale = (player != null) ? Math.max(0f, Math.min(1f, player.getVolume())) : com.tqk114514.craftmusic.client.ClientConfig.getVolume();
        for (int i = 0; i < bands; i++) {
            float v = spectrumBuf[i] * volScale; // 随音量缩放振幅，不改变最大高度
            if (v < 0f) v = 0f; if (v > 1f) v = 1f;
            int h = (int)(v * (barAreaHeight - 4));
            int bx = x0 + i * (barW + barGap);
            int by = yBottom - h;
            int color = 0xFFFFFFFF; // 纯白色
            gfx.fill(bx, by, bx + barW, yBottom, color);
        }
    }

    class TrackList extends ObjectSelectionList<TrackEntry> {
        private final int left = 2;  // 稍微留2px边距，避免文字超出
        private final int listWidth;
        
        public TrackList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
            this.listWidth = width;
            reloadInfos(MusicLibrary.getTrackInfos());
        }

        @Override
        public int getRowLeft() {
            return this.left;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.left + this.listWidth - 6;
        }

        @Override
        public int getRowWidth() {
            return this.listWidth - 8; // 给滚动条留出空间
        }

        void reload(List<Path> paths) {
            clearEntries();
            int i = 0;
            for (Path p : paths) {
                addEntry(new TrackEntry(this, p, i++));
            }
            if (selectedIndex >= 0 && selectedIndex < getItemCount()) {
                setSelected(getEntry(selectedIndex));
            }
            QuickPlayScreen.this.updateControlsEnabled();
        }

        void reloadInfos(List<MusicLibrary.TrackInfo> infos) {
            clearEntries();
            int i = 0;
            for (MusicLibrary.TrackInfo info : infos) {
                addEntry(new TrackEntry(this, info.getAudioPath(), i++, info.hasLyrics()));
            }
            if (selectedIndex >= 0 && selectedIndex < getItemCount()) {
                setSelected(getEntry(selectedIndex));
            }
            QuickPlayScreen.this.updateControlsEnabled();
        }
        
        void reloadFilteredInfos(List<MusicLibrary.TrackInfo> infos) {
            clearEntries();
            int i = 0;
            for (MusicLibrary.TrackInfo info : infos) {
                addEntry(new TrackEntry(this, info.getAudioPath(), i++, info.hasLyrics()));
            }
            if (!infos.isEmpty()) {
                selectedIndex = 0;
                setSelected(getEntry(0));
            }
            QuickPlayScreen.this.updateControlsEnabled();
        }

        void selectIndex(int index) {
            if (index >= 0 && index < getItemCount()) {
                setSelected(getEntry(index));
            }
            QuickPlayScreen.this.updateControlsEnabled();
        }
    }

    class TrackEntry extends ObjectSelectionList.Entry<TrackEntry> {
        private final Path path;
        private final TrackList parent;
        private final int index;
        private final boolean hasLyrics;

        TrackEntry(TrackList parent, Path path, int index) {
            this.parent = parent;
            this.path = path;
            this.index = index;
            this.hasLyrics = false;
        }

        TrackEntry(TrackList parent, Path path, int index, boolean hasLyrics) {
            this.parent = parent;
            this.path = path;
            this.index = index;
            this.hasLyrics = hasLyrics;
        }

        @Override
        public void render(@Nonnull net.minecraft.client.gui.GuiGraphics gfx, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick) {
            String name = path.getFileName().toString();
            String suffix = hasLyrics ? Component.translatable("craftmusic.ui.has_lyrics").getString() : Component.translatable("craftmusic.ui.no_lyrics").getString();
            int fh = QuickPlayScreen.this.font.lineHeight;
            int textY = y + Math.max(0, (entryHeight - fh) / 2);
            // 使用传入的x参数作为基准，这是列表项的实际渲染位置
            int textX = x + 6; // 使用传入的x坐标，加6px内边距
            gfx.drawString(QuickPlayScreen.this.font, name + "  [" + suffix + "]", textX, textY, 0xFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false;
            // 选择该项
            parent.setSelected(this);
            selectedIndex = this.index;

            long now = System.currentTimeMillis();
            boolean isDoubleClick = (lastClickedPath != null && lastClickedPath.equals(this.path) && (now - lastClickMs) <= DOUBLE_CLICK_MS);
            lastClickedPath = this.path;
            lastClickMs = now;

            if (isDoubleClick) {
                // 双击播放
                playTrack(this.path, this.index);
            }
            return true;
        }

        @Override
        public @Nonnull Component getNarration() {
            String name = path.getFileName().toString();
            String suffix = hasLyrics ? Component.translatable("craftmusic.ui.has_lyrics").getString() : Component.translatable("craftmusic.ui.no_lyrics").getString();
            return Component.literal(name + " [" + suffix + "]");
        }
    }

    private void playPrev() {
        if (filteredTracks.isEmpty()) return;
        int current = resolveCurrentIndexFromFiltered();
        if (current < 0) current = 0;
        int idx = (current - 1 + filteredTracks.size()) % filteredTracks.size();
        playTrack(filteredTracks.get(idx).getAudioPath(), idx);
    }

    private void playNext() {
        if (filteredTracks.isEmpty()) return;
        int current = resolveCurrentIndexFromFiltered();
        if (current < 0) current = -1;
        int idx = (current + 1 + filteredTracks.size()) % filteredTracks.size();
        playTrack(filteredTracks.get(idx).getAudioPath(), idx);
    }

    private void togglePlayPause() {
        if (player == null || !player.isOutputReady()) return;
        if (isPlaying && !player.isPaused()) {
            player.pause();
            isPlaying = false;
            updatePlayButtonLabel();
            return;
        }
        // 若处于暂停，则恢复
        if (player.isPaused()) {
            player.resume();
            isPlaying = true;
            updatePlayButtonLabel();
            return;
        }
        // 未在播放/未暂停，则播放当前选择或第一首
        if (filteredTracks.isEmpty()) return;
        int idx = selectedIndex >= 0 ? selectedIndex : 0;
        playTrack(filteredTracks.get(idx).getAudioPath(), idx);
    }

    private void playTrack(Path p, int idx) {
        if (player == null || !player.isOutputReady()) return;
        CraftMusic.LOGGER.info("UI play: {}", p);
        int rc = player.play(p.toAbsolutePath().toString());
        if (rc == 0) {
            this.isPlaying = true;
            this.currentPath = p;
            this.selectedIndex = idx;
            if (trackList != null) trackList.selectIndex(idx);
            // 播放时根据是否有歌词，决定按钮初始状态
            updateLyricsStateForCurrent();
            loadLyricsForCurrent();
        } else {
            this.isPlaying = false;
        }
        updatePlayButtonLabel();
        updateControlsEnabled();
    }

    private void updatePlayButtonLabel() {
        if (playToggleBtn != null) {
            String key = (player != null && player.isPaused()) ? "craftmusic.ui.resume" : (isPlaying ? "craftmusic.ui.pause" : "craftmusic.ui.play");
            playToggleBtn.setMessage(Component.translatable(key));
        }
    }

    private void updateControlsEnabled() {
        boolean hasTracks = !filteredTracks.isEmpty();
        boolean multi = hasTracks && filteredTracks.size() > 1;
        if (prevBtn != null) prevBtn.active = multi;
        if (nextBtn != null) nextBtn.active = multi;
        if (playToggleBtn != null) playToggleBtn.active = hasTracks && player != null && player.isOutputReady();
        if (modeBtn != null) modeBtn.active = hasTracks;
        if (openFolderBtn != null) openFolderBtn.active = true;
        // 歌词按钮在当前曲目有歌词时可用
        if (lyricsBtn != null) lyricsBtn.active = hasLyricsForCurrent();
        if (settingsBtn != null) settingsBtn.active = true;
    }

    // ---- 进度条绘制与交互 ----
    private void drawSeekBar(net.minecraft.client.gui.GuiGraphics gfx, int mouseX) {
        int x0 = seekBarX;
        int x1 = seekBarX + seekBarW;
        int y = seekBarY;
        // 背景条
        gfx.fill(x0, y, x1, y + seekBarH, 0x80000000);
        // 进度
        int len = (player != null) ? player.getLengthMs() : 0;
        int pos = (player != null) ? player.getPositionMs() : 0;
        // 拖动中仅显示装饰性位置，不改变真实播放位置
        if (dragging && pendingSeekMs >= 0) pos = pendingSeekMs;
        float pct = (len > 0) ? Math.min(1f, Math.max(0f, pos / (float)len)) : 0f;
        int knobX = x0 + Math.round(pct * seekBarW);
        gfx.fill(x0, y, knobX, y + seekBarH, 0xFF00AAFF);
        // 拖动手柄
        gfx.fill(knobX - 2, y - 2, knobX + 2, y + seekBarH + 2, 0xFFFFFFFF);

        // 时间文本（当前/总时长）
        String timeStr = formatTime(pos) + " / " + formatTime(len);
        gfx.drawString(this.font, timeStr, x1 - Math.max(60, this.font.width(timeStr)), y - 10, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseY >= seekBarY - 4 && mouseY <= seekBarY + seekBarH + 4 && mouseX >= seekBarX && mouseX <= seekBarX + seekBarW) {
                dragging = true;
                updatePendingSeek((int)mouseX);
                return true;
            }
            if (mouseY >= volBarY - 4 && mouseY <= volBarY + volBarH + 4 && mouseX >= volBarX && mouseX <= volBarX + volBarW) {
                draggingVol = true;
                updateVolumeFromMouse((int)mouseX);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && button == 0) {
            updatePendingSeek((int)mouseX);
            return true;
        }
        if (draggingVol && button == 0) {
            updateVolumeFromMouse((int)mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        }

        @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            if (player != null && player.isOutputReady() && pendingSeekMs >= 0) {
                player.seekToMs(pendingSeekMs);
                // 若处于暂停，则释放后立即恢复播放
                if (player.isPaused()) {
                    player.resume();
                    isPlaying = true;
                    updatePlayButtonLabel();
                }
                // 同步 UI 状态
                syncFromPlayer();
            }
            pendingSeekMs = -1;
            return true;
        }
        if (draggingVol && button == 0) {
            draggingVol = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updatePendingSeek(int mouseX) {
        if (player == null || !player.isOutputReady()) return;
        int x0 = seekBarX;
        int rel = Math.max(0, Math.min(seekBarW, mouseX - x0));
        int len = player.getLengthMs();
        int target = (len > 0) ? (int)((rel / (float)seekBarW) * len) : 0;
        pendingSeekMs = target;
    }

    private void drawVolumeBar(net.minecraft.client.gui.GuiGraphics gfx, int mouseX) {
        int x0 = volBarX;
        int x1 = volBarX + volBarW;
        int y = volBarY;
        gfx.fill(x0, y, x1, y + volBarH, 0x80000000);
        float vol = ClientConfig.getVolume();
        int filled = x0 + Math.round(vol * volBarW);
        gfx.fill(x0, y, filled, y + volBarH, 0xFFFFB000);
        gfx.fill(filled - 2, y - 2, filled + 2, y + volBarH + 2, 0xFFFFFFFF);
        // 文本：左侧“音量”，右侧百分比
        String label = Component.translatable("craftmusic.ui.volume").getString();
        String percent = (int)Math.round(vol * 100) + "%";
        int textY = y - 9;
        gfx.drawString(this.font, label, x0, textY, 0xFFFFFFFF, false);
        gfx.drawString(this.font, percent, x1 - this.font.width(percent), textY, 0xFFFFFFFF, false);
    }

    private void updateVolumeFromMouse(int mouseX) {
        int x0 = volBarX;
        int rel = Math.max(0, Math.min(volBarW, mouseX - x0));
        float v = rel / (float) volBarW;
        if (player != null && player.isOutputReady()) {
            player.setVolume(v);
        }
        ClientConfig.setVolume(v);
    }

    private static String formatTime(int ms) {
        int totalSec = Math.max(0, ms / 1000);
        int m = totalSec / 60;
        int s = totalSec % 60;
        return String.format("%d:%02d", m, s);
    }

    // 自动切歌交由后台控制器处理

    private void loadLyricsForCurrent() {
        try {
            if (currentPath == null) {
                currentLyrics = Lyrics.empty();
                lyricsLoadedForPath = null;
                return;
            }
            String cur = currentPath.toAbsolutePath().toString();
            if (cur.equalsIgnoreCase(lyricsLoadedForPath)) return;
            var infos = MusicLibrary.getTrackInfos();
            Lyrics loaded = Lyrics.empty();
            if (infos != null) {
                for (MusicLibrary.TrackInfo info : infos) {
                    if (info != null && info.getAudioPath() != null && info.getAudioPath().toAbsolutePath().toString().equalsIgnoreCase(cur)) {
                        Path lrc = info.getLyricsPath();
                        if (lrc != null) loaded = LrcParser.parse(lrc);
                        break;
                    }
                }
            }
            currentLyrics = loaded;
            lyricsLoadedForPath = cur;
        } catch (Exception ignored) {}
    }

    private void drawLyricsPanel(net.minecraft.client.gui.GuiGraphics gfx) {
        // 左右分栏：左列表贴左边，右边保留10px边距
        int gap = 10;
        int rightPadding = 10;
        int leftWidth = (this.width - rightPadding - gap) / 2;
        int x0 = leftWidth + gap;  // 左列表宽度 + 中间间隔
        int x1 = this.width - 10;
        int top = this.lyricsTopY;    // 使用歌词面板专用的高度
        int bottom = this.lyricsBottomY;
        gfx.fill(x0, top, x1, bottom, 0x90000000);

        // 固定显示歌词（按钮不再控制）。若无歌词，显示“无歌词/纯音乐”并居中放大 8%。
        List<Lyrics.Line> lines = (currentLyrics != null) ? currentLyrics.getLines() : java.util.Collections.emptyList();
        if (lines.isEmpty()) {
            String txt = Component.translatable("craftmusic.ui.no_lyrics_or_instrumental").getString();
            int centerY = this.lyricsTopY + 8 + (this.lyricsBottomY - this.lyricsTopY - 16) / 2 - this.font.lineHeight / 2;
            int midX = (x0 + x1) / 2;
            if (ClientConfig.isLyricEffects()) {
                long now = System.currentTimeMillis();
                if (!noLyricsActive) { noLyricsActive = true; noLyricsAnimStartMs = now; }
                float t = Math.max(0f, Math.min(1f, (now - noLyricsAnimStartMs) / (float)SCALE_ANIM_MS));
                float eased = (t < 0.5f) ? (4f * t * t * t) : (1f - (float)Math.pow(-2f * t + 2f, 3f) / 2f);
                float scale = 1.0f + 0.08f * eased;
                drawCenteredScaledString(gfx, txt, midX, centerY, scale, 0xFFFFFFFF);
            } else {
                noLyricsActive = false;
                int tx = midX - this.font.width(txt) / 2;
                gfx.drawString(this.font, txt, tx, centerY, 0xFFFFFFFF, false);
            }
            return;
        } else {
            noLyricsActive = false;
        }
        int curMs = (player != null) ? player.getPositionMs() : 0;
        int curIdx = findCurrentLineIndex(lines, curMs);
        long now = System.currentTimeMillis();
        if (!ClientConfig.isLyricEffects()) {
            lyricScrollPos = (curIdx < 0) ? 0f : Math.min(curIdx, lines.size() - 1);
            lyricScrollVel = 0f;
            lyricLastUpdateMs = now;
        } else {
            if (lyricLastUpdateMs == 0L || lyricScrollPos < 0f) {
                lyricScrollPos = (curIdx < 0) ? 0f : Math.min(curIdx, lines.size() - 1);
                lyricScrollVel = 0f;
                lyricLastUpdateMs = now;
            } else {
                float target = (curIdx < 0) ? 0f : Math.min(curIdx, lines.size() - 1);
                // 跳跃过大时直接对齐，避免长距离拖尾
                if (Math.abs(target - lyricScrollPos) > 3f) {
                    lyricScrollPos = target;
                    lyricScrollVel = 0f;
                } else {
                    float dt = Math.min(MAX_DT, (now - lyricLastUpdateMs) / 1000f);
                    float delta = target - lyricScrollPos;
                    float accel = SCROLL_SPRING_K * delta - SCROLL_DAMP_C * lyricScrollVel;
                    lyricScrollVel += accel * dt;
                    lyricScrollPos += lyricScrollVel * dt;
                }
                lyricLastUpdateMs = now;
            }
        }
        int lineH = this.font.lineHeight + 2;
        int centerY = this.lyricsTopY + 8 + (this.lyricsBottomY - this.lyricsTopY - 16) / 2 - this.font.lineHeight / 2;
        int midX = (x0 + x1) / 2;
        // 缩放动画：当当前歌词行变化时，前一行缩回，当前行放大
        if (ClientConfig.isLyricEffects()) {
            if (scaleCurrentIndex != curIdx) {
                scalePrevIndex = scaleCurrentIndex;
                scaleCurrentIndex = curIdx;
                scaleAnimStartMs = now;
            }
        } else {
            scaleCurrentIndex = -1;
            scalePrevIndex = -1;
        }
        // 使用连续滚动位置绘制
        float drawCenterIndex = (lyricScrollPos < 0f) ? (curIdx < 0 ? 0f : curIdx) : lyricScrollPos;
        int visiblePx = Math.max(0, (this.lyricsBottomY - this.lyricsTopY - 16)); // 上下各 8px 内边距
        int approxVisibleLines = Math.max(1, visiblePx / lineH + 1);
        int half = approxVisibleLines / 2;
        int firstIdx = Math.max(0, (int)Math.floor(drawCenterIndex) - half);
        int lastIdx = Math.min(lines.size() - 1, (int)Math.ceil(drawCenterIndex) + half);
        int textTopBound = this.lyricsTopY + 8;
        int textBottomBound = this.lyricsBottomY - 8 - this.font.lineHeight;
        for (int i = firstIdx; i <= lastIdx; i++) {
            float diff = i - drawCenterIndex;
            int y = Math.round(centerY + diff * lineH);
            if (y < textTopBound || y > textBottomBound) continue;
            String text = lines.get(i).text;
            int tx = midX - this.font.width(text) / 2;
            int color = (i == curIdx) ? 0xFFFFFFFF : 0xFFAAAAAA;
            if (!ClientConfig.isLyricEffects()) {
                gfx.drawString(this.font, text, tx, y, color, false);
            } else {
                float t = Math.max(0f, Math.min(1f, (now - scaleAnimStartMs) / (float)SCALE_ANIM_MS));
                // easeInOutCubic
                float eased = (t < 0.5f) ? (4f * t * t * t) : (1f - (float)Math.pow(-2f * t + 2f, 3f) / 2f);
                float scale;
                if (i == scaleCurrentIndex) {
                    scale = 1.0f + 0.08f * eased;
                } else if (i == scalePrevIndex) {
                    scale = 1.08f - 0.08f * eased;
                } else {
                    scale = 1.0f;
                }
                drawCenteredScaledString(gfx, text, midX, y, scale, color);
            }
        }
    }

    private int findCurrentLineIndex(List<Lyrics.Line> lines, int curMs) {
        if (lines == null || lines.isEmpty()) return -1;
        int lo = 0, hi = lines.size() - 1, ans = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int t = lines.get(mid).timeMs;
            if (t <= curMs) { ans = mid; lo = mid + 1; } else { hi = mid - 1; }
        }
        return ans;
    }

    private void drawCenteredScaledString(net.minecraft.client.gui.GuiGraphics gfx, String text, int midX, int y, float scale, int argb) {
        if (scale <= 0f) return;
        if (Math.abs(scale - 1f) < 0.001f) {
            int tx = midX - this.font.width(text) / 2;
            gfx.drawString(this.font, text, tx, y, argb, false);
            return;
        }
        var pose = gfx.pose();
        pose.pushPose();
        float textW = this.font.width(text);
        float scaledW = textW * scale;
        float tx = midX - scaledW / 2f;
        pose.translate(tx, y, 0);
        pose.scale(scale, scale, 1f);
        gfx.drawString(this.font, text, 0, 0, argb, false);
        pose.popPose();
    }
    
    private void filterTracks(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            filteredTracks = new ArrayList<>(MusicLibrary.getTrackInfos());
        } else {
            String lowerSearch = searchText.toLowerCase(Locale.ROOT);
            filteredTracks = MusicLibrary.getTrackInfos().stream()
                .filter(info -> {
                    String fileName = info.getAudioPath().getFileName().toString().toLowerCase(Locale.ROOT);
                    return fileName.contains(lowerSearch);
                })
                .collect(java.util.stream.Collectors.toList());
        }
        trackList.reloadFilteredInfos(filteredTracks);
    }
    
    private int resolveCurrentIndexFromFiltered() {
        if (currentPath == null) return -1;
        String current = currentPath.toAbsolutePath().toString();
        for (int i = 0; i < filteredTracks.size(); i++) {
            if (filteredTracks.get(i).getAudioPath().toAbsolutePath().toString().equalsIgnoreCase(current)) {
                return i;
            }
        }
        return -1;
    }

    private void cycleMode() {
        com.tqk114514.craftmusic.client.PlaybackController.cycleMode();
        updateModeButtonLabel();
    }

    private void updateModeButtonLabel() {
        if (modeBtn == null) return;
        String key = com.tqk114514.craftmusic.client.PlaybackController.modeTranslationKey();
        modeBtn.setMessage(Component.translatable("craftmusic.ui.mode", Component.translatable(key)));
    }

    private void syncFromPlayer() {
        if (player == null) return;
        String last = player.getLastPlayedAbsolutePath();
        if (last != null && !last.isBlank()) {
            if (currentPath == null || !currentPath.toAbsolutePath().toString().equalsIgnoreCase(last)) {
                try {
                    currentPath = java.nio.file.Paths.get(last);
                    var ls = MusicLibrary.getTracks();
                    if (ls != null) {
                        for (int i = 0; i < ls.size(); i++) {
                            if (ls.get(i).toAbsolutePath().toString().equalsIgnoreCase(last)) {
                                selectedIndex = i;
                                if (trackList != null) trackList.selectIndex(i);
                                break;
                            }
                        }
                    }
                    // 后台切歌时尝试加载歌词
                    loadLyricsForCurrent();
                } catch (Exception ignored) {}
            } else {
                // 已在播放同一首，但未曾加载或已清空缓存时，补载一次歌词
                if (currentPath != null) {
                    String cur = currentPath.toAbsolutePath().toString();
                    if (lyricsLoadedForPath == null || !lyricsLoadedForPath.equalsIgnoreCase(cur)) {
                        loadLyricsForCurrent();
                    }
                }
            }
        }
        isPlaying = player.isPlaying();
        updatePlayButtonLabel();
        // 同步当前曲目时，仅刷新按钮可用状态与文案，不强制改动用户选择
        refreshLyricsControls();
    }

    private boolean hasLyricsForCurrent() {
        if (currentPath == null) return false;
        var infos = MusicLibrary.getTrackInfos();
        if (infos == null || infos.isEmpty()) return false;
        String cur = currentPath.toAbsolutePath().toString();
        for (MusicLibrary.TrackInfo info : infos) {
            if (info != null && info.getAudioPath() != null) {
                String p = info.getAudioPath().toAbsolutePath().toString();
                if (p.equalsIgnoreCase(cur)) return info.hasLyrics();
            }
        }
        return false;
    }

    private void updateLyricsStateForCurrent() {
        // 固定显示歌词，不再根据有无歌词改变显示开关
        refreshLyricsControls();
    }

    private void refreshLyricsControls() {
        if (lyricsBtn != null) lyricsBtn.active = true; // 始终可点但无功能
    }

    private void openLibraryFolder() {
        try {
            MusicLibrary.ensureFolderExists();
            java.nio.file.Path dir = MusicLibrary.getLibraryDir();
            if (dir == null) return;
            String path = dir.toAbsolutePath().toString();
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(dir.toFile());
                    return;
                }
            } catch (Throwable ignored) {}
            new ProcessBuilder("explorer", path).start();
        } catch (Exception e) {
            CraftMusic.LOGGER.warn("Open folder failed: {}", e.toString());
        }
    }
}

