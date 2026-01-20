package com.hypixel.hytale.server.core.plugin;

import com.hypixel.hytale.server.core.event.EventRegistry;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Stub for Hytale server API - JavaPlugin.
 * This file is provided for compilation purposes only.
 * The actual implementation is provided by the Hytale server at runtime.
 */
public abstract class JavaPlugin {
    private final JavaPluginInit init;

    protected JavaPlugin(@Nonnull JavaPluginInit init) {
        this.init = init;
    }

    /**
     * Called before setup for async configuration loading.
     */
    protected CompletableFuture<Void> preLoad() {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called during plugin setup phase.
     */
    protected void setup() {
    }

    /**
     * Called after all plugins complete setup.
     */
    protected void start() {
    }

    /**
     * Called during plugin shutdown.
     */
    protected void shutdown() {
    }

    /**
     * Get the plugin's logger.
     */
    protected Logger getLogger() {
        return Logger.getLogger(getClass().getName());
    }

    /**
     * Get the plugin's data directory.
     */
    protected Path getDataDirectory() {
        return Path.of("data/mods/" + getClass().getSimpleName());
    }

    /**
     * Get the event registry for registering event listeners.
     */
    protected EventRegistry getEventRegistry() {
        return new EventRegistry();
    }
}
