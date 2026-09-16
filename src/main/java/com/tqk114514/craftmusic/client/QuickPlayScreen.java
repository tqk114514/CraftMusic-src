package com.tqk114514.craftmusic.client;

import com.tqk114514.craftmusic.CraftMusic;
import com.tqk114514.craftmusic.audio.MiniaudioPlayer;
import com.tqk114514.craftmusic.client.settings.SettingsScreen;
import com.tqk114514.craftmusic.client.widget.LyricsPanelView;
import com.tqk114514.craftmusic.client.widget.SeekBarView;
import com.tqk114514.craftmusic.client.widget.TrackListWidget;
import com.tqk114514.craftmusic.client.widget.SpectrumView;
import com.tqk114514.craftmusic.client.widget.VolumeBarView;

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

public class QuickPlayScreen extends Screen implements TrackListWidget.Listener {
    private final MiniaudioPlayer player;
    private TrackListWidget trackList;
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
    private final SeekBarView seekBar = new SeekBarView();
    // 保留字段移除，逻辑由后台控制器处理
    // 音量条
    private final VolumeBarView volumeBar = new VolumeBarView();
    // 播放模式改为由全局控制器管理
    // 歌词面板：几何、滚动/缩放动画状态全部归组件所有
    private final LyricsPanelView lyricsPanel = new LyricsPanelView();
    private String lyricsLoadedForPath = null; // 仅用于判断是否需要重新解析歌词

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
        lyricsPanel.layout(lyricsTop, lyricsHeight);
        
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
        trackList = new TrackListWidget(minecraft, this.font, leftWidth - 2, listHeight, listTop, 20, this);  // 宽度减2补偿左边距
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
        volumeBar.layout(xb, bottomY + 7, Math.max(60, computedVolW));
        xb += volumeBar.getWidth() + gapB;

        modeBtn = addRenderableWidget(Button.builder(Component.literal("") , b -> cycleMode())
                .bounds(xb, bottomY, modeW, 20).build());
        updateModeButtonLabel();

        // 进度条区域
        seekBar.layout(this.width, bottomY - 12);

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
    public void extractRenderState(@Nonnull net.minecraft.client.gui.GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // 频谱作为“背景”先绘制，避免覆盖按钮/列表等UI
        if (ClientConfig.isSpectrumEnabled() && player != null && player.isOutputReady()) {
            spectrumView.extract(gfx, player, this.width, this.height);
        }
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        gfx.text(this.font, Component.translatable("craftmusic.ui.title"), 2, 10 - 9, 0xFFFFFFFF, false);
        if (currentPath != null && (isPlaying || (player != null && player.isPaused()))) {
            String name = currentPath.getFileName().toString();
            int textX = 2;  // 与其他元素对齐
            int textY = seekBar.getY() - 12; // 显示在进度条上方，避免与底部按钮冲突
            gfx.text(this.font, Component.literal(name), textX, textY, 0xFFFFFFFF, false);
        }

        // 绘制进度条
        seekBar.extract(gfx, this.font, player);
        volumeBar.extract(gfx, this.font);
        // 同步显示与索引（全局控制器可能已切歌）
        syncFromPlayer();
        // 绘制右侧歌词
        lyricsPanel.extract(gfx, this.font, player, this.width, ClientConfig.isLyricEffects());
    }

    private final SpectrumView spectrumView = new SpectrumView();

    @Override
    public void onSelectionChanged(int index) {
        this.selectedIndex = index;
        updateControlsEnabled();
    }

    @Override
    public void onPlay(Path path, int index) {
        playTrack(path, index);
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
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        if (button == 0) {
            if (seekBar.containsPoint(mouseX, mouseY)) {
                seekBar.beginDrag((int)mouseX, (player != null) ? player.getLengthMs() : 0);
                return true;
            }
            if (volumeBar.containsPoint(mouseX, mouseY)) {
                volumeBar.beginDrag((int)mouseX, player);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        if (seekBar.isDragging() && button == 0) {
            seekBar.updateDrag((int)mouseX, (player != null) ? player.getLengthMs() : 0);
            return true;
        }
        if (volumeBar.isDragging() && button == 0) {
            volumeBar.updateDrag((int)mouseX, player);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
        }

        @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        if (seekBar.isDragging() && button == 0) {
            int targetMs = seekBar.endDrag();
            if (player != null && player.isOutputReady() && targetMs >= 0) {
                player.seekToMs(targetMs);
                // 若处于暂停，则释放后立即恢复播放
                if (player.isPaused()) {
                    player.resume();
                    isPlaying = true;
                    updatePlayButtonLabel();
                }
                // 同步 UI 状态
                syncFromPlayer();
            }
            return true;
        }
        if (volumeBar.isDragging() && button == 0) {
            volumeBar.endDrag();
            return true;
        }
        return super.mouseReleased(event);
    }

    private static String formatTime(int ms) {
        int totalSec = Math.max(0, ms / 1000);
        int m = totalSec / 60;
        int s = totalSec % 60;
        return String.format("%d:%02d", m, s);
    }

    // 自动切歌交由后台控制器处理

    private void loadLyricsForCurrent() {
        if (currentPath == null) {
            lyricsPanel.setLyrics(Lyrics.empty());
            lyricsLoadedForPath = null;
            return;
        }
        String cur = currentPath.toAbsolutePath().toString();
        if (cur.equalsIgnoreCase(lyricsLoadedForPath)) return;
        lyricsPanel.setLyrics(MusicLibrary.loadLyrics(cur));
        lyricsLoadedForPath = cur;
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
        var info = MusicLibrary.findTrack(currentPath.toAbsolutePath().toString());
        return info != null && info.hasLyrics();
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

