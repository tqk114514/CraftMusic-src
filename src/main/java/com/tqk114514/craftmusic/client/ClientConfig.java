package com.tqk114514.craftmusic.client;

import com.tqk114514.craftmusic.CraftMusic;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientConfig {
    private static final String FILE_NAME = "craftmusic-client.json";
    private static volatile boolean initialized = false;
    private static volatile float volume = 1.0f;
    private static volatile String mode = "REPEAT_ALL"; // SEQUENTIAL, REPEAT_ALL, SHUFFLE, REPEAT_ONE
    private static volatile boolean lyricEffects = true; // 歌词效果默认开
    private static volatile boolean floatingLyrics = false; // 悬浮歌词默认关
    private static volatile String floatingLyricsRender = "GLOBAL"; // GLOBAL or WORLD
    // 新增：悬浮歌词可视化设置
    private static volatile float floatingLyricsPosX = 0.02f; // 相对坐标 0..1（左上）
    private static volatile float floatingLyricsPosY = 0.02f;
    private static volatile float floatingLyricsFontScale = 1.0f; // 1.0=原版
    private static volatile boolean floatingLyricsOutline = false; // 描边
    private static volatile int floatingLyricsColor = 0xFFFFFFFF; // ARGB 颜色
    private static volatile boolean spectrumEnabled = false; // 频谱可视化默认关
    private static volatile boolean floatingLyricsSnap = true; // 悬浮歌词位置设置：吸附开关，默认开

    private ClientConfig() {}

    public static synchronized void initAndLoad() {
        if (initialized) return;
        initialized = true;
        Path f = getConfigFile();
        boolean needSave = false;
        if (Files.exists(f)) {
            try {
                String json = Files.readString(f, StandardCharsets.UTF_8);
                Float v = parseVolume(json);
                if (v != null) volume = clamp(v);
                String m = parseMode(json);
                if (m != null && !m.isBlank()) mode = m.trim();
                Boolean le = parseBoolean(json, "lyricEffects");
                if (le != null) {
                    lyricEffects = le;
                } else {
                    needSave = true; // 缺失该字段则立刻写入默认
                }
                Boolean fl = parseBoolean(json, "floatingLyrics");
                if (fl != null) {
                    floatingLyrics = fl;
                } else {
                    needSave = true; // 缺失该字段则立刻写入默认（false）
                }
                String fr = parseString(json, "floatingLyricsRender");
                if (fr != null && !fr.isBlank()) {
                    setFloatingLyricsRenderInternal(fr.trim());
                } else {
                    needSave = true; // 缺失则写入默认 GLOBAL
                }
                // 兼容旧字段，转换为相对坐标
                String fp = parseString(json, "floatingLyricsPosition");
                if (fp != null && !fp.isBlank()) {
                    String p = fp.trim().toUpperCase();
                    switch (p) {
                        case "TOP_LEFT" -> { floatingLyricsPosX = 0.02f; floatingLyricsPosY = 0.02f; }
                        case "TOP_RIGHT" -> { floatingLyricsPosX = 0.98f; floatingLyricsPosY = 0.02f; }
                        case "BOTTOM_LEFT" -> { floatingLyricsPosX = 0.02f; floatingLyricsPosY = 0.95f; }
                        case "BOTTOM_RIGHT" -> { floatingLyricsPosX = 0.98f; floatingLyricsPosY = 0.95f; }
                        default -> { floatingLyricsPosX = 0.02f; floatingLyricsPosY = 0.02f; }
                    }
                    needSave = true;
                }
                Float px = parseFloat(json, "floatingLyricsPosX");
                Float py = parseFloat(json, "floatingLyricsPosY");
                if (px != null && py != null) { floatingLyricsPosX = clamp(px); floatingLyricsPosY = clamp(py); } else { needSave = true; }
                Float fs = parseFloat(json, "floatingLyricsFontScale");
                if (fs != null) { floatingLyricsFontScale = clampRange(fs, 0.5f, 3.0f); } else { needSave = true; }
                Boolean fo = parseBoolean(json, "floatingLyricsOutline");
                if (fo != null) { floatingLyricsOutline = fo; } else { needSave = true; }
                Integer col = parseInt(json, "floatingLyricsColor");
                if (col != null) { floatingLyricsColor = col; } else { needSave = true; }
                Boolean snap = parseBoolean(json, "floatingLyricsSnap");
                if (snap != null) { floatingLyricsSnap = snap; } else { needSave = true; }
                Boolean sp = parseBoolean(json, "spectrumEnabled");
                if (sp != null) {
                    spectrumEnabled = sp;
                } else {
                    needSave = true; // 缺失则写入默认 false
                }
            } catch (IOException e) {
                CraftMusic.LOGGER.warn("ClientConfig load failed: {}", e.toString());
            }
        } else {
            needSave = true; // 文件不存在，立即创建默认
        }
        if (needSave) save();
    }

    public static float getVolume() {
        if (!initialized) initAndLoad();
        return volume;
    }

    public static void setVolume(float v) {
        if (!initialized) initAndLoad();
        volume = clamp(v);
        save();
    }

    private static void save() {
        try {
            Path f = getConfigFile();
            if (!Files.exists(f.getParent())) Files.createDirectories(f.getParent());
            String json = "{\n" +
                    "  \"volume\": " + Float.toString(volume) + ",\n" +
                    "  \"mode\": \"" + mode + "\",\n" +
                    "  \"lyricEffects\": " + Boolean.toString(lyricEffects) + ",\n" +
                    "  \"floatingLyrics\": " + Boolean.toString(floatingLyrics) + ",\n" +
                    "  \"floatingLyricsRender\": \"" + floatingLyricsRender + "\",\n" +
                    "  \"floatingLyricsPosX\": " + floatingLyricsPosX + ",\n" +
                    "  \"floatingLyricsPosY\": " + floatingLyricsPosY + ",\n" +
                    "  \"floatingLyricsFontScale\": " + floatingLyricsFontScale + ",\n" +
                    "  \"floatingLyricsOutline\": " + Boolean.toString(floatingLyricsOutline) + ",\n" +
                    "  \"floatingLyricsColor\": " + floatingLyricsColor + ",\n" +
                    "  \"floatingLyricsSnap\": " + Boolean.toString(floatingLyricsSnap) + ",\n" +
                    "  \"spectrumEnabled\": " + Boolean.toString(spectrumEnabled) + "\n" +
                    "}";
            Files.writeString(f, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            CraftMusic.LOGGER.warn("ClientConfig save failed: {}", e.toString());
        }
    }

    private static Path getConfigFile() {
        try {
            // 优先使用 NeoForge 提供的 CONFIGDIR，避免早期阶段 Minecraft 实例未就绪导致路径错误
            Path cfgDir = FMLPaths.CONFIGDIR.get();
            return cfgDir.resolve(FILE_NAME);
        } catch (Throwable ignored) {
            Minecraft mc = Minecraft.getInstance();
            Path gameDir = (mc != null && mc.gameDirectory != null) ? mc.gameDirectory.toPath() : Path.of(".");
            Path cfgDir = gameDir.resolve("config");
            return cfgDir.resolve(FILE_NAME);
        }
    }

    private static Float parseVolume(String json) {
        if (json == null) return null;
        int idx = json.indexOf("\"volume\"");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int start = colon + 1;
        // 跳过空白
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        // 读取可能的有符号小数/科学计数法（简单容错）
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '+' || c == '-' || c == '.' || (c >= '0' && c <= '9') || c == 'e' || c == 'E') {
                end++;
            } else {
                break;
            }
        }
        String token = json.substring(start, end).trim();
        if (token.isEmpty()) return null;
        try {
            return Float.parseFloat(token);
        } catch (Exception ignored) { return null; }
    }

    private static String parseMode(String json) {
        if (json == null) return null;
        int idx = json.indexOf("\"mode\"");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        char c = start < json.length() ? json.charAt(start) : 0;
        if (c == '"' || c == '\'') {
            char quote = c;
            int end = json.indexOf(quote, start + 1);
            if (end > start) return json.substring(start + 1, end);
            return null;
        } else {
            int end = start;
            while (end < json.length() && !Character.isWhitespace(json.charAt(end)) && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }

    public static String getMode() {
        if (!initialized) initAndLoad();
        return mode;
    }

    public static void setMode(String m) {
        if (m == null || m.isBlank()) return;
        mode = m.trim();
        save();
    }

    public static boolean isLyricEffects() {
        if (!initialized) initAndLoad();
        return lyricEffects;
    }

    public static void setLyricEffects(boolean v) {
        lyricEffects = v;
        save();
    }

    public static boolean isFloatingLyrics() {
        if (!initialized) initAndLoad();
        return floatingLyrics;
    }

    public static void setFloatingLyrics(boolean v) {
        if (!initialized) initAndLoad();
        floatingLyrics = v;
        save();
    }

    public static String getFloatingLyricsRender() {
        if (!initialized) initAndLoad();
        return floatingLyricsRender;
    }

    public static void setFloatingLyricsRender(String mode) {
        if (!initialized) initAndLoad();
        setFloatingLyricsRenderInternal(mode);
        save();
    }

    private static void setFloatingLyricsRenderInternal(String mode) {
        String m = (mode == null) ? "" : mode.toUpperCase();
        if (!"GLOBAL".equals(m) && !"WORLD".equals(m)) {
            m = "GLOBAL";
        }
        floatingLyricsRender = m;
    }

    // 新接口：悬浮歌词参数
    public static float getFloatingLyricsPosX() { if (!initialized) initAndLoad(); return floatingLyricsPosX; }
    public static float getFloatingLyricsPosY() { if (!initialized) initAndLoad(); return floatingLyricsPosY; }
    public static void setFloatingLyricsPos(float x, float y) { floatingLyricsPosX = clamp(x); floatingLyricsPosY = clamp(y); save(); }
    public static float getFloatingLyricsFontScale() { if (!initialized) initAndLoad(); return floatingLyricsFontScale; }
    public static void setFloatingLyricsFontScale(float s) { floatingLyricsFontScale = clampRange(s, 0.5f, 3.0f); save(); }
    public static boolean isFloatingLyricsOutline() { if (!initialized) initAndLoad(); return floatingLyricsOutline; }
    public static void setFloatingLyricsOutline(boolean v) { floatingLyricsOutline = v; save(); }
    public static int getFloatingLyricsColor() { if (!initialized) initAndLoad(); return floatingLyricsColor; }
    public static void setFloatingLyricsColor(int c) { floatingLyricsColor = c; save(); }

    public static boolean isSpectrumEnabled() {
        if (!initialized) initAndLoad();
        return spectrumEnabled;
    }

    public static void setSpectrumEnabled(boolean v) {
        if (!initialized) initAndLoad();
        spectrumEnabled = v;
        save();
    }

    public static boolean isFloatingLyricsSnap() {
        if (!initialized) initAndLoad();
        return floatingLyricsSnap;
    }

    public static void setFloatingLyricsSnap(boolean v) {
        if (!initialized) initAndLoad();
        floatingLyricsSnap = v;
        save();
    }


    private static Integer parseInt(String json, String key) {
        Float f = parseFloat(json, key);
        if (f == null) return null;
        return (int)f.floatValue();
    }

    private static Boolean parseBoolean(String json, String key) {
        if (json == null || key == null) return null;
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (json.regionMatches(true, start, "true", 0, 4)) return true;
        if (json.regionMatches(true, start, "false", 0, 5)) return false;
        return null;
    }

    private static String parseString(String json, String key) {
        if (json == null || key == null) return null;
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        char c = start < json.length() ? json.charAt(start) : 0;
        if (c == '"' || c == '\'') {
            char quote = c;
            int end = json.indexOf(quote, start + 1);
            if (end > start) return json.substring(start + 1, end);
            return null;
        } else {
            int end = start;
            while (end < json.length() && !Character.isWhitespace(json.charAt(end)) && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }

    private static Float parseFloat(String json, String key) {
        if (json == null || key == null) return null;
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '+' || c == '-' || c == '.' || (c >= '0' && c <= '9') || c == 'e' || c == 'E') end++; else break;
        }
        try { return Float.parseFloat(json.substring(start, end).trim()); } catch (Exception ignored) { return null; }
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float clampRange(float v, float lo, float hi) { if (v < lo) return lo; if (v > hi) return hi; return v; }

}

