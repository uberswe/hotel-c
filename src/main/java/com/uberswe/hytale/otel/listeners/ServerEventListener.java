package com.uberswe.hytale.otel.listeners;

import com.uberswe.hytale.otel.config.PluginConfig;
import com.uberswe.hytale.otel.telemetry.TelemetryManager;
import com.hypixel.hytale.server.core.event.EventHandler;
import com.hypixel.hytale.server.core.event.EventListener;
import com.hypixel.hytale.server.core.event.EventPriority;
import com.hypixel.hytale.server.core.event.server.BootEvent;
import com.hypixel.hytale.server.core.event.server.ShutdownEvent;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;

import java.util.logging.Logger;

/**
 * Listens to server lifecycle events and records metrics/traces.
 */
public class ServerEventListener implements EventListener {
    private static final AttributeKey<String> EVENT_TYPE = AttributeKey.stringKey("event.type");
    private static final AttributeKey<Long> SERVER_UPTIME = AttributeKey.longKey("server.uptime_ms");

    private final Logger logger;
    private final TelemetryManager telemetry;
    private final PluginConfig config;

    private Span serverLifecycleSpan;
    private long bootTime;

    public ServerEventListener(Logger logger, TelemetryManager telemetry, PluginConfig config) {
        this.logger = logger;
        this.telemetry = telemetry;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerBoot(BootEvent event) {
        bootTime = System.currentTimeMillis();

        try {
            if (config.getTracing().isEnabled()) {
                serverLifecycleSpan = telemetry.getTracer()
                        .spanBuilder("server.lifecycle")
                        .setSpanKind(SpanKind.SERVER)
                        .setAttribute(EVENT_TYPE, "boot")
                        .startSpan();

                serverLifecycleSpan.addEvent("server.started", Attributes.builder()
                        .put("boot.time_ms", bootTime)
                        .build());
            }

            logger.info("OpenTelemetry: Server boot event recorded");
        } catch (Exception e) {
            logger.warning("Failed to record server boot event: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerShutdown(ShutdownEvent event) {
        try {
            long shutdownTime = System.currentTimeMillis();
            long uptime = shutdownTime - bootTime;

            if (serverLifecycleSpan != null) {
                serverLifecycleSpan.addEvent("server.shutdown", Attributes.builder()
                        .put("shutdown.time_ms", shutdownTime)
                        .put(SERVER_UPTIME, uptime)
                        .build());

                serverLifecycleSpan.setStatus(StatusCode.OK);
                serverLifecycleSpan.end();
            }

            logger.info("OpenTelemetry: Server shutdown event recorded. Uptime: " + (uptime / 1000) + "s");
        } catch (Exception e) {
            logger.warning("Failed to record server shutdown event: " + e.getMessage());
        }
    }
}
