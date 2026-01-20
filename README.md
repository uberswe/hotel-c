<p align="center">
  <img src="https://img.shields.io/badge/HOTEL_C-Hytale_OpenTelemetry_Collector-blue?style=for-the-badge&logo=opentelemetry" alt="HOTEL C"/>
</p>

<h1 align="center">🏨 HOTEL C</h1>

<p align="center">
  <strong>H</strong>ytale <strong>O</strong>pen<strong>TEL</strong>emetry <strong>C</strong>ollector
</p>

<p align="center">
  <a href="https://github.com/uberswe/hotel-c/actions/workflows/build.yml">
    <img src="https://github.com/uberswe/hotel-c/actions/workflows/build.yml/badge.svg" alt="Build Status"/>
  </a>
  <a href="https://github.com/uberswe/hotel-c/releases/latest">
    <img src="https://img.shields.io/github/v/release/uberswe/hotel-c?include_prereleases&style=flat-square" alt="Latest Release"/>
  </a>
  <a href="https://github.com/uberswe/hotel-c/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/uberswe/hotel-c?style=flat-square" alt="License"/>
  </a>
  <a href="https://github.com/uberswe/hotel-c/releases">
    <img src="https://img.shields.io/github/downloads/uberswe/hotel-c/total?style=flat-square" alt="Downloads"/>
  </a>
</p>

<p align="center">
  <a href="#-features">Features</a> •
  <a href="#-installation">Installation</a> •
  <a href="#-configuration">Configuration</a> •
  <a href="#-grafana-dashboard">Dashboard</a> •
  <a href="#-contributing">Contributing</a>
</p>

---

A general-purpose OpenTelemetry collector plugin for Hytale servers. Collects metrics and traces from your Hytale server and exports them via OTLP to any compatible backend (Grafana, Jaeger, Prometheus, etc.).

## ✨ Features

### 📊 Metrics

| Category | Metric | Type | Description |
|----------|--------|------|-------------|
| **Players** | `hytale.players.count` | Gauge | Current number of online players |
| | `hytale.players.connections` | Counter | Total player connections |
| | `hytale.players.disconnections` | Counter | Total player disconnections |
| **Blocks** | `hytale.blocks.placed` | Counter | Total blocks placed |
| | `hytale.blocks.broken` | Counter | Total blocks broken |
| | `hytale.blocks.interactions` | Counter | Total block interactions |
| **Worlds** | `hytale.worlds.loaded` | Counter | Total worlds loaded |
| | `hytale.worlds.unloaded` | Counter | Total worlds unloaded |
| **Server** | `hytale.server.memory.used` | Gauge | JVM heap memory used |
| | `hytale.server.memory.max` | Gauge | JVM max heap memory |
| | `hytale.server.uptime` | Gauge | Server uptime in seconds |

### 🔍 Tracing

- Player session traces with events for connect, ready, and disconnect
- Server lifecycle traces
- Distributed tracing support

### 🏷️ Attributes

All metrics include relevant attributes:
- `player.uuid`, `player.name`
- `world.name`, `world.id`
- `block.type`, `block.x`, `block.y`, `block.z`

## 📋 Requirements

- ☕ Java 21
- 🐘 Gradle 9.2.0 (for building)
- 🎮 Hytale Server

## 📦 Installation

### From Releases (Recommended)

1. Download the latest `hotel-c-*.jar` from [Releases](https://github.com/uberswe/hotel-c/releases/latest)
2. Place the JAR in your Hytale server's `mods` directory
3. Start your server
4. Configure the OTLP endpoint in `data/mods/uberswe_HotelC/config.json`
5. Restart the server

### From Source

```bash
# Clone the repository
git clone https://github.com/uberswe/hotel-c.git
cd hotel-c

# Build with Gradle
./gradlew build

# Copy to your server
cp build/libs/hotel-c-*.jar /path/to/hytale/server/mods/
```

## ⚙️ Configuration

The plugin is configured via `config.json`:

<details>
<summary>Click to expand full configuration</summary>

```json
{
  "enabled": true,
  "serviceName": "hytale-server",
  "serviceNamespace": "hytale",
  "serviceVersion": "1.0.0",
  "otlp": {
    "endpoint": "http://opentelemetry-collector.monitoring.svc.cluster.local:4317",
    "protocol": "grpc",
    "headers": {},
    "timeout": 10000,
    "compression": "gzip"
  },
  "metrics": {
    "enabled": true,
    "exportIntervalMs": 30000,
    "playerMetrics": {
      "enabled": true,
      "trackConnections": true,
      "trackSessions": true
    },
    "blockMetrics": {
      "enabled": true,
      "trackPlacement": true,
      "trackBreaking": true,
      "trackInteractions": true
    },
    "worldMetrics": {
      "enabled": true,
      "trackWorldLoading": true,
      "trackChunkLoading": false
    },
    "serverMetrics": {
      "enabled": true,
      "trackTps": true,
      "trackMemory": true,
      "trackUptime": true
    }
  },
  "tracing": {
    "enabled": true,
    "sampleRate": 1.0,
    "tracePlayerSessions": true,
    "traceBlockOperations": false
  },
  "attributes": {
    "environment": "production",
    "cluster": "default"
  }
}
```

</details>

### 🔧 Environment Variables

| Variable | Description |
|----------|-------------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Override the OTLP endpoint |
| `OTEL_SERVICE_NAME` | Override the service name |

## ☸️ Kubernetes Deployment

Add environment variables to your pod spec:

```yaml
env:
  - name: OTEL_EXPORTER_OTLP_ENDPOINT
    value: "http://opentelemetry-collector.monitoring.svc.cluster.local:4317"
  - name: OTEL_SERVICE_NAME
    value: "hytale-server-prod"
```

## 📈 Grafana Dashboard

HOTEL C includes a pre-built Grafana dashboard for visualizing your Hytale server metrics.

### Import the Dashboard

1. Download [`dashboards/hotel-c-dashboard.json`](dashboards/hotel-c-dashboard.json)
2. In Grafana, go to **Dashboards** → **Import**
3. Upload the JSON file or paste its contents
4. Select your Prometheus data source
5. Click **Import**

### Dashboard Preview

The dashboard includes:

| Section | Panels |
|---------|--------|
| 🎮 **Player Activity** | Players Online, Connections, Disconnections, Player Count Over Time |
| 🧱 **Block Activity** | Blocks Placed, Broken, Interactions, Activity Rate |
| 🌍 **World Activity** | Worlds Loaded, Unloaded, Events Over Time |
| 🖥️ **Server Health** | Memory Usage Gauge, Uptime, Memory Over Time |

### Example Prometheus Queries

```promql
# Current player count
hytale_players_count

# Player connections rate (per minute)
rate(hytale_players_connections_total[5m]) * 60

# Blocks placed per minute by type
sum(rate(hytale_blocks_placed_total[5m])) by (block_type) * 60

# Memory usage percentage
hytale_server_memory_used / hytale_server_memory_max * 100

# Server uptime in hours
hytale_server_uptime / 3600
```

## 🏗️ Project Structure

```
hotel-c/
├── 📁 .github/workflows/     # CI/CD workflows
├── 📁 dashboards/            # Grafana dashboards
├── 📁 config/                # Sample configurations
├── 📁 src/main/java/com/uberswe/hytale/otel/
│   ├── 📄 OpenTelemetryPlugin.java     # Main plugin class with event handlers
│   ├── 📁 config/
│   │   └── 📄 PluginConfig.java        # Configuration classes
│   └── 📁 telemetry/
│       └── 📄 TelemetryManager.java    # OpenTelemetry SDK setup
└── 📁 src/main/resources/
    ├── 📄 manifest.json                 # Plugin manifest
    └── 📄 config.json                   # Default configuration
```

## 🔌 API for Other Plugins

Other plugins can access the TelemetryManager to add custom metrics:

```java
// Get the HOTEL C plugin instance
OpenTelemetryPlugin otelPlugin = /* get plugin instance */;
TelemetryManager telemetry = otelPlugin.getTelemetryManager();

// Create custom metrics
Meter meter = telemetry.getMeter();
LongCounter customCounter = meter.counterBuilder("myplugin.custom.counter")
    .setDescription("My custom counter")
    .build();

customCounter.add(1, Attributes.of(AttributeKey.stringKey("custom.attribute"), "value"));
```

## 🔧 Troubleshooting

<details>
<summary><strong>Metrics not appearing</strong></summary>

1. Check the plugin logs for errors
2. Verify the OTLP endpoint is reachable from the server
3. Ensure the OpenTelemetry Collector is configured to accept OTLP

</details>

<details>
<summary><strong>High memory usage</strong></summary>

- Increase `exportIntervalMs` to reduce export frequency
- Disable unused metric categories
- Set `traceBlockOperations: false` if not needed

</details>

<details>
<summary><strong>Connection issues</strong></summary>

- Check firewall rules for port 4317 (gRPC) or 4318 (HTTP)
- Verify TLS configuration if using HTTPS
- Check collector logs for connection errors

</details>

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.

Copyright (c) 2026 uberswe (admin@uberswe.com)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

<p align="center">
  Made with ❤️ for the Hytale community
</p>
