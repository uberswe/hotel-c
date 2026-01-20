plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.uberswe.hytale"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

repositories {
    mavenCentral()
    // Hytale server API - use local maven repo or file dependency
    // In production, this would be published to a repository
    flatDir {
        dirs("libs")
    }
}

val otelVersion = "1.44.1"
val otelInstrumentationVersion = "2.10.0"

dependencies {
    // Hytale Server API - provided at runtime by the server
    compileOnly(files("libs/hytale-server-api.jar"))

    // OpenTelemetry API
    implementation("io.opentelemetry:opentelemetry-api:$otelVersion")
    implementation("io.opentelemetry:opentelemetry-sdk:$otelVersion")
    implementation("io.opentelemetry:opentelemetry-sdk-metrics:$otelVersion")
    implementation("io.opentelemetry:opentelemetry-sdk-trace:$otelVersion")

    // OTLP Exporters
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:$otelVersion")

    // OpenTelemetry Semantic Conventions
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.28.0-alpha")

    // Logging (use server's logger, but need SLF4J for OTEL)
    implementation("org.slf4j:slf4j-simple:2.0.16")

    // Configuration parsing
    implementation("com.google.code.gson:gson:2.11.0")

    // Annotations
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("--enable-preview")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

tasks.shadowJar {
    archiveClassifier.set("")

    // Relocate OpenTelemetry to avoid conflicts with other plugins
    relocate("io.opentelemetry", "com.uberswe.hytale.otel.shaded.otel")
    relocate("io.grpc", "com.uberswe.hytale.otel.shaded.grpc")
    relocate("com.google.gson", "com.uberswe.hytale.otel.shaded.gson")
    relocate("org.slf4j", "com.uberswe.hytale.otel.shaded.slf4j")

    // Minimize jar size
    minimize {
        exclude(dependency("io.opentelemetry:.*"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
