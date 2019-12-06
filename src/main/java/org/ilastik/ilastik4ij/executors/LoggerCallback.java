package org.ilastik.ilastik4ij.executors;

public interface LoggerCallback {
    void info(String message);

    void warn(String message);

    void error(String message);
}
