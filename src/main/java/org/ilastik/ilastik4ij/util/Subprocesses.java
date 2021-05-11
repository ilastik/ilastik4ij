package org.ilastik.ilastik4ij.util;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class Subprocesses {
    private Subprocesses() {
    }

    /**
     * Run a subprocess.
     * <p>
     * Synchronously start a subprocess and wait for it's completion,
     * redirecting standard output and standard error to consumers.
     *
     * @param cmd    executable path and arguments
     * @param env    environment variables
     * @param stdout consumes standard output
     * @param stderr consumes standard error
     * @throws RuntimeException the process is finished with non-zero exit code
     */
    public static void run(List<String> cmd,
                           Map<String, String> env,
                           Consumer<? super String> stdout,
                           Consumer<? super String> stderr) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().putAll(env);
        Process p = pb.start();

        try (AutoStartJoin ignored1 = new AutoStartJoin(lineConsumer(p.getInputStream(), stdout));
             AutoStartJoin ignored2 = new AutoStartJoin(lineConsumer(p.getErrorStream(), stderr))) {
            if (p.waitFor() != 0) {
                throw new RuntimeException("non-zero exit code " + p.exitValue());
            }
        }
    }

    private static Runnable lineConsumer(InputStream is, Consumer<? super String> f) {
        return () -> new BufferedReader(new InputStreamReader(is)).lines().forEachOrdered(f);
    }

    private static final class AutoStartJoin implements AutoCloseable {
        final Thread thread;

        AutoStartJoin(Runnable r) {
            thread = new Thread(r);
            thread.start();
        }

        @Override
        public void close() throws InterruptedException {
            thread.join();
        }
    }
}