package org.ilastik.ilastik4ij.util;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary directory that can be used in the try-with-resources style.
 *
 * <pre>
 * try (TempDir tempDir = new TempDir("my-dir")) {
 *     Path myFile = tempDir.path.resolve("my-file");
 *     // Use myFile...
 * }
 * </pre>
 */
public class TempDir implements Closeable {
    /** Path to the temporary directory. */
    public final Path path;

    /**
     * Create a temporary directory with the specified prefix.
     */
    public TempDir(String prefix) throws IOException {
        path = Files.createTempDirectory(prefix);
    }

    /**
     * Recursively delete the temporary directory.
     *
     * If one or more exceptions occurred, throw a single exception that carries
     * all underlying exceptions as suppressed.
     */
    @Override
    public void close() throws IOException {
        RecursiveDeleteFileVisitor fv = new RecursiveDeleteFileVisitor();
        Files.walkFileTree(path, fv);
        if (!fv.exceptions.isEmpty()) {
            IOException e = new IOException("failed to delete temporary directory " + path);
            fv.exceptions.forEach(e::addSuppressed);
            throw e;
        }
    }

    private static final class RecursiveDeleteFileVisitor implements FileVisitor<Path> {
        public final List<IOException> exceptions = new ArrayList<>();

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            try {
                Files.delete(file);
            } catch (IOException e) {
                exceptions.add(e);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            exceptions.add(exc);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            if (exc != null) {
                throw new RuntimeException("unexpected exception", exc);
            }
            try {
                Files.delete(dir);
            } catch (IOException e) {
                exceptions.add(e);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
