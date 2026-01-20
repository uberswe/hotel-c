package com.uberswe.hytale.otel.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the OpenTelemetry plugin.
 * Loaded from config.json in the plugin's data directory.
 */
public class PluginConfig {
    private boolean enabled = true;
    private String serviceName = "hytale-server";
    private String serviceNamespace = "hytale";
    private String serviceVersion = "1.0.0";
    private OtlpConfig otlp = new OtlpConfig();
    private MetricsConfig metrics = new MetricsConfig();
    private TracingConfig tracing = new TracingConfig();
    private Map<String, String> attributes = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceNamespace() {
        return serviceNamespace;
    }

    public void setServiceNamespace(String serviceNamespace) {
        this.serviceNamespace = serviceNamespace;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public OtlpConfig getOtlp() {
        return otlp;
    }

    public void setOtlp(OtlpConfig otlp) {
        this.otlp = otlp;
    }

    public MetricsConfig getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsConfig metrics) {
        this.metrics = metrics;
    }

    public TracingConfig getTracing() {
        return tracing;
    }

    public void setTracing(TracingConfig tracing) {
        this.tracing = tracing;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * OTLP exporter configuration
     */
    public static class OtlpConfig {
        private String endpoint = "http://localhost:4317";
        private String protocol = "grpc"; // grpc or http
        private Map<String, String> headers = new HashMap<>();
        private int timeout = 10000;
        private String compression = "gzip";

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public String getCompression() {
            return compression;
        }

        public void setCompression(String compression) {
            this.compression = compression;
        }
    }

    /**
     * Metrics collection configuration
     */
    public static class MetricsConfig {
        private boolean enabled = true;
        private long exportIntervalMs = 30000;
        private PlayerMetricsConfig playerMetrics = new PlayerMetricsConfig();
        private BlockMetricsConfig blockMetrics = new BlockMetricsConfig();
        private WorldMetricsConfig worldMetrics = new WorldMetricsConfig();
        private ServerMetricsConfig serverMetrics = new ServerMetricsConfig();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getExportIntervalMs() {
            return exportIntervalMs;
        }

        public void setExportIntervalMs(long exportIntervalMs) {
            this.exportIntervalMs = exportIntervalMs;
        }

        public PlayerMetricsConfig getPlayerMetrics() {
            return playerMetrics;
        }

        public void setPlayerMetrics(PlayerMetricsConfig playerMetrics) {
            this.playerMetrics = playerMetrics;
        }

        public BlockMetricsConfig getBlockMetrics() {
            return blockMetrics;
        }

        public void setBlockMetrics(BlockMetricsConfig blockMetrics) {
            this.blockMetrics = blockMetrics;
        }

        public WorldMetricsConfig getWorldMetrics() {
            return worldMetrics;
        }

        public void setWorldMetrics(WorldMetricsConfig worldMetrics) {
            this.worldMetrics = worldMetrics;
        }

        public ServerMetricsConfig getServerMetrics() {
            return serverMetrics;
        }

        public void setServerMetrics(ServerMetricsConfig serverMetrics) {
            this.serverMetrics = serverMetrics;
        }
    }

    public static class PlayerMetricsConfig {
        private boolean enabled = true;
        private boolean trackConnections = true;
        private boolean trackSessions = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isTrackConnections() {
            return trackConnections;
        }

        public void setTrackConnections(boolean trackConnections) {
            this.trackConnections = trackConnections;
        }

        public boolean isTrackSessions() {
            return trackSessions;
        }

        public void setTrackSessions(boolean trackSessions) {
            this.trackSessions = trackSessions;
        }
    }

    public static class BlockMetricsConfig {
        private boolean enabled = true;
        private boolean trackPlacement = true;
        private boolean trackBreaking = true;
        private boolean trackInteractions = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isTrackPlacement() {
            return trackPlacement;
        }

        public void setTrackPlacement(boolean trackPlacement) {
            this.trackPlacement = trackPlacement;
        }

        public boolean isTrackBreaking() {
            return trackBreaking;
        }

        public void setTrackBreaking(boolean trackBreaking) {
            this.trackBreaking = trackBreaking;
        }

        public boolean isTrackInteractions() {
            return trackInteractions;
        }

        public void setTrackInteractions(boolean trackInteractions) {
            this.trackInteractions = trackInteractions;
        }
    }

    public static class WorldMetricsConfig {
        private boolean enabled = true;
        private boolean trackWorldLoading = true;
        private boolean trackChunkLoading = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isTrackWorldLoading() {
            return trackWorldLoading;
        }

        public void setTrackWorldLoading(boolean trackWorldLoading) {
            this.trackWorldLoading = trackWorldLoading;
        }

        public boolean isTrackChunkLoading() {
            return trackChunkLoading;
        }

        public void setTrackChunkLoading(boolean trackChunkLoading) {
            this.trackChunkLoading = trackChunkLoading;
        }
    }

    public static class ServerMetricsConfig {
        private boolean enabled = true;
        private boolean trackTps = true;
        private boolean trackMemory = true;
        private boolean trackUptime = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isTrackTps() {
            return trackTps;
        }

        public void setTrackTps(boolean trackTps) {
            this.trackTps = trackTps;
        }

        public boolean isTrackMemory() {
            return trackMemory;
        }

        public void setTrackMemory(boolean trackMemory) {
            this.trackMemory = trackMemory;
        }

        public boolean isTrackUptime() {
            return trackUptime;
        }

        public void setTrackUptime(boolean trackUptime) {
            this.trackUptime = trackUptime;
        }
    }

    /**
     * Tracing configuration
     */
    public static class TracingConfig {
        private boolean enabled = true;
        private double sampleRate = 1.0;
        private boolean tracePlayerSessions = true;
        private boolean traceBlockOperations = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(double sampleRate) {
            this.sampleRate = sampleRate;
        }

        public boolean isTracePlayerSessions() {
            return tracePlayerSessions;
        }

        public void setTracePlayerSessions(boolean tracePlayerSessions) {
            this.tracePlayerSessions = tracePlayerSessions;
        }

        public boolean isTraceBlockOperations() {
            return traceBlockOperations;
        }

        public void setTraceBlockOperations(boolean traceBlockOperations) {
            this.traceBlockOperations = traceBlockOperations;
        }
    }
}
