package org.ilastik.ilastik4ij.executors;

import org.scijava.log.LogService;

public class LogServiceWrapper implements LoggerCallback {
    private final LogService log;

    public LogServiceWrapper(LogService log) {
        this.log = log;
    }

    @Override
    public void info(String message) {
        this.log.info(message);
    }

    @Override
    public void warn(String message) {
        this.log.warn(message);
    }

    @Override
    public void error(String message) {
        this.log.error(message);
    }
}
