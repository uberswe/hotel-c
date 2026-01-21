package com.uberswe.hytale.otel.telemetry;

import com.uberswe.hytale.otel.config.PluginConfig;
import com.hypixel.hytale.logger.HytaleLogger;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages OpenTelemetry SDK initialization and provides access to metrics and tracing.
 */
public class TelemetryManager {
    private static final String INSTRUMENTATION_SCOPE = "hotel-c";

    private final HytaleLogger logger;
    private final PluginConfig config;

    private OpenTelemetrySdk openTelemetry;
    private Meter meter;
    private Tracer tracer;

    // Player metrics
    private LongUpDownCounter playerCount;
    private LongCounter playerConnections;
    private LongCounter playerDisconnections;

    // Block metrics
    private LongCounter blocksPlaced;
    private LongCounter blocksBroken;
    private LongCounter blockInteractions;

    // World metrics
    private LongCounter worldsLoaded;
    private LongCounter worldsUnloaded;

    // Server metrics
    private ObservableLongGauge memoryUsed;
    private ObservableLongGauge memoryMax;
    private ObservableLongGauge uptime;

    private final long startTime;
    private volatile int currentPlayerCount = 0;

    public TelemetryManager(HytaleLogger logger, PluginConfig config) {
        this.logger = logger;
        this.config = config;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Initialize the OpenTelemetry SDK with configured exporters.
     */
    public void initialize() {
        logger.at(Level.INFO).log(String.format("Initializing OpenTelemetry with endpoint: %s", config.getOtlp().getEndpoint()));

        // Build resource with service information
        Resource resource = Resource.getDefault().toBuilder()
                .put(AttributeKey.stringKey("service.name"), config.getServiceName())
                .put(AttributeKey.stringKey("service.namespace"), config.getServiceNamespace())
                .put(AttributeKey.stringKey("service.version"), config.getServiceVersion())
                .putAll(buildCustomAttributes())
                .build();

        // Build OpenTelemetry SDK
        var builder = OpenTelemetrySdk.builder();

        // Configure metrics if enabled
        if (config.getMetrics().isEnabled()) {
            SdkMeterProvider meterProvider = buildMeterProvider(resource);
            builder.setMeterProvider(meterProvider);
        }

        // Configure tracing if enabled
        if (config.getTracing().isEnabled()) {
            SdkTracerProvider tracerProvider = buildTracerProvider(resource);
            builder.setTracerProvider(tracerProvider);
        }

        openTelemetry = builder.buildAndRegisterGlobal();

        // Get meter and tracer
        meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);
        tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE);

        // Initialize metrics instruments
        initializeMetrics();

        logger.at(Level.INFO).log("OpenTelemetry initialized successfully");
    }

    private Attributes buildCustomAttributes() {
        var builder = Attributes.builder();
        for (var entry : config.getAttributes().entrySet()) {
            builder.put(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    private SdkMeterProvider buildMeterProvider(Resource resource) {
        MetricExporter metricExporter;

        if ("http".equalsIgnoreCase(config.getOtlp().getProtocol())) {
            // Use HTTP/protobuf exporter
            var httpBuilder = OtlpHttpMetricExporter.builder()
                    .setEndpoint(config.getOtlp().getEndpoint() + "/v1/metrics")
                    .setTimeout(config.getOtlp().getTimeout(), TimeUnit.MILLISECONDS);

            for (var header : config.getOtlp().getHeaders().entrySet()) {
                httpBuilder.addHeader(header.getKey(), header.getValue());
            }

            if ("gzip".equalsIgnoreCase(config.getOtlp().getCompression())) {
                httpBuilder.setCompression("gzip");
            }

            metricExporter = httpBuilder.build();
        } else {
            // Use gRPC exporter (default)
            var grpcBuilder = OtlpGrpcMetricExporter.builder()
                    .setEndpoint(config.getOtlp().getEndpoint())
                    .setTimeout(config.getOtlp().getTimeout(), TimeUnit.MILLISECONDS);

            for (var header : config.getOtlp().getHeaders().entrySet()) {
                grpcBuilder.addHeader(header.getKey(), header.getValue());
            }

            if ("gzip".equalsIgnoreCase(config.getOtlp().getCompression())) {
                grpcBuilder.setCompression("gzip");
            }

            metricExporter = grpcBuilder.build();
        }

        PeriodicMetricReader metricReader = PeriodicMetricReader.builder(metricExporter)
                .setInterval(Duration.ofMillis(config.getMetrics().getExportIntervalMs()))
                .build();

        return SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(metricReader)
                .build();
    }

    private SdkTracerProvider buildTracerProvider(Resource resource) {
        io.opentelemetry.sdk.trace.export.SpanExporter spanExporter;

        if ("http".equalsIgnoreCase(config.getOtlp().getProtocol())) {
            // Use HTTP/protobuf exporter
            var httpBuilder = OtlpHttpSpanExporter.builder()
                    .setEndpoint(config.getOtlp().getEndpoint() + "/v1/traces")
                    .setTimeout(config.getOtlp().getTimeout(), TimeUnit.MILLISECONDS);

            for (var header : config.getOtlp().getHeaders().entrySet()) {
                httpBuilder.addHeader(header.getKey(), header.getValue());
            }

            if ("gzip".equalsIgnoreCase(config.getOtlp().getCompression())) {
                httpBuilder.setCompression("gzip");
            }

            spanExporter = httpBuilder.build();
        } else {
            // Use gRPC exporter (default)
            var grpcBuilder = OtlpGrpcSpanExporter.builder()
                    .setEndpoint(config.getOtlp().getEndpoint())
                    .setTimeout(config.getOtlp().getTimeout(), TimeUnit.MILLISECONDS);

            for (var header : config.getOtlp().getHeaders().entrySet()) {
                grpcBuilder.addHeader(header.getKey(), header.getValue());
            }

            if ("gzip".equalsIgnoreCase(config.getOtlp().getCompression())) {
                grpcBuilder.setCompression("gzip");
            }

            spanExporter = grpcBuilder.build();
        }

        // Configure sampler based on sample rate
        Sampler sampler = config.getTracing().getSampleRate() >= 1.0
                ? Sampler.alwaysOn()
                : Sampler.traceIdRatioBased(config.getTracing().getSampleRate());

        return SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setSampler(sampler)
                .build();
    }

    private void initializeMetrics() {
        var metricsConfig = config.getMetrics();

        // Player metrics
        if (metricsConfig.getPlayerMetrics().isEnabled()) {
            playerCount = meter.upDownCounterBuilder("hytale.players.count")
                    .setDescription("Current number of online players")
                    .setUnit("{players}")
                    .build();

            if (metricsConfig.getPlayerMetrics().isTrackConnections()) {
                playerConnections = meter.counterBuilder("hytale.players.connections")
                        .setDescription("Total number of player connections")
                        .setUnit("{connections}")
                        .build();

                playerDisconnections = meter.counterBuilder("hytale.players.disconnections")
                        .setDescription("Total number of player disconnections")
                        .setUnit("{disconnections}")
                        .build();
            }
        }

        // Block metrics
        if (metricsConfig.getBlockMetrics().isEnabled()) {
            if (metricsConfig.getBlockMetrics().isTrackPlacement()) {
                blocksPlaced = meter.counterBuilder("hytale.blocks.placed")
                        .setDescription("Total number of blocks placed")
                        .setUnit("{blocks}")
                        .build();
            }

            if (metricsConfig.getBlockMetrics().isTrackBreaking()) {
                blocksBroken = meter.counterBuilder("hytale.blocks.broken")
                        .setDescription("Total number of blocks broken")
                        .setUnit("{blocks}")
                        .build();
            }

            if (metricsConfig.getBlockMetrics().isTrackInteractions()) {
                blockInteractions = meter.counterBuilder("hytale.blocks.interactions")
                        .setDescription("Total number of block interactions")
                        .setUnit("{interactions}")
                        .build();
            }
        }

        // World metrics
        if (metricsConfig.getWorldMetrics().isEnabled()) {
            if (metricsConfig.getWorldMetrics().isTrackWorldLoading()) {
                worldsLoaded = meter.counterBuilder("hytale.worlds.loaded")
                        .setDescription("Total number of worlds loaded")
                        .setUnit("{worlds}")
                        .build();

                worldsUnloaded = meter.counterBuilder("hytale.worlds.unloaded")
                        .setDescription("Total number of worlds unloaded")
                        .setUnit("{worlds}")
                        .build();
            }
        }

        // Server metrics
        if (metricsConfig.getServerMetrics().isEnabled()) {
            if (metricsConfig.getServerMetrics().isTrackMemory()) {
                memoryUsed = meter.gaugeBuilder("hytale.server.memory.used")
                        .setDescription("JVM heap memory used")
                        .setUnit("By")
                        .ofLongs()
                        .buildWithCallback(measurement -> {
                            Runtime runtime = Runtime.getRuntime();
                            measurement.record(runtime.totalMemory() - runtime.freeMemory());
                        });

                memoryMax = meter.gaugeBuilder("hytale.server.memory.max")
                        .setDescription("JVM max heap memory")
                        .setUnit("By")
                        .ofLongs()
                        .buildWithCallback(measurement -> {
                            measurement.record(Runtime.getRuntime().maxMemory());
                        });
            }

            if (metricsConfig.getServerMetrics().isTrackUptime()) {
                uptime = meter.gaugeBuilder("hytale.server.uptime")
                        .setDescription("Server uptime in seconds")
                        .setUnit("s")
                        .ofLongs()
                        .buildWithCallback(measurement -> {
                            measurement.record((System.currentTimeMillis() - startTime) / 1000);
                        });
            }
        }
    }

    /**
     * Shutdown the OpenTelemetry SDK gracefully.
     */
    public void shutdown() {
        if (openTelemetry != null) {
            logger.at(Level.INFO).log("Shutting down OpenTelemetry...");
            openTelemetry.close();
            logger.at(Level.INFO).log("OpenTelemetry shutdown complete");
        }
    }

    // Metrics recording methods

    public void recordPlayerConnect(Attributes attributes) {
        if (playerCount != null) {
            currentPlayerCount++;
            playerCount.add(1, attributes);
        }
        if (playerConnections != null) {
            playerConnections.add(1, attributes);
        }
    }

    public void recordPlayerDisconnect(Attributes attributes) {
        if (playerCount != null) {
            currentPlayerCount--;
            playerCount.add(-1, attributes);
        }
        if (playerDisconnections != null) {
            playerDisconnections.add(1, attributes);
        }
    }

    public void recordBlockPlaced(Attributes attributes) {
        if (blocksPlaced != null) {
            blocksPlaced.add(1, attributes);
        }
    }

    public void recordBlockBroken(Attributes attributes) {
        if (blocksBroken != null) {
            blocksBroken.add(1, attributes);
        }
    }

    public void recordBlockInteraction(Attributes attributes) {
        if (blockInteractions != null) {
            blockInteractions.add(1, attributes);
        }
    }

    public void recordWorldLoaded(Attributes attributes) {
        if (worldsLoaded != null) {
            worldsLoaded.add(1, attributes);
        }
    }

    public void recordWorldUnloaded(Attributes attributes) {
        if (worldsUnloaded != null) {
            worldsUnloaded.add(1, attributes);
        }
    }

    public Tracer getTracer() {
        return tracer;
    }

    public Meter getMeter() {
        return meter;
    }

    public OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }

    public int getCurrentPlayerCount() {
        return currentPlayerCount;
    }
}
