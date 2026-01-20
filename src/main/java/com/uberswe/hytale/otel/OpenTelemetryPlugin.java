package com.uberswe.hytale.otel;

import com.uberswe.hytale.otel.config.PluginConfig;
import com.uberswe.hytale.otel.listeners.BlockEventListener;
import com.uberswe.hytale.otel.listeners.PlayerEventListener;
import com.uberswe.hytale.otel.listeners.ServerEventListener;
import com.uberswe.hytale.otel.listeners.WorldEventListener;
import com.uberswe.hytale.otel.telemetry.TelemetryManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * OpenTelemetry Collector Plugin for Hytale servers.
 *
 * Collects metrics and traces from the server and exports them via OTLP
 * to an OpenTelemetry Collector or compatible backend (Grafana, Jaeger, etc.).
 *
 * Features:
 * - Player activity metrics (connections, disconnections, session tracking)
 * - Block interaction metrics (placement, breaking, usage)
 * - World events (loading, unloading)
 * - Server performance metrics (memory, uptime)
 * - Distributed tracing for player sessions
 */
public class OpenTelemetryPlugin extends JavaPlugin {
    private static final String CONFIG_FILE = "config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PluginConfig config;
    private TelemetryManager telemetryManager;
    private PlayerEventListener playerEventListener;
    private BlockEventListener blockEventListener;
    private WorldEventListener worldEventListener;
    private ServerEventListener serverEventListener;

    /**
     * Required constructor for Hytale plugins.
     * The server uses reflection to instantiate plugins with JavaPluginInit.
     */
    public OpenTelemetryPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected CompletableFuture<Void> preLoad() {
        return CompletableFuture.runAsync(() -> {
            try {
                loadConfiguration();
            } catch (Exception e) {
                getLogger().severe("Failed to load configuration: " + e.getMessage());
                throw new RuntimeException("Configuration loading failed", e);
            }
        });
    }

    @Override
    protected void setup() {
        Logger logger = getLogger();

        if (!config.isEnabled()) {
            logger.info("OpenTelemetry plugin is disabled in configuration");
            return;
        }

        logger.info("Setting up OpenTelemetry plugin...");

        // Initialize telemetry manager
        telemetryManager = new TelemetryManager(logger, config);
        telemetryManager.initialize();

        // Register event listeners
        registerEventListeners(logger);

        logger.info("OpenTelemetry plugin setup complete");
        logger.info("Exporting to: " + config.getOtlp().getEndpoint());
    }

    @Override
    protected void start() {
        if (!config.isEnabled()) {
            return;
        }

        getLogger().info("OpenTelemetry plugin started");
        getLogger().info("Service: " + config.getServiceNamespace() + "/" + config.getServiceName());
        getLogger().info("Metrics enabled: " + config.getMetrics().isEnabled());
        getLogger().info("Tracing enabled: " + config.getTracing().isEnabled());
    }

    @Override
    protected void shutdown() {
        getLogger().info("Shutting down OpenTelemetry plugin...");

        // Cleanup event listeners
        if (playerEventListener != null) {
            playerEventListener.shutdown();
        }

        // Shutdown telemetry manager (flushes pending data)
        if (telemetryManager != null) {
            telemetryManager.shutdown();
        }

        getLogger().info("OpenTelemetry plugin shutdown complete");
    }

    private void loadConfiguration() throws IOException {
        Path configPath = getDataDirectory().resolve(CONFIG_FILE);

        if (Files.exists(configPath)) {
            // Load existing configuration
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, PluginConfig.class);
                getLogger().info("Loaded configuration from " + configPath);
            }
        } else {
            // Create default configuration from resources
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (is != null) {
                    try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        config = GSON.fromJson(reader, PluginConfig.class);
                    }
                } else {
                    config = new PluginConfig();
                }
            }

            // Save default configuration
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(config), StandardCharsets.UTF_8);
            getLogger().info("Created default configuration at " + configPath);
        }

        // Override endpoint from environment variable if set
        String envEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (envEndpoint != null && !envEndpoint.isBlank()) {
            config.getOtlp().setEndpoint(envEndpoint);
            getLogger().info("Using OTLP endpoint from environment: " + envEndpoint);
        }

        // Override service name from environment variable if set
        String envServiceName = System.getenv("OTEL_SERVICE_NAME");
        if (envServiceName != null && !envServiceName.isBlank()) {
            config.setServiceName(envServiceName);
            getLogger().info("Using service name from environment: " + envServiceName);
        }
    }

    private void registerEventListeners(Logger logger) {
        var eventRegistry = getEventRegistry();

        // Player events
        if (config.getMetrics().getPlayerMetrics().isEnabled()) {
            playerEventListener = new PlayerEventListener(logger, telemetryManager, config);
            eventRegistry.register(this, playerEventListener);
            logger.info("Registered player event listener");
        }

        // Block events
        if (config.getMetrics().getBlockMetrics().isEnabled()) {
            blockEventListener = new BlockEventListener(logger, telemetryManager, config);
            eventRegistry.register(this, blockEventListener);
            logger.info("Registered block event listener");
        }

        // World events
        if (config.getMetrics().getWorldMetrics().isEnabled()) {
            worldEventListener = new WorldEventListener(logger, telemetryManager, config);
            eventRegistry.register(this, worldEventListener);
            logger.info("Registered world event listener");
        }

        // Server events
        if (config.getMetrics().getServerMetrics().isEnabled()) {
            serverEventListener = new ServerEventListener(logger, telemetryManager, config);
            eventRegistry.register(this, serverEventListener);
            logger.info("Registered server event listener");
        }
    }

    /**
     * Get the telemetry manager for external access.
     * Can be used by other plugins to add custom metrics.
     */
    public TelemetryManager getTelemetryManager() {
        return telemetryManager;
    }

    /**
     * Get the current configuration.
     */
    public PluginConfig getConfig() {
        return config;
    }
}
