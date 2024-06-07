/*-
 * #%L
 * ilastik
 * %%
 * Copyright (C) 2017 - 2024 N/A
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package org.ilastik.ilastik4ij.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
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
