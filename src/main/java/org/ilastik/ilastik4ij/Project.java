package org.ilastik.ilastik4ij;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Project {
    public enum Type {
        PIXEL_CLASSIFICATION("Pixel Classification",
                immutableList(
                        "Probabilities",
                        "Simple Segmentation",
                        "Uncertainty",
                        "Features",
                        "Labels")),

        AUTOCONTEXT("Autocontext",
                immutableList(
                        "Probabilities Stage 1",
                        "Probabilities Stage 2",
                        "Probabilities All Stages",
                        "Simple Segmentation Stage 1",
                        "Simple Segmentation Stage 2",
                        "Uncertainty Stage 1",
                        "Uncertainty Stage 2",
                        "Features Stage 1",
                        "Features Stage 2",
                        "Labels Stage 1",
                        "Labels Stage 2",
                        "Input Stage 1",
                        "Input Stage 2")),

        OBJECT_CLASSIFICATION("Object Classification",
                immutableList(
                        "Object Predictions",
                        "Object Probabilities",
                        "Blockwise Object Predictions",
                        "Blockwise Object Probabilities",
                        "Pixel Probabilities")),

        MULTICUT("Multicut",
                immutableList("Multicut Segmentation")),

        COUNTING("Counting",
                immutableList("Probabilities")),

        TRACKING("Tracking",
                // TODO: Include other options?
                immutableList("Tracking-Result"));

        public final String name;
        public final List<String> exportSources;

        Type(String name, List<String> exportSources) {
            this.name = name;
            this.exportSources = exportSources;
        }

        public static Optional<Type> fromName(String name) {
            return Arrays.stream(values())
                    .filter(v -> v.name.equals(name))
                    .findFirst();
        }
    }

    public final Path path;
    public final Type type;

    /**
     * Create ilastik project file.
     *
     * @throws RuntimeException if project file has an unknown workflow
     */
    public Project(Path path) {
        this.path = path;

        try (IHDF5Reader reader = HDF5Factory.openForReading(path.toString())) {
            String name = reader.readString("workflowName");
            type = Type.fromName(name).orElseThrow(() ->
                    new RuntimeException("unknown workflow " + name));
        }
    }

    public Project(File file) {
        this(file.toPath());
    }

    public Project(String path) {
        this(Paths.get(path));
    }

    @Override
    public String toString() {
        return MessageFormat.format("Project'{'path={0}, type={1}'}'", path, type);
    }

    @SafeVarargs
    private static <T> List<T> immutableList(T... items) {
        if (items.length == 0) {
            return Collections.emptyList();
        }
        if (items.length == 1) {
            return Collections.singletonList(items[0]);
        }
        return Collections.unmodifiableList(Arrays.asList(items));
    }
}
