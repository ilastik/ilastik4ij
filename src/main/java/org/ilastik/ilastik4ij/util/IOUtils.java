package org.ilastik.ilastik4ij.util;

import org.scijava.log.LogService;

import java.io.*;
import java.nio.charset.Charset;

public class IOUtils {
    public static String getTemporaryFileName(String extension) throws IOException {
        File tmpFile = File.createTempFile("ilastik4j", extension, null);
        try {
            return tmpFile.getAbsolutePath();
        } finally {
            tmpFile.delete();
        }
    }

    public static void redirectOutputToLogService(final InputStream in, final LogService logService, final Boolean isErrorStream) {
        new Thread(() -> {

            String line;

            try (BufferedReader bis = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()))) {
                while ((line = bis.readLine()) != null) {
                    if (isErrorStream) {
                        logService.error(line);
                    } else {
                        logService.info(line);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read ilastik output", e);
            }
        }).start();
    }
}
