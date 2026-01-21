package com.uberswe.hytale.otel;

import com.uberswe.hytale.otel.config.PluginConfig;
import com.uberswe.hytale.otel.ecs.BlockBreakEventSystem;
import com.uberswe.hytale.otel.ecs.BlockPlaceEventSystem;
import com.uberswe.hytale.otel.ecs.BlockUseEventSystem;
import com.uberswe.hytale.otel.telemetry.TelemetryManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.BootEvent;
import com.hypixel.hytale.server.core.event.events.ShutdownEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HOTEL C - Hytale OpenTelemetry Collector Plugin.
 *
 * Collects metrics and traces from the server and exports them via OTLP
 * to an OpenTelemetry Collector or compatible backend (Grafana, Jaeger, etc.).
 */
public class OpenTelemetryPlugin extends JavaPlugin {
    private static final String CONFIG_FILE = "config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final AttributeKey<String> PLAYER_UUID = AttributeKey.stringKey("player.uuid");
    private static final AttributeKey<String> PLAYER_NAME = AttributeKey.stringKey("player.name");
    private static final AttributeKey<String> WORLD_NAME = AttributeKey.stringKey("world.name");

    private PluginConfig config;
    private TelemetryManager telemetryManager;
    private HytaleLogger logger;

    // Track player sessions for tracing
    private final Map<UUID, Span> playerSessionSpans = new ConcurrentHashMap<>();

    // Server lifecycle tracking
    private Span serverLifecycleSpan;
    private long bootTime;

    public OpenTelemetryPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    public CompletableFuture<Void> preLoad() {
        return CompletableFuture.runAsync(() -> {
            try {
                loadConfiguration();
            } catch (Exception e) {
                throw new RuntimeException("Configuration loading failed", e);
            }
        });
    }

    @Override
    protected void setup() {
        // Get logger after plugin is initialized
        logger = getLogger();

        if (!config.isEnabled()) {
            logger.at(Level.INFO).log("HOTEL C plugin is disabled in configuration");
            return;
        }

        logger.at(Level.INFO).log("Setting up HOTEL C plugin...");

        // Initialize telemetry manager
        telemetryManager = new TelemetryManager(logger, config);
        telemetryManager.initialize();

        // Register event listeners
        registerEventListeners();

        logger.at(Level.INFO).log("HOTEL C plugin setup complete");
        logger.at(Level.INFO).log(String.format("Exporting to: %s", config.getOtlp().getEndpoint()));
    }

    @Override
    protected void start() {
        if (!config.isEnabled()) {
            return;
        }

        logger.at(Level.INFO).log("HOTEL C plugin started");
        logger.at(Level.INFO).log(String.format("Service: %s/%s", config.getServiceNamespace(), config.getServiceName()));
        logger.at(Level.INFO).log(String.format("Metrics enabled: %s", config.getMetrics().isEnabled()));
        logger.at(Level.INFO).log(String.format("Tracing enabled: %s", config.getTracing().isEnabled()));
    }

    @Override
    protected void shutdown() {
        if (logger != null) {
            logger.at(Level.INFO).log("Shutting down HOTEL C plugin...");
        }

        // End any active player session spans
        for (Span span : playerSessionSpans.values()) {
            span.setStatus(StatusCode.OK, "Server shutdown");
            span.end();
        }
        playerSessionSpans.clear();

        // End server lifecycle span
        if (serverLifecycleSpan != null) {
            serverLifecycleSpan.setStatus(StatusCode.OK);
            serverLifecycleSpan.end();
        }

        // Shutdown telemetry manager (flushes pending data)
        if (telemetryManager != null) {
            telemetryManager.shutdown();
        }

        if (logger != null) {
            logger.at(Level.INFO).log("HOTEL C plugin shutdown complete");
        }
    }

    private void loadConfiguration() throws IOException {
        Path configPath = getDataDirectory().resolve(CONFIG_FILE);

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, PluginConfig.class);
            }
        } else {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (is != null) {
                    try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        config = GSON.fromJson(reader, PluginConfig.class);
                    }
                } else {
                    config = new PluginConfig();
                }
            }

            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(config), StandardCharsets.UTF_8);
        }

        // Override from environment variables
        String envEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (envEndpoint != null && !envEndpoint.isBlank()) {
            config.getOtlp().setEndpoint(envEndpoint);
        }

        String envServiceName = System.getenv("OTEL_SERVICE_NAME");
        if (envServiceName != null && !envServiceName.isBlank()) {
            config.setServiceName(envServiceName);
        }
    }

    private void registerEventListeners() {
        var eventRegistry = getEventRegistry();

        // Server events
        if (config.getMetrics().getServerMetrics().isEnabled()) {
            eventRegistry.register(BootEvent.class, this::onServerBoot);
            eventRegistry.register(ShutdownEvent.class, this::onServerShutdown);
            logger.at(Level.INFO).log("Registered server event listeners");
        }

        // Player events
        if (config.getMetrics().getPlayerMetrics().isEnabled()) {
            eventRegistry.register(PlayerConnectEvent.class, this::onPlayerConnect);
            eventRegistry.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
            logger.at(Level.INFO).log("Registered player event listeners");
        }

        // World events - use registerGlobal for keyed events to listen across all worlds
        if (config.getMetrics().getWorldMetrics().isEnabled()) {
            eventRegistry.registerGlobal(AddWorldEvent.class, this::onWorldAdd);
            eventRegistry.registerGlobal(RemoveWorldEvent.class, this::onWorldRemove);
            logger.at(Level.INFO).log("Registered world event listeners");
        }

        // PlayerReadyEvent - fired when player is fully loaded and ready (keyed event)
        if (config.getMetrics().getPlayerMetrics().isEnabled()) {
            eventRegistry.registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
            logger.at(Level.INFO).log("Registered PlayerReadyEvent listener");
        }

        // Block events - ECS event systems registered with the entity store
        if (config.getMetrics().getBlockMetrics().isEnabled()) {
            var entityStoreRegistry = getEntityStoreRegistry();

            if (config.getMetrics().getBlockMetrics().isTrackPlacement()) {
                entityStoreRegistry.registerSystem(new BlockPlaceEventSystem(telemetryManager));
                logger.at(Level.INFO).log("Registered BlockPlaceEventSystem");
            }

            if (config.getMetrics().getBlockMetrics().isTrackBreaking()) {
                entityStoreRegistry.registerSystem(new BlockBreakEventSystem(telemetryManager));
                logger.at(Level.INFO).log("Registered BlockBreakEventSystem");
            }

            if (config.getMetrics().getBlockMetrics().isTrackInteractions()) {
                entityStoreRegistry.registerSystem(new BlockUseEventSystem(telemetryManager));
                logger.at(Level.INFO).log("Registered BlockUseEventSystem");
            }

            logger.at(Level.INFO).log("Registered block ECS event systems");
        }
    }

    // Server event handlers
    private void onServerBoot(BootEvent event) {
        bootTime = System.currentTimeMillis();
        try {
            if (config.getTracing().isEnabled()) {
                serverLifecycleSpan = telemetryManager.getTracer()
                        .spanBuilder("server.lifecycle")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan();
                serverLifecycleSpan.addEvent("server.started");
            }
            logger.at(Level.INFO).log("HOTEL C: Server boot event recorded");
        } catch (Exception e) {
            logger.at(Level.WARNING).withCause(e).log("Failed to record server boot event");
        }
    }

    private void onServerShutdown(ShutdownEvent event) {
        try {
            long uptime = System.currentTimeMillis() - bootTime;
            if (serverLifecycleSpan != null) {
                serverLifecycleSpan.addEvent("server.shutdown",
                        Attributes.of(AttributeKey.longKey("server.uptime_ms"), uptime));
            }
            logger.at(Level.INFO).log(String.format("HOTEL C: Server shutdown event recorded. Uptime: %ds", uptime / 1000));
        } catch (Exception e) {
            logger.at(Level.WARNING).withCause(e).log("Failed to record server shutdown event");
        }
    }

    // Player event handlers
    private void onPlayerConnect(PlayerConnectEvent event) {
        try {
            var player = event.getPlayer();
            var playerRef = event.getPlayerRef();
            var attributes = Attributes.builder()
                    .put(PLAYER_UUID, playerRef.getUuid().toString())
                    .put(PLAYER_NAME, playerRef.getUsername())
                    .build();

            telemetryManager.recordPlayerConnect(attributes);

            // Start session span if tracing is enabled
            if (config.getTracing().isEnabled() && config.getTracing().isTracePlayerSessions()) {
                Span sessionSpan = telemetryManager.getTracer()
                        .spanBuilder("player.session")
                        .setSpanKind(SpanKind.SERVER)
                        .setAttribute(PLAYER_UUID, playerRef.getUuid().toString())
                        .setAttribute(PLAYER_NAME, playerRef.getUsername())
                        .startSpan();
                playerSessionSpans.put(playerRef.getUuid(), sessionSpan);
            }

            logger.at(Level.FINE).log(String.format("Recorded player connect: %s", playerRef.getUsername()));
        } catch (Exception e) {
            logger.at(Level.WARNING).withCause(e).log("Failed to record player connect event");
        }
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        try {
            // PlayerDisconnectEvent uses PlayerRef, not Player
            PlayerRef playerRef = event.getPlayerRef();
            var attributes = Attributes.builder()
                    .put(PLAYER_UUID, playerRef.getUuid().toString())
                    .put(PLAYER_NAME, playerRef.getUsername())
                    .build();

            telemetryManager.recordPlayerDisconnect(attributes);

            // End session span
            Span sessionSpan = playerSessionSpans.remove(playerRef.getUuid());
            if (sessionSpan != null) {
                sessionSpan.setStatus(StatusCode.OK);
                sessionSpan.end();
            }

            logger.at(Level.FINE).log(String.format("Recorded player disconnect: %s", playerRef.getUsername()));
        } catch (Exception e) {
            logger.at(Level.WARNING).withCause(e).log("Failed to record player disconnect event");
        }
    }

    private void onPlayerReady(PlayerReadyEvent event) {
        try {
            var player = event.getPlayer();
            PlayerRef playerRef = player.getPlayerRef();
            // Add event to the player's session span if tracing
            Span sessionSpan = playerSessionSpans.get(playerRef.getUuid());
            if (sessionSpan != null) {
                sessionSpan.addEvent("player.ready");
            }
            logger.at(Level.FINE).log(String.format("Recorded player ready: %s", playerRef.getUsername()));
        } catch (Exception e) {
            logger.at(Level.WARNING).withCause(e).log("Failed to record player ready event");
        }
    }

    // World event handlers
    private void onWorldAdd(AddWorldEvent event) {
        try {
            var world = event.getWorld();
            var attributes = Attributes.builder()
                    .put(WORLD_NAME, world.getName())
                    .build();

            telemetryManager.recordWorldLoaded(attributes);

            // Add trace event if enabled
            if (config.getTracing().isEnabled() && serverLifecycleSpan != null) {
                serverLifecycleSpan.addEvent("world.loaded",
                        Attributes.of(WORLD_NAME, world.getName()));
            }

            logger.at(Level.FINE).log(String.format("Recorded world loaded: %s", world.getName()));
        } catch (Exception e) {
            logger.at(Level.WARNING).withCause(e).log("Failed to record world add event");
        }
    }

    private void onWorldRemove(RemoveWorldEvent event) {
        try {
            var world = event.getWorld();
            var attributes = Attributes.builder()
                    .put(WORLD_NAME, world.getName())
                    .build();

            telemetryManager.recordWorldUnloaded(attributes);

            // Add trace event if enabled
            if (config.getTracing().isEnabled() && serverLifecycleSpan != null) {
                serverLifecycleSpan.addEvent("world.unloaded",
                        Attributes.of(WORLD_NAME, world.getName()));
            }

            logger.at(Level.FINE).log(String.format("Recorded world unloaded: %s", world.getName()));
        } catch (Exception e) {
            logger.at(Level.WARNING).withCause(e).log("Failed to record world remove event");
        }
    }

    public TelemetryManager getTelemetryManager() {
        return telemetryManager;
    }

    public PluginConfig getPluginConfig() {
        return config;
    }
}
