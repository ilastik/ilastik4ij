package org.ilastik.ilastik4ij;

import org.ilastik.ilastik4ij.util.TempDir;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class TempDirTest {
    @Test
    public void testTempDirRecursivelyDeletesItselfOnClose() throws IOException {
        Path root;
        try (TempDir dir = new TempDir("test_")) {
            root = dir.path;
            Files.createFile(root.resolve("file1"));
            Path subDir = Files.createDirectory(root.resolve("dir1"));
            Files.createFile(subDir.resolve("file2"));
        }
        assertFalse(Files.exists(root));
    }
}
