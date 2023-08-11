package org.ilastik.ilastik4ij.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Wrapper around {@link ProcessBuilder}.
 * Run subprocess with the specified arguments and environment,
 * optionally capturing output and error streams.
 */
public final class Subprocess implements IntSupplier {
    private final List<String> args;
    private final Map<String, String> env;
    private final Consumer<String> stdout;
    private final Consumer<String> stderr;

    public Subprocess(String arg) {
        this(Collections.singletonList(arg));
    }

    public Subprocess(List<String> args) {
        this(args, Collections.emptyMap());
    }

    public Subprocess(List<String> args, Map<String, String> env) {
        this(args, env, null);
    }

    public Subprocess(List<String> args, Map<String, String> env, Consumer<String> sink) {
        this(args, env, sink, sink);
    }

    public Subprocess(
            List<String> args,
            Map<String, String> env,
            Consumer<String> stdout,
            Consumer<String> stderr) {
        this.args = Objects.requireNonNull(args);
        this.env = Objects.requireNonNull(env);
        this.stdout = stdout;
        this.stderr = stderr;
    }

    @Override
    public int getAsInt() {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.environment().putAll(env);
        try {
            Process process = pb.start();
            redirect(pool, process.getInputStream(), stdout);
            redirect(pool, process.getErrorStream(), stderr);
            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            pool.shutdown();
        }
    }

    private static void redirect(ExecutorService pool, InputStream src, Consumer<String> dst) {
        if (dst == null) {
            return;
        }
        pool.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(src, StandardCharsets.UTF_8))) {
                reader.lines().forEachOrdered(dst);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
