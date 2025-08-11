package com.tqk114514.craftmusic.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LrcParser {
    private static final Pattern TIME_TOKEN = Pattern.compile("\\[(\\d{1,2}):(\\d{1,2})(?:\\.(\\d{1,3}))?]\\s*(.*)");
    private static final Pattern TAG_TOKEN = Pattern.compile("\\[(ar|ti|al|by|offset):([^]]*)]", Pattern.CASE_INSENSITIVE);

    private LrcParser() {}

    public static Lyrics parse(Path lrcPath) {
        if (lrcPath == null || !Files.isRegularFile(lrcPath)) return Lyrics.empty();
        // 尝试多种编码：UTF-8 → GBK
        for (Charset cs : new Charset[]{StandardCharsets.UTF_8, Charset.forName("GBK")}) {
            try {
                Lyrics l = parseWithCharset(lrcPath, cs);
                if (!l.isEmpty() || cs.equals(StandardCharsets.UTF_8)) return l; // 有内容或UTF-8解析成功
            } catch (IOException ignored) {}
        }
        return Lyrics.empty();
    }

    private static Lyrics parseWithCharset(Path lrcPath, Charset cs) throws IOException {
        List<Lyrics.Line> lines = new ArrayList<>();
        Map<String, String> tags = new LinkedHashMap<>();
        int globalOffset = 0;
        try (BufferedReader br = Files.newBufferedReader(lrcPath, cs)) {
            String raw;
            while ((raw = br.readLine()) != null) {
                String line = raw.strip();
                if (line.isEmpty()) continue;

                // 标签行
                Matcher tm = TAG_TOKEN.matcher(line);
                boolean matchedAnyTag = false;
                while (tm.find()) {
                    matchedAnyTag = true;
                    String key = tm.group(1).toLowerCase(Locale.ROOT);
                    String value = tm.group(2).trim();
                    if ("offset".equals(key)) {
                        try { globalOffset = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                    } else {
                        tags.put(key, value);
                    }
                }
                if (matchedAnyTag && !line.startsWith("[")) continue;

                // 时间戳 + 文本
                Matcher m = TIME_TOKEN.matcher(line);
                if (m.matches()) {
                    int mm = parseInt(m.group(1));
                    int ss = parseInt(m.group(2));
                    int ms = parseMs(m.group(3));
                    int timeMs = mm * 60_000 + ss * 1_000 + ms;
                    String text = m.group(4).strip();
                    lines.add(new Lyrics.Line(timeMs, text));
                }
            }
        }
        // 偏移
        if (globalOffset != 0) {
            for (int i = 0; i < lines.size(); i++) {
                Lyrics.Line ln = lines.get(i);
                lines.set(i, new Lyrics.Line(Math.max(0, ln.timeMs + globalOffset), ln.text));
            }
        }
        // 按时间排序并去重
        lines.sort(Comparator.comparingInt(a -> a.timeMs));
        List<Lyrics.Line> dedup = new ArrayList<>();
        int lastTime = -1; String lastText = null;
        for (Lyrics.Line ln : lines) {
            if (ln.timeMs != lastTime || !Objects.equals(ln.text, lastText)) {
                dedup.add(ln);
                lastTime = ln.timeMs; lastText = ln.text;
            }
        }
        return new Lyrics(dedup, tags, globalOffset);
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
    private static int parseMs(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            if (s.length() == 1) return Integer.parseInt(s) * 100;
            if (s.length() == 2) return Integer.parseInt(s) * 10;
            return Integer.parseInt(s.substring(0, Math.min(3, s.length())));
        } catch (Exception e) { return 0; }
    }
}

