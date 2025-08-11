package com.tqk114514.craftmusic.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Lyrics {
    public static final class Line {
        public final int timeMs;
        public final String text;

        public Line(int timeMs, String text) {
            this.timeMs = Math.max(0, timeMs);
            this.text = text == null ? "" : text;
        }
    }

    private final List<Line> lines;
    private final Map<String, String> tags;
    private final int offsetMs;

    public Lyrics(List<Line> lines, Map<String, String> tags, int offsetMs) {
        this.lines = lines == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(lines));
        this.tags = tags;
        this.offsetMs = offsetMs;
    }

    public List<Line> getLines() { return lines; }
    public Map<String, String> getTags() { return tags; }
    public int getOffsetMs() { return offsetMs; }
    public boolean isEmpty() { return lines.isEmpty(); }

    public static Lyrics empty() { return new Lyrics(List.of(), Map.of(), 0); }
}

