package org.ilastik.ilastik4ij.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class Subprocesses {
    private Subprocesses() {
    }

    /**
     * Run a subprocess.
     *
     * Synchronously start a subprocess and wait for it's completion,
     * redirecting standard output and standard error to consumers.
     *
     * @param args executable path and arguments
     * @param env environment variables
     * @param stdout consumes standard output
     * @param stderr consumes standard error
     * @throws RuntimeException if the process is finished with non-zero exit code
     */
    public static void run(
            List<String> args,
            Map<String, String> env,
            Consumer<? super String> stdout,
            Consumer<? super String> stderr)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.environment().putAll(env);

        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            Process p = pb.start();

            pool.submit(lineConsumer(p.getInputStream(), stdout));
            pool.submit(lineConsumer(p.getErrorStream(), stderr));

            int exitCode = p.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("non-zero exit code " + exitCode);
            }

        } finally {
            pool.shutdown();
        }
    }

    private static Runnable lineConsumer(InputStream is, Consumer<? super String> f) {
        return () -> new BufferedReader(new InputStreamReader(is))
                .lines()
                .forEachOrdered(f);
    }
}
