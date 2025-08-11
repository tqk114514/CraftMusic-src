package com.tqk114514.craftmusic.client;

import com.tqk114514.craftmusic.CraftMusic;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MusicLibrary {
    private static final String FOLDER_NAME = "CraftMusic";
    private static final String[] EXTENSIONS = new String[]{".ogg", ".mp3", ".flac", ".wav"};

    private static Path libraryDir;
    private static volatile List<Path> tracks = Collections.emptyList();
    private static volatile List<TrackInfo> trackInfos = Collections.emptyList();

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
        CraftMusic.LOGGER.info("MusicLibrary scanned {} tracks in {}", tracks.size(), dir);
    }

    public static List<Path> getTracks() {
        return tracks;
    }

    public static List<TrackInfo> getTrackInfos() {
        return trackInfos;
    }

    public static Path getLibraryDir() {
        if (libraryDir == null) {
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            libraryDir = gameDir.resolve(FOLDER_NAME);
        }
        return libraryDir;
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

