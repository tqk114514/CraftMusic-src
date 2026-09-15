package com.tqk114514.craftmusic.client;

import com.tqk114514.craftmusic.CraftMusic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MusicLibrary {
    private static final String FOLDER_NAME = "CraftMusic";
    private static final String[] EXTENSIONS = new String[]{".ogg", ".mp3", ".flac", ".wav"};

    private static Path libraryDir;
    private static volatile List<Path> tracks = Collections.emptyList();
    private static volatile List<TrackInfo> trackInfos = Collections.emptyList();
    // 绝对路径（小写）→ TrackInfo，避免每次按曲目查歌词都线性扫描整个库
    private static volatile Map<String, TrackInfo> trackIndex = Collections.emptyMap();

    private MusicLibrary() {}

    public static void initAndScan() {
        try {
            ensureFolderExists();
            scan();
        } catch (Exception e) {
            CraftMusic.LOGGER.warn("MusicLibrary init error: {}", e.toString());
        }
    }

    public static void ensureFolderExists() throws IOException {
        Path dir = getLibraryDir();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    public static synchronized void scan() {
        Path dir = getLibraryDir();
        List<Path> resultPaths = new ArrayList<>();
        List<TrackInfo> resultInfos = new ArrayList<>();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.list(dir)) {
                List<Path> audioFiles = s.filter(Files::isRegularFile)
                        .filter(MusicLibrary::isSupported)
                        .sorted((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()))
                        .collect(Collectors.toList());
                for (Path audio : audioFiles) {
                    Path lrc = findLyricsFor(audio);
                    boolean hasLyrics = lrc != null && Files.exists(lrc);
                    resultPaths.add(audio);
                    resultInfos.add(new TrackInfo(audio, hasLyrics, hasLyrics ? lrc : null));
                }
            } catch (IOException e) {
                CraftMusic.LOGGER.warn("MusicLibrary scan error: {}", e.toString());
            }
        }
        tracks = Collections.unmodifiableList(resultPaths);
        trackInfos = Collections.unmodifiableList(resultInfos);
        Map<String, TrackInfo> idx = new HashMap<>();
        for (TrackInfo info : resultInfos) {
            idx.put(key(info.getAudioPath()), info);
        }
        trackIndex = Collections.unmodifiableMap(idx);
        CraftMusic.LOGGER.info("MusicLibrary scanned {} tracks in {}", tracks.size(), dir);
    }

    public static List<Path> getTracks() {
        return tracks;
    }

    public static List<TrackInfo> getTrackInfos() {
        return trackInfos;
    }

    /**
     * 按音频绝对路径查找曲目，找不到返回 null。查找与大小写无关。
     */
    public static TrackInfo findTrack(String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) return null;
        try {
            return trackIndex.get(key(Path.of(absolutePath)));
        } catch (RuntimeException e) {
            // 路径可能来自 /craftmusic play 的任意用户输入，非法路径不应当抛出去
            return null;
        }
    }

    /**
     * 按音频绝对路径加载歌词；曲目不存在或无同名 lrc 时返回空歌词。
     */
    public static Lyrics loadLyrics(String absolutePath) {
        TrackInfo info = findTrack(absolutePath);
        if (info == null || info.getLyricsPath() == null) return Lyrics.empty();
        return LrcParser.parse(info.getLyricsPath());
    }

    private static String key(Path audioPath) {
        return audioPath.toAbsolutePath().toString().toLowerCase(Locale.ROOT);
    }

    /**
     * 注入游戏目录。必须由加载器层（NeoForge 的客户端初始化）在调用任何其他方法前完成，
     * 这样本类就不必直接依赖 Minecraft 客户端类，便于将来复用于其他加载器。
     */
    public static void initialize(Path gameDirectory) {
        if (gameDirectory == null) {
            throw new IllegalArgumentException("gameDirectory must not be null");
        }
        libraryDir = gameDirectory.resolve(FOLDER_NAME);
    }

    public static Path getLibraryDir() {
        Path dir = libraryDir;
        if (dir == null) {
            throw new IllegalStateException("MusicLibrary 尚未初始化：需先调用 initialize(gameDirectory)");
        }
        return dir;
    }

    private static boolean isSupported(Path p) {
        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String ext : EXTENSIONS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }

    private static Path findLyricsFor(Path audioPath) {
        try {
            Path parent = audioPath.getParent();
            if (parent == null) return null;
            String fileName = audioPath.getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            String base = dot > 0 ? fileName.substring(0, dot) : fileName;
            Path candidate = parent.resolve(base + ".lrc");
            if (Files.exists(candidate)) return candidate;
        } catch (Exception ignored) {
        }
        return null;
    }

    public static final class TrackInfo {
        private final Path audioPath;
        private final boolean hasLyrics;
        private final Path lyricsPath;

        public TrackInfo(Path audioPath, boolean hasLyrics, Path lyricsPath) {
            this.audioPath = audioPath;
            this.hasLyrics = hasLyrics;
            this.lyricsPath = lyricsPath;
        }

        public Path getAudioPath() { return audioPath; }
        public boolean hasLyrics() { return hasLyrics; }
        public Path getLyricsPath() { return lyricsPath; }
    }
}

