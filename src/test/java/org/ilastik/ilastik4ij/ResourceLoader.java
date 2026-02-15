package org.ilastik.ilastik4ij;

import org.ilastik.ilastik4ij.hdf5.Hdf5Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ResourceLoader {
    public static Path copyResource(String resourcePath, Path dstDir) {
        try (InputStream in = Hdf5Test.class.getResourceAsStream(resourcePath)) {
            Objects.requireNonNull(in);
            Path path = dstDir.resolve(resourcePath.replaceFirst("^/+", ""));
            Files.createDirectories(path.getParent());
            Files.copy(in, path);
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
