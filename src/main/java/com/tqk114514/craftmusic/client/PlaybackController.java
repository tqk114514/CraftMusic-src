package com.tqk114514.craftmusic.client;

import com.tqk114514.craftmusic.CraftMusic;
import com.tqk114514.craftmusic.audio.MiniaudioPlayer;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

public final class PlaybackController {
    public enum PlaybackMode { SEQUENTIAL, REPEAT_ALL, SHUFFLE, REPEAT_ONE }

    private static volatile PlaybackMode mode = fromString(ClientConfig.getMode());
    private static final Random RNG = new Random();
    private static volatile boolean endHandled = false;

    private PlaybackController() {}

    public static void update(MiniaudioPlayer player) {
        if (player == null || !player.isOutputReady() || !player.isPlaying()) return;
        int len = player.getLengthMs();
        int pos = player.getPositionMs();
        if (len <= 0) return;
        if (pos >= len - 200) {
            if (!endHandled) {
                endHandled = true;
                onTrackFinished(player);
            }
        } else {
            endHandled = false;
        }
    }

    private static void onTrackFinished(MiniaudioPlayer player) {
        switch (mode) {
            case REPEAT_ONE -> replayCurrent(player);
            case SHUFFLE -> playRandomNext(player);
            case REPEAT_ALL -> playNextInList(player, true);
            case SEQUENTIAL -> playNextInList(player, false);
        }
    }

    public static void cycleMode() {
        switch (mode) {
            case SEQUENTIAL -> mode = PlaybackMode.REPEAT_ALL;
            case REPEAT_ALL -> mode = PlaybackMode.SHUFFLE;
            case SHUFFLE -> mode = PlaybackMode.REPEAT_ONE;
            case REPEAT_ONE -> mode = PlaybackMode.SEQUENTIAL;
        }
        ClientConfig.setMode(mode.name());
    }

    public static PlaybackMode getMode() { return mode; }

    public static String modeTranslationKey() {
        return switch (mode) {
            case SEQUENTIAL -> "craftmusic.mode.sequential";
            case REPEAT_ALL -> "craftmusic.mode.repeat_all";
            case SHUFFLE -> "craftmusic.mode.shuffle";
            case REPEAT_ONE -> "craftmusic.mode.repeat_one";
        };
    }

    private static PlaybackMode fromString(String m) {
        if (m == null) return PlaybackMode.REPEAT_ALL;
        try {
            return PlaybackMode.valueOf(m.trim());
        } catch (Exception ignored) {
            return PlaybackMode.REPEAT_ALL;
        }
    }

    public static void playNext(MiniaudioPlayer player) {
        switch (mode) {
            case REPEAT_ONE -> replayCurrent(player);
            case SHUFFLE -> playRandomNext(player);
            case REPEAT_ALL -> playNextInList(player, true);
            case SEQUENTIAL -> playNextInList(player, false);
        }
    }

    public static void playPrev(MiniaudioPlayer player) {
        List<Path> ts = MusicLibrary.getTracks();
        if (ts == null || ts.isEmpty()) return;
        int current = resolveCurrentIndex(player, ts);
        if (current < 0) current = 0;
        int idx;
        switch (mode) {
            case SHUFFLE -> {
                if (ts.size() == 1) idx = 0; else {
                    do { idx = RNG.nextInt(ts.size()); } while (idx == current);
                }
            }
            case REPEAT_ONE -> idx = current;
            case REPEAT_ALL, SEQUENTIAL -> idx = (current - 1 + ts.size()) % ts.size();
            default -> idx = current;
        }
        Path p = ts.get(idx);
        CraftMusic.LOGGER.info("Auto prev: {}", p);
        player.play(p.toAbsolutePath().toString());
    }

    private static void replayCurrent(MiniaudioPlayer player) {
        String last = player.getLastPlayedAbsolutePath();
        if (last != null) {
            player.play(last);
        }
    }

    private static void playRandomNext(MiniaudioPlayer player) {
        List<Path> ts = MusicLibrary.getTracks();
        if (ts == null || ts.isEmpty()) return;
        int current = resolveCurrentIndex(player, ts);
        int idx;
        if (ts.size() == 1) idx = 0; else {
            do { idx = RNG.nextInt(ts.size()); } while (idx == current);
        }
        Path p = ts.get(idx);
        CraftMusic.LOGGER.info("Auto shuffle next: {}", p);
        player.play(p.toAbsolutePath().toString());
    }

    private static void playNextInList(MiniaudioPlayer player, boolean wrap) {
        List<Path> ts = MusicLibrary.getTracks();
        if (ts == null || ts.isEmpty()) return;
        int current = resolveCurrentIndex(player, ts);
        int next = current + 1;
        if (next >= ts.size()) {
            if (!wrap) {
                // stop
                player.pause();
                return;
            }
            next = 0;
        }
        Path p = ts.get(next);
        CraftMusic.LOGGER.info("Auto next: {}", p);
        player.play(p.toAbsolutePath().toString());
    }

    private static int resolveCurrentIndex(MiniaudioPlayer player, List<Path> ts) {
        String last = player.getLastPlayedAbsolutePath();
        if (last == null) return -1;
        for (int i = 0; i < ts.size(); i++) {
            if (ts.get(i).toAbsolutePath().toString().equalsIgnoreCase(last)) return i;
        }
        return -1;
    }
}

