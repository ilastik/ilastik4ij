package org.ilastik.ilastik4ij.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary directory wrapper that is used in try-with-resources.
 *
 * <pre>
 * try (TempDirCloseable temp = new TempDirCloseable("my-dir")) {
 *     Files.createFile(temp.dir.resolve("my-file"));
 * }
 * // "my-dir" and "my-file" have been deleted.
 * </pre>
 */
public final class TempDirCloseable implements AutoCloseable {
    /** Path to the temporary directory. */
    public final Path dir;

    /**
     * Create a temporary directory.
     *
     * @param prefix directory prefix, may be {@code null}.
     */
    public TempDirCloseable(String prefix) throws IOException {
        dir = Files.createTempDirectory(prefix);
    }

    /**
     * Recursively delete the temporary directory.
     *
     * @throws IOException some exceptions have been caught during deletion
     */
    @Override
    public void close() throws IOException {
        RecursiveDeleteFileVisitor fv = new RecursiveDeleteFileVisitor();
        Files.walkFileTree(dir, fv);
        if (!fv.exceptions.isEmpty()) {
            IOException e = new IOException("failed to delete temporary directory " + dir);
            fv.exceptions.forEach(e::addSuppressed);
            throw e;
        }
    }

    private static final class RecursiveDeleteFileVisitor implements FileVisitor<Path> {
        List<IOException> exceptions = new ArrayList<>();

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
                throw new RuntimeException(exc);
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