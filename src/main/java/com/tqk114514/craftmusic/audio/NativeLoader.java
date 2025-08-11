package com.tqk114514.craftmusic.audio;

import com.tqk114514.craftmusic.CraftMusic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

final class NativeLoader {
    private NativeLoader() {}

    static void loadFromJarOrSystem(String baseLibraryName) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        String platformDir = detectPlatformDir(os, arch);
        String fileName = mapLibraryFileName(baseLibraryName, os);

        if (platformDir != null && fileName != null) {
            String resourcePath = "/natives/" + platformDir + "/" + fileName;
            try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
                if (in != null) {
                    Path tempDir = Files.createTempDirectory("craftmusic_native_");
                    tempDir.toFile().deleteOnExit();
                    Path target = tempDir.resolve(fileName);
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    target.toFile().deleteOnExit();
                    System.load(target.toAbsolutePath().toString());
                    CraftMusic.LOGGER.info("Loaded native library from bundled resource: {}", resourcePath);
                    return;
                } else {
                    CraftMusic.LOGGER.debug("Bundled native not found at {}. Falling back to System.loadLibrary.", resourcePath);
                }
            } catch (IOException e) {
                CraftMusic.LOGGER.warn("Failed extracting bundled native: {}", e.toString());
            } catch (UnsatisfiedLinkError e) {
                CraftMusic.LOGGER.warn("Failed loading bundled native: {}", e.toString());
            }
        }

        // Fallback to regular system search (PATH/java.library.path)
        System.loadLibrary(baseLibraryName);
        CraftMusic.LOGGER.info("Loaded native library via System.loadLibrary: {}", baseLibraryName);
    }

    private static String detectPlatformDir(String os, String arch) {
        boolean isArm = arch.contains("aarch64") || arch.contains("arm64");
        boolean isX64 = arch.contains("amd64") || arch.contains("x86_64") || arch.equals("x64");

        if (os.contains("win")) {
            if (isX64) return "windows-x86_64";
        } else if (os.contains("mac") || os.contains("darwin")) {
            if (isArm) return "macos-aarch64";
            if (isX64) return "macos-x86_64";
        } else if (os.contains("nux") || os.contains("nix")) {
            if (isX64) return "linux-x86_64";
            if (isArm) return "linux-aarch64";
        }
        return null;
    }

    private static String mapLibraryFileName(String base, String osNameLower) {
        if (osNameLower.contains("win")) {
            return base + ".dll";
        } else if (osNameLower.contains("mac") || osNameLower.contains("darwin")) {
            return "lib" + base + ".dylib";
        } else if (osNameLower.contains("nux") || osNameLower.contains("nix")) {
            return "lib" + base + ".so";
        }
        return null;
    }
}

