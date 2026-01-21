package com.hypixel.hytale.logger;

import java.util.logging.Level;

/**
 * Stub class for Hytale HytaleLogger API.
 * Based on actual Hytale server API from SimpleClaims analysis.
 */
public abstract class HytaleLogger {

    /**
     * Get a logging API for the specified level.
     * Usage: logger.at(Level.INFO).log("message")
     */
    public abstract Api at(Level level);

    /**
     * Logging API interface returned by at(Level).
     */
    public interface Api {
        void log(String message);
        Api withCause(Throwable cause);
    }
}
