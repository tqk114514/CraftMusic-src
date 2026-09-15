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

    /**
     * 返回 timeMs 时刻所处的歌词行下标；若 timeMs 早于第一行，返回 -1。
     * 歌词行按时间升序，用二分查找。
     */
    public static int findLineIndexAt(List<Line> lines, int timeMs) {
        if (lines == null || lines.isEmpty()) return -1;
        int lo = 0, hi = lines.size() - 1, ans = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (lines.get(mid).timeMs <= timeMs) {
                ans = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }

    /** {@link #findLineIndexAt(List, int)} 的实例版本，在本对象的行上查找。 */
    public int findLineIndexAt(int timeMs) {
        return findLineIndexAt(lines, timeMs);
    }
}

