package com.tqk114514.craftmusic;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.tqk114514.craftmusic.audio.MiniaudioPlayer;
import com.tqk114514.craftmusic.client.QuickPlayScreen;
import com.tqk114514.craftmusic.client.settings.lyrics.FloatingLyricsPositionScreen;
import com.tqk114514.craftmusic.client.MusicLibrary;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.tqk114514.craftmusic.client.PlaybackController;
import com.tqk114514.craftmusic.client.Lyrics;
import com.tqk114514.craftmusic.client.ClientConfig;

@Mod(value = CraftMusic.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CraftMusic.MODID, value = Dist.CLIENT)
public class CraftMusicClient {
    private static MiniaudioPlayer PLAYER;
        private static KeyMapping OPEN_UI_KEY;
    private static volatile Lyrics overlayLyrics = Lyrics.empty();
    private static volatile String overlayLyricsForPath = null;
    private static volatile float lastAppliedVolume = -1f;
    public CraftMusicClient(ModContainer container) {}

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CraftMusic.LOGGER.info("CraftMusic client setup initialized");
        // 先加载配置
        com.tqk114514.craftmusic.client.ClientConfig.initAndLoad();
        float cfgVol = com.tqk114514.craftmusic.client.ClientConfig.getVolume();
        CraftMusic.LOGGER.info("CraftMusic load volume from config: {}", cfgVol);
        // 初始化播放器
        PLAYER = new MiniaudioPlayer(48000, 2);
        if (!PLAYER.isOutputReady()) {
            CraftMusic.LOGGER.warn("Native audio library not loaded. Playback will be disabled until JNI is provided.");
        } else {
            try {
                PLAYER.setVolume(cfgVol);
                CraftMusic.LOGGER.info("CraftMusic applied initial volume: {}", cfgVol);
            } catch (Throwable ignored) {}
        }
        lastAppliedVolume = cfgVol;
        // 启动时扫描一次
        MusicLibrary.initAndScan();
    }

    @SubscribeEvent
    static void registerClientCommands(RegisterClientCommandsEvent event) {
        var play = Commands.literal("craftmusic").then(
                Commands.literal("play").then(
                        Commands.argument("path", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String raw = StringArgumentType.getString(ctx, "path").trim();
                                    String path = sanitizePath(raw);
                                    if (PLAYER == null || !PLAYER.isOutputReady()) {
                                        CraftMusic.LOGGER.warn("Audio output not ready.");
                                        var mc0 = Minecraft.getInstance();
                                        if (mc0.player != null) {
                                            mc0.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("craftmusic.error.output_not_ready"), false);
                                        }
                                        return 0;
                                    }
                                    CraftMusic.LOGGER.info("Playing via cmd: {}", path);
                                    int rc = PLAYER.play(path);
                                    if (rc != 0) {
                                        CraftMusic.LOGGER.warn("Play failed with code {}", rc);
                                        var mc = Minecraft.getInstance();
                                        var player = mc.player;
                                        if (player != null) {
                                            var err = net.minecraft.network.chat.Component.translatable(errorKey(rc));
                                            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("craftmusic.play.failed", err, Integer.toString(rc)), false);
                                        }
                                        return 0;
                                    }
                                    var mc = Minecraft.getInstance();
                                    var player = mc.player;
                                    if (player != null) {
                                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("craftmusic.play.start", path), false);
                                    }
                                    return 1;
                                })
                )
        ).then(
                Commands.literal("stop").executes(ctx -> {
                    var mc = Minecraft.getInstance();
                    var player = mc.player;
                    if (PLAYER != null && PLAYER.isOutputReady()) {
                        PLAYER.stop();
                        if (player != null) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("craftmusic.play.stop"), false);
                    } else if (player != null) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("craftmusic.error.output_not_ready"), false);
                    }
                    return 1;
                })
        ).then(
                Commands.literal("ui").executes(ctx -> {
                    if (PLAYER != null) {
                        Minecraft.getInstance().setScreen(new QuickPlayScreen(PLAYER));
                    }
                    return 1;
                })
        ).then(
                Commands.literal("tone").executes(ctx -> {
                    if (PLAYER == null || !PLAYER.isOutputReady()) {
                        CraftMusic.LOGGER.warn("Audio output not ready.");
                        var mc0 = Minecraft.getInstance();
                        var player0 = mc0.player;
                        if (player0 != null) {
                            player0.displayClientMessage(net.minecraft.network.chat.Component.translatable("craftmusic.error.output_not_ready"), false);
                        }
                        return 0;
                    }
                    PLAYER.playTone(440, 1000);
                    return 1;
                }).then(
                        Commands.argument("hz", StringArgumentType.word()).then(
                                Commands.argument("ms", StringArgumentType.word()).executes(ctx -> {
                                    if (PLAYER == null || !PLAYER.isOutputReady()) return 0;
                                    int hz;
                                    int ms;
                                    try {
                                        hz = Integer.parseInt(StringArgumentType.getString(ctx, "hz"));
                                        ms = Integer.parseInt(StringArgumentType.getString(ctx, "ms"));
                                    } catch (NumberFormatException ex) {
                                        CraftMusic.LOGGER.warn("Invalid tone args");
                                        return 0;
                                    }
                                    PLAYER.playTone(hz, ms);
                                    return 1;
                                })
                        )
                )
        );

        event.getDispatcher().register(play);
    }

    public static MiniaudioPlayer getPlayer() { return PLAYER; }

    private static String sanitizePath(String input) {
        if (input == null) return null;
        String s = input.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            if (s.length() >= 2) s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String errorKey(int code) {
        return switch (code) {
            case 0 -> "craftmusic.error.ok";
            case -2 -> "craftmusic.error.invalid_args";
            case -7 -> "craftmusic.error.not_found"; // MA_DOES_NOT_EXIST
            case -9 -> "craftmusic.error.format_not_supported"; // MA_FORMAT_NOT_SUPPORTED
            case -10 -> "craftmusic.error.invalid_file"; // MA_INVALID_FILE
            case -1 -> "craftmusic.error.unknown";
            default -> "craftmusic.error.unknown";
        };
    }

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen screen) {
            int width = screen.width;
            int x = width - 110; // 右上角留出边距
            int y = 5;
            Button btn = Button.builder(Component.translatable("craftmusic.button.open"), b -> {
                if (PLAYER != null) {
                    Minecraft.getInstance().setScreen(new QuickPlayScreen(PLAYER));
                } else {
                    var mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.displayClientMessage(Component.translatable("craftmusic.error.output_not_ready"), false);
                    }
                }
            }).bounds(x, y, 100, 20).build();
            event.addListener(btn);
        }
    }

    @SubscribeEvent
    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_UI_KEY = new KeyMapping(
                "key.craftmusic.open_ui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "key.categories.craftmusic"
        );
        event.register(OPEN_UI_KEY);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        // 按键打开 UI
        if (OPEN_UI_KEY != null && OPEN_UI_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new QuickPlayScreen(PLAYER));
        }
        // 后台自动切歌 / 模式逻辑
        PlaybackController.update(PLAYER);
        // 音量守护：确保实际输出音量与配置一致（防止原生层在播放开始时恢复为 100%）
        try {
            if (PLAYER != null && PLAYER.isOutputReady()) {
                float cfg = com.tqk114514.craftmusic.client.ClientConfig.getVolume();
                float cur = PLAYER.getVolume();
                if (Math.abs(cur - cfg) > 0.02f || Math.abs(lastAppliedVolume - cfg) > 0.001f) {
                    PLAYER.setVolume(cfg);
                    lastAppliedVolume = cfg;
                }
            }
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        // HUD 场景：仅当配置为 GLOBAL 或 WORLD 且在世界中时渲染
        boolean inWorld = Minecraft.getInstance().level != null;
        String scope = ClientConfig.getFloatingLyricsRender();
        if (Minecraft.getInstance().screen == null) {
            if ("GLOBAL".equals(scope) || ("WORLD".equals(scope) && inWorld)) {
                renderFloatingLyrics(event.getGuiGraphics());
            }
        }
    }

    @SubscribeEvent
    static void onScreenRender(ScreenEvent.Render.Post event) {
        // Screen 场景：仅当配置为 GLOBAL 时渲染（WORLD 模式下只在 HUD）。
        // 若当前为位置设置界面，跳过全局渲染，避免与该界面自身预览重复。
        String scope = ClientConfig.getFloatingLyricsRender();
        if ("GLOBAL".equals(scope)) {
            if (event.getScreen() instanceof FloatingLyricsPositionScreen) return;
            renderFloatingLyrics(event.getGuiGraphics());
        }
    }

    private static void renderFloatingLyrics(GuiGraphics gfx) {
        if (!ClientConfig.isFloatingLyrics()) return;
        if (PLAYER == null || !PLAYER.isOutputReady()) return;
        String path = PLAYER.getLastPlayedAbsolutePath();
        if (path == null || path.isBlank()) return;
        var lines = loadOverlayLyrics(path).getLines();
        if (lines.isEmpty()) return;
        int curMs = 0;
        try { curMs = PLAYER.getPositionMs(); } catch (Throwable ignored) {}
        int idx = Lyrics.findLineIndexAt(lines, curMs);
        if (idx < 0 || idx >= lines.size()) return;
        String text = lines.get(idx).text;
        if (text == null || text.isBlank()) return;
        var mc = Minecraft.getInstance();
        
        // 字号缩放与对齐（左右吸附影响对齐方式）
        float scale = ClientConfig.getFloatingLyricsFontScale();
        int color = ClientConfig.getFloatingLyricsColor();
        boolean outline = ClientConfig.isFloatingLyricsOutline();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int anchorX = Math.round(ClientConfig.getFloatingLyricsPosX() * screenW);
        int anchorY = Math.round(ClientConfig.getFloatingLyricsPosY() * screenH);
        
        // 使用默认字体
        {
            var font = Minecraft.getInstance().font;
            int textW = Math.round(font.width(text) * scale);
            int textH = Math.round(font.lineHeight * scale);
            int snap = 12;
            boolean nearLeft = Math.abs(anchorX - 0) < snap;
            boolean nearRight = Math.abs(anchorX - screenW) < snap;
            int drawX = anchorX - (nearLeft ? 0 : (nearRight ? textW : textW / 2));
            int drawY = Math.max(0, Math.min(anchorY, screenH - textH));
            
            var pose = gfx.pose();
            pose.pushPose();
            pose.translate(drawX, drawY, 0);
            pose.scale(scale, scale, 1);
            
            int a = (color >>> 24) & 0xFF;
            if (a <= 0) { 
                pose.popPose(); 
                return; 
            }
            
            if (outline) {
                int outlineColor = (a << 24);
                gfx.drawString(font, text, 1, 0, outlineColor, false);
                gfx.drawString(font, text, -1, 0, outlineColor, false);
                gfx.drawString(font, text, 0, 1, outlineColor, false);
                gfx.drawString(font, text, 0, -1, outlineColor, false);
            }
            gfx.drawString(font, text, 0, 0, color, false);
            pose.popPose();
        }
    }

    /**
     * 取当前播放曲目对应的歌词，结果按路径缓存；换曲时重新解析。
     */
    private static Lyrics loadOverlayLyrics(String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) return Lyrics.empty();
        if (overlayLyrics != null && overlayLyricsForPath != null
                && overlayLyricsForPath.equalsIgnoreCase(absolutePath)) {
            return overlayLyrics;
        }
        overlayLyrics = MusicLibrary.loadLyrics(absolutePath);
        overlayLyricsForPath = absolutePath;
        return overlayLyrics;
    }

    // 提供当前位置的浮动歌词文本，供位置设置界面预览/拖拽
    public static String getCurrentFloatingLyricText() {
        try {
            if (PLAYER == null || !PLAYER.isOutputReady()) return null;
            String path = PLAYER.getLastPlayedAbsolutePath();
            if (path == null || path.isBlank()) return null;
            var lines = loadOverlayLyrics(path).getLines();
            if (lines.isEmpty()) return null;
            int curMs = (PLAYER != null) ? PLAYER.getPositionMs() : 0;
            int idx = Lyrics.findLineIndexAt(lines, curMs);
            if (idx < 0 || idx >= lines.size()) return null;
            String text = lines.get(idx).text;
            return (text == null || text.isBlank()) ? null : text;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
