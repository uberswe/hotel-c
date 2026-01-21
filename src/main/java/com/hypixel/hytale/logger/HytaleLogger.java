package com.hypixel.hytale.logger;

/**
 * Stub class for Hytale HytaleLogger API.
 */
public interface HytaleLogger {

    LogBuilder atInfo();

    LogBuilder atWarning();

    LogBuilder atFine();

    interface LogBuilder {
        void log(String message);
        void log(String message, Object... args);
        LogBuilder withCause(Throwable cause);
    }
}
