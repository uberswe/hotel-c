package com.uberswe.hytale.otel.listeners;

import com.uberswe.hytale.otel.config.PluginConfig;
import com.uberswe.hytale.otel.telemetry.TelemetryManager;
import com.hypixel.hytale.server.core.event.EventHandler;
import com.hypixel.hytale.server.core.event.EventListener;
import com.hypixel.hytale.server.core.event.EventPriority;
import com.hypixel.hytale.server.core.event.block.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.block.PlaceBlockEvent;
import com.hypixel.hytale.server.core.event.block.UseBlockEvent;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

import java.util.logging.Logger;

/**
 * Listens to block events and records metrics.
 */
public class BlockEventListener implements EventListener {
    private static final AttributeKey<String> BLOCK_TYPE = AttributeKey.stringKey("block.type");
    private static final AttributeKey<String> BLOCK_STATE = AttributeKey.stringKey("block.state");
    private static final AttributeKey<String> WORLD_NAME = AttributeKey.stringKey("world.name");
    private static final AttributeKey<Long> BLOCK_X = AttributeKey.longKey("block.x");
    private static final AttributeKey<Long> BLOCK_Y = AttributeKey.longKey("block.y");
    private static final AttributeKey<Long> BLOCK_Z = AttributeKey.longKey("block.z");
    private static final AttributeKey<String> PLAYER_UUID = AttributeKey.stringKey("player.uuid");
    private static final AttributeKey<String> PLAYER_NAME = AttributeKey.stringKey("player.name");

    private final Logger logger;
    private final TelemetryManager telemetry;
    private final PluginConfig config;

    public BlockEventListener(Logger logger, TelemetryManager telemetry, PluginConfig config) {
        this.logger = logger;
        this.telemetry = telemetry;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(PlaceBlockEvent event) {
        if (!config.getMetrics().getBlockMetrics().isEnabled() ||
            !config.getMetrics().getBlockMetrics().isTrackPlacement()) {
            return;
        }

        try {
            var attributesBuilder = Attributes.builder();

            // Get block information
            var block = event.getBlock();
            if (block != null) {
                attributesBuilder.put(BLOCK_TYPE, block.getType().getIdentifier().toString());
            }

            // Get position
            var position = event.getPosition();
            if (position != null) {
                attributesBuilder.put(BLOCK_X, (long) position.getX());
                attributesBuilder.put(BLOCK_Y, (long) position.getY());
                attributesBuilder.put(BLOCK_Z, (long) position.getZ());
            }

            // Get world
            var world = event.getWorld();
            if (world != null) {
                attributesBuilder.put(WORLD_NAME, world.getName());
            }

            // Get player if available
            var player = event.getPlayer();
            if (player != null) {
                attributesBuilder.put(PLAYER_UUID, player.getUniqueId().toString());
                attributesBuilder.put(PLAYER_NAME, player.getName());
            }

            telemetry.recordBlockPlaced(attributesBuilder.build());
            logger.finest("Recorded block placed");
        } catch (Exception e) {
            logger.warning("Failed to record block place event: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BreakBlockEvent event) {
        if (!config.getMetrics().getBlockMetrics().isEnabled() ||
            !config.getMetrics().getBlockMetrics().isTrackBreaking()) {
            return;
        }

        try {
            var attributesBuilder = Attributes.builder();

            // Get block information
            var block = event.getBlock();
            if (block != null) {
                attributesBuilder.put(BLOCK_TYPE, block.getType().getIdentifier().toString());
            }

            // Get position
            var position = event.getPosition();
            if (position != null) {
                attributesBuilder.put(BLOCK_X, (long) position.getX());
                attributesBuilder.put(BLOCK_Y, (long) position.getY());
                attributesBuilder.put(BLOCK_Z, (long) position.getZ());
            }

            // Get world
            var world = event.getWorld();
            if (world != null) {
                attributesBuilder.put(WORLD_NAME, world.getName());
            }

            // Get player if available
            var player = event.getPlayer();
            if (player != null) {
                attributesBuilder.put(PLAYER_UUID, player.getUniqueId().toString());
                attributesBuilder.put(PLAYER_NAME, player.getName());
            }

            telemetry.recordBlockBroken(attributesBuilder.build());
            logger.finest("Recorded block broken");
        } catch (Exception e) {
            logger.warning("Failed to record block break event: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockUse(UseBlockEvent event) {
        if (!config.getMetrics().getBlockMetrics().isEnabled() ||
            !config.getMetrics().getBlockMetrics().isTrackInteractions()) {
            return;
        }

        try {
            var attributesBuilder = Attributes.builder();

            // Get block information
            var block = event.getBlock();
            if (block != null) {
                attributesBuilder.put(BLOCK_TYPE, block.getType().getIdentifier().toString());
            }

            // Get position
            var position = event.getPosition();
            if (position != null) {
                attributesBuilder.put(BLOCK_X, (long) position.getX());
                attributesBuilder.put(BLOCK_Y, (long) position.getY());
                attributesBuilder.put(BLOCK_Z, (long) position.getZ());
            }

            // Get world
            var world = event.getWorld();
            if (world != null) {
                attributesBuilder.put(WORLD_NAME, world.getName());
            }

            // Get player if available
            var player = event.getPlayer();
            if (player != null) {
                attributesBuilder.put(PLAYER_UUID, player.getUniqueId().toString());
                attributesBuilder.put(PLAYER_NAME, player.getName());
            }

            telemetry.recordBlockInteraction(attributesBuilder.build());
            logger.finest("Recorded block interaction");
        } catch (Exception e) {
            logger.warning("Failed to record block use event: " + e.getMessage());
        }
    }
}
