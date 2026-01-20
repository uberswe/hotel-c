package com.uberswe.hytale.otel.listeners;

import com.uberswe.hytale.otel.config.PluginConfig;
import com.uberswe.hytale.otel.telemetry.TelemetryManager;
import com.hypixel.hytale.server.core.event.EventHandler;
import com.hypixel.hytale.server.core.event.EventListener;
import com.hypixel.hytale.server.core.event.EventPriority;
import com.hypixel.hytale.server.core.event.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.player.PlayerReadyEvent;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Listens to player events and records metrics/traces.
 */
public class PlayerEventListener implements EventListener {
    private static final AttributeKey<String> PLAYER_UUID = AttributeKey.stringKey("player.uuid");
    private static final AttributeKey<String> PLAYER_NAME = AttributeKey.stringKey("player.name");
    private static final AttributeKey<String> WORLD_NAME = AttributeKey.stringKey("world.name");

    private final Logger logger;
    private final TelemetryManager telemetry;
    private final PluginConfig config;

    // Track player sessions for tracing
    private final Map<UUID, Span> playerSessionSpans = new ConcurrentHashMap<>();

    public PlayerEventListener(Logger logger, TelemetryManager telemetry, PluginConfig config) {
        this.logger = logger;
        this.telemetry = telemetry;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerConnect(PlayerConnectEvent event) {
        if (!config.getMetrics().getPlayerMetrics().isEnabled()) {
            return;
        }

        try {
            var player = event.getPlayer();
            var attributes = Attributes.builder()
                    .put(PLAYER_UUID, player.getUniqueId().toString())
                    .put(PLAYER_NAME, player.getName())
                    .build();

            telemetry.recordPlayerConnect(attributes);

            // Start session span if tracing is enabled
            if (config.getTracing().isEnabled() && config.getTracing().isTracePlayerSessions()) {
                Span sessionSpan = telemetry.getTracer()
                        .spanBuilder("player.session")
                        .setSpanKind(SpanKind.SERVER)
                        .setAttribute(PLAYER_UUID, player.getUniqueId().toString())
                        .setAttribute(PLAYER_NAME, player.getName())
                        .startSpan();

                playerSessionSpans.put(player.getUniqueId(), sessionSpan);
            }

            logger.fine("Recorded player connect: " + player.getName());
        } catch (Exception e) {
            logger.warning("Failed to record player connect event: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (!config.getMetrics().getPlayerMetrics().isEnabled()) {
            return;
        }

        try {
            var player = event.getPlayer();
            var attributes = Attributes.builder()
                    .put(PLAYER_UUID, player.getUniqueId().toString())
                    .put(PLAYER_NAME, player.getName())
                    .build();

            telemetry.recordPlayerDisconnect(attributes);

            // End session span if tracing is enabled
            if (config.getTracing().isEnabled() && config.getTracing().isTracePlayerSessions()) {
                Span sessionSpan = playerSessionSpans.remove(player.getUniqueId());
                if (sessionSpan != null) {
                    sessionSpan.setStatus(StatusCode.OK);
                    sessionSpan.end();
                }
            }

            logger.fine("Recorded player disconnect: " + player.getName());
        } catch (Exception e) {
            logger.warning("Failed to record player disconnect event: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerReady(PlayerReadyEvent event) {
        // Player is fully loaded and ready to play
        try {
            var player = event.getPlayer();

            // Add event to session span if exists
            Span sessionSpan = playerSessionSpans.get(player.getUniqueId());
            if (sessionSpan != null) {
                sessionSpan.addEvent("player.ready", Attributes.builder()
                        .put(PLAYER_NAME, player.getName())
                        .build());
            }

            logger.fine("Player ready: " + player.getName());
        } catch (Exception e) {
            logger.warning("Failed to process player ready event: " + e.getMessage());
        }
    }

    /**
     * Cleanup any remaining spans on shutdown.
     */
    public void shutdown() {
        for (Span span : playerSessionSpans.values()) {
            span.setStatus(StatusCode.OK, "Server shutdown");
            span.end();
        }
        playerSessionSpans.clear();
    }
}
