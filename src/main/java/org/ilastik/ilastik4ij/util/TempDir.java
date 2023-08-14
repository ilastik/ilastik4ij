package org.ilastik.ilastik4ij.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Create a temporary directory {@link #path}, and recursively delete it on {@link #close()}.
 */
public final class TempDir implements AutoCloseable {
    private static final FileVisitor<Path> RECURSIVE_DELETE = new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    public final Path path;

    public TempDir(String prefix) throws IOException {
        path = Files.createTempDirectory(Objects.requireNonNull(prefix));
    }

    @Override
    public void close() throws IOException {
        Files.walkFileTree(path, RECURSIVE_DELETE);
    }
}
