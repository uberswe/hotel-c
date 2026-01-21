package com.hypixel.hytale.server.core.plugin;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Stub class for Hytale JavaPlugin API.
 * This is a compile-time stub - the real implementation is provided by the Hytale server at runtime.
 */
public abstract class JavaPlugin {

    protected JavaPlugin(@Nonnull JavaPluginInit init) {
    }

    protected abstract void setup();

    protected abstract void start();

    protected abstract void shutdown();

    public CompletableFuture<Void> preLoad() {
        return CompletableFuture.completedFuture(null);
    }

    public HytaleLogger getLogger() {
        throw new UnsupportedOperationException("Stub");
    }

    public Path getDataDirectory() {
        throw new UnsupportedOperationException("Stub");
    }

    public EventRegistry getEventRegistry() {
        throw new UnsupportedOperationException("Stub");
    }

    public ComponentRegistryProxy getEntityStoreRegistry() {
        throw new UnsupportedOperationException("Stub");
    }
}
