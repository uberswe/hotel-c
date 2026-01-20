package com.uberswe.hytale.otel.listeners;

import com.uberswe.hytale.otel.config.PluginConfig;
import com.uberswe.hytale.otel.telemetry.TelemetryManager;
import com.hypixel.hytale.server.core.event.EventHandler;
import com.hypixel.hytale.server.core.event.EventListener;
import com.hypixel.hytale.server.core.event.EventPriority;
import com.hypixel.hytale.server.core.event.world.AddWorldEvent;
import com.hypixel.hytale.server.core.event.world.RemoveWorldEvent;
import com.hypixel.hytale.server.core.event.world.StartWorldEvent;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

import java.util.logging.Logger;

/**
 * Listens to world events and records metrics.
 */
public class WorldEventListener implements EventListener {
    private static final AttributeKey<String> WORLD_NAME = AttributeKey.stringKey("world.name");
    private static final AttributeKey<String> WORLD_ID = AttributeKey.stringKey("world.id");

    private final Logger logger;
    private final TelemetryManager telemetry;
    private final PluginConfig config;

    public WorldEventListener(Logger logger, TelemetryManager telemetry, PluginConfig config) {
        this.logger = logger;
        this.telemetry = telemetry;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldAdd(AddWorldEvent event) {
        if (!config.getMetrics().getWorldMetrics().isEnabled() ||
            !config.getMetrics().getWorldMetrics().isTrackWorldLoading()) {
            return;
        }

        try {
            var world = event.getWorld();
            var attributes = Attributes.builder()
                    .put(WORLD_NAME, world.getName())
                    .put(WORLD_ID, world.getUniqueId().toString())
                    .build();

            telemetry.recordWorldLoaded(attributes);
            logger.fine("Recorded world loaded: " + world.getName());
        } catch (Exception e) {
            logger.warning("Failed to record world add event: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldRemove(RemoveWorldEvent event) {
        if (!config.getMetrics().getWorldMetrics().isEnabled() ||
            !config.getMetrics().getWorldMetrics().isTrackWorldLoading()) {
            return;
        }

        try {
            var world = event.getWorld();
            var attributes = Attributes.builder()
                    .put(WORLD_NAME, world.getName())
                    .put(WORLD_ID, world.getUniqueId().toString())
                    .build();

            telemetry.recordWorldUnloaded(attributes);
            logger.fine("Recorded world unloaded: " + world.getName());
        } catch (Exception e) {
            logger.warning("Failed to record world remove event: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldStart(StartWorldEvent event) {
        // World has been started and is ready for players
        try {
            var world = event.getWorld();
            logger.fine("World started: " + world.getName());
        } catch (Exception e) {
            logger.warning("Failed to process world start event: " + e.getMessage());
        }
    }
}
