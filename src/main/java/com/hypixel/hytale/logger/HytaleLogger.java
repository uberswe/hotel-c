package com.hypixel.hytale.logger;

/**
 * Stub class for Hytale HytaleLogger API.
 */
public abstract class HytaleLogger {

    public abstract LogBuilder atInfo();

    public abstract LogBuilder atWarning();

    public abstract LogBuilder atFine();

    public interface LogBuilder {
        void log(String message);
        void log(String message, Object... args);
        LogBuilder withCause(Throwable cause);
    }
}
