package com.yfy.createcinema.client.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Prepares the JavaCPP native cache for this run.
 * <p>
 * JavaCPP normally extracts every native library on each load by deleting the cached copy and writing it again
 * ({@code Loader.extractResource}). On Windows a security product (or a previous game process that has not exited yet)
 * can hold the freshly written DLL, the rewrite fails, and because that happens inside a class initialiser the whole
 * FFmpeg stack stays broken for the session - the infamous
 * {@code FileNotFoundException: ...jniavutil.dll (being used by another process)}.
 * <p>
 * Two things avoid it:
 * <ol>
 *   <li>a cache directory that belongs to this JVM only, so a leftover directory from an earlier run cannot conflict;</li>
 *   <li>extracting the DLLs here, with retries, and then pointing JavaCPP's {@code pathsFirst} /
 *       {@code platform.preloadpath} at that directory. {@code Loader.findLibrary} then returns {@code file:} URLs and
 *       {@code loadLibrary} maps them directly instead of deleting and rewriting them.</li>
 * </ol>
 */
final class NativeLibraries {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NativeLibraries.class);
    private static final String INDEX_RESOURCE = "/createcinema-natives.txt";
    private static final long SIZE_UNKNOWN = -1L;

    private NativeLibraries() {
    }

    /** Returns a cache directory used by this JVM only, cleaning up leftovers from earlier runs. */
    static Path perProcessCacheDir(Path baseDirectory) {
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException error) {
            LOGGER.warn("Could not create native cache root {}", baseDirectory, error);
        }
        long pid = currentPid();
        String ownDirectory = "run-" + pid;
        cleanupOldRuns(baseDirectory, ownDirectory);
        Path directory = baseDirectory.resolve(ownDirectory);
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            LOGGER.warn("Could not create native cache {}", directory, error);
        }
        return directory;
    }

    /**
     * Copies every bundled native for {@code platform} into {@code cacheDir/<platform>}.
     *
     * @return the directory holding the extracted libraries
     */
    static Path prepare(Path cacheDir, String platform) {
        Path target = cacheDir.resolve(platform);
        try {
            Files.createDirectories(target);
        } catch (IOException error) {
            LOGGER.warn("Could not create native cache {}", target, error);
            return target;
        }
        List<String[]> entries = readIndex(platform);
        if (entries.isEmpty()) {
            LOGGER.warn("Bundled native index has no entries for {}", platform);
            return target;
        }
        int copied = 0;
        int reused = 0;
        int skipped = 0;
        for (String[] entry : entries) {
            String resource = entry[0];
            String fileName = resource.substring(resource.lastIndexOf('/') + 1);
            long expectedSize = parseSize(entry[1]);
            Path destination = target.resolve(fileName);
            if (isComplete(destination, expectedSize)) {
                reused++;
                continue;
            }
            if (copyWithRetry(resource, destination, fileName, expectedSize)) copied++;
            else skipped++;
        }
        LOGGER.info("FFmpeg natives ready in {} (copied {}, reused {}, skipped {})", target, copied, reused, skipped);
        return target;
    }

    private static List<String[]> readIndex(String platform) {
        List<String[]> entries = new ArrayList<>();
        try (InputStream input = NativeLibraries.class.getResourceAsStream(INDEX_RESOURCE)) {
            if (input == null) return entries;
            String content = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String line : content.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                int split = trimmed.lastIndexOf(' ');
                String resource = split < 0 ? trimmed : trimmed.substring(0, split);
                String size = split < 0 ? "" : trimmed.substring(split + 1);
                if (!resource.startsWith("org/bytedeco/")) continue;
                if (!resource.contains("/" + platform + "/")) continue;
                entries.add(new String[]{resource, size});
            }
        } catch (IOException error) {
            LOGGER.warn("Could not read the bundled native index", error);
        }
        return entries;
    }

    private static boolean isComplete(Path file, long expectedSize) {
        try {
            if (!Files.isRegularFile(file)) return false;
            if (expectedSize <= 0L) return true;
            return Files.size(file) == expectedSize;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean copyWithRetry(String resource, Path destination, String fileName, long expectedSize) {
        for (int attempt = 1; attempt <= 8; attempt++) {
            try (InputStream input = NativeLibraries.class.getResourceAsStream("/" + resource)) {
                if (input == null) {
                    LOGGER.debug("Bundled native {} is missing from the jar", resource);
                    return false;
                }
                Path temporary = destination.resolveSibling(fileName + ".tmp");
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
                if (expectedSize > 0L && Files.size(temporary) != expectedSize) {
                    Files.deleteIfExists(temporary);
                    throw new IOException("size mismatch for " + resource);
                }
                try {
                    Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException moveFailure) {
                    Files.copy(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(temporary);
                }
                return true;
            } catch (IOException error) {
                if (attempt == 8) {
                    LOGGER.warn("Could not prepare native {} after {} attempts: {}",
                            resource, attempt, error.toString());
                    return false;
                }
                sleep(150L * attempt);
            }
        }
        return false;
    }

    private static long currentPid() {
        try {
            return ProcessHandle.current().pid();
        } catch (Throwable ignored) {
            return Math.abs(System.nanoTime() % 100_000L);
        }
    }

    private static void cleanupOldRuns(Path baseDirectory, String ownDirectory) {
        try (var children = Files.list(baseDirectory)) {
            for (Path child : children.toList()) {
                if (!Files.isDirectory(child)) continue;
                if (child.getFileName().toString().equals(ownDirectory)) continue;
                // "run-<pid>" directories are leftovers from earlier launches; the older single "<platform>"
                // directory is the pre-per-process layout and is no longer used either.
                deleteRecursively(child);
            }
        } catch (IOException ignored) {
            // best effort only - a directory that is still in use simply stays around
        }
    }

    private static void deleteRecursively(Path directory) {
        try (var walk = Files.walk(directory)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // in use by another process; leave it for a later run
                }
            }
        } catch (IOException ignored) {
            // best effort only
        }
    }

    private static long parseSize(String raw) {
        try {
            return raw == null || raw.isEmpty() ? SIZE_UNKNOWN : Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return SIZE_UNKNOWN;
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
