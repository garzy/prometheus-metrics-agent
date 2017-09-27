package com.fleury.metrics.agent.reporter;

import static java.util.logging.Level.WARNING;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.ClassLoadingExports;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;
import io.prometheus.client.hotspot.ThreadExports;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The static methods in this class are called from the bytecode we instrument. Hence do not change any static methods
 * here related and/or method signatures unless you change the corresponding bytecode.
 *
 * @author Will Fleury
 */
public class PrometheusMetricSystem {

    private static final Logger LOGGER = Logger.getLogger(PrometheusMetricSystem.class.getName());

    private static final int DEFAULT_HTTP_PORT = 9899;
    
    private final Map<String, Object> configuration;

    protected PrometheusMetricSystem(Map<String, Object> configuration) {
        this.configuration = configuration;

        new StandardExports().register();

        addJVMMetrics(configuration);
    }

    public static Counter createAndRegisterCounted(String name, String[] labels, String doc) {
        Counter.Builder builder = Counter.build().name(name).help(doc);
        if (labels != null) {
            builder.labelNames(labels);
        }

        return builder.register();
    }

    public static Counter createAndRegisterExceptionCounted(String name, String[] labels, String doc) {
        Counter.Builder builder = Counter.build().name(name).help(doc);
        if (labels != null) {
            builder.labelNames(labels);
        }

        return builder.register();
    }

    public static Gauge createAndRegisterGauged(String name, String[] labels, String doc) {
        Gauge.Builder builder = Gauge.build().name(name).help(doc);
        if (labels != null) {
            builder.labelNames(labels);
        }

        return builder.register();
    }

    public static Histogram createAndRegisterTimed(String name, String[] labels, String doc) {
        Histogram.Builder builder = Histogram.build().name(name).help(doc);
        if (labels != null) {
            builder.labelNames(labels);
        }

        return builder.register();
    }

    public static void recordCount(Counter counter, String name, String[] labels) {
        if (labels != null) {
            counter.labels(labels).inc();
        } else {
            counter.inc();
        }
    }

    public static void recordCount(Counter counter, String name, String[] labels, long n) {
        if (labels != null) {
            counter.labels(labels).inc(n);
        } else {
            counter.inc(n);
        }
    }

    public static void recordGaugeInc(Gauge gauge, String name, String[] labelValues) {
        if (labelValues != null) {
            gauge.labels(labelValues).inc();
        } else {
            gauge.inc();
        }
    }

    public static void recordGaugeDec(Gauge gauge, String name, String[] labelValues) {
        if (labelValues != null) {
            gauge.labels(labelValues).dec();
        } else {
            gauge.dec();
        }
    }

    public static void recordTime(Histogram histogram, String name, String[] labels, long duration) {
        if (labels != null) {
            histogram.labels(labels).observe(duration);
        } else {
            histogram.observe(duration);
        }
    }

    public void startDefaultEndpoint() {
        int port = DEFAULT_HTTP_PORT;

        if (configuration.containsKey("httpPort")) {
            port = Integer.parseInt((String)configuration.get("httpPort"));
        }

        try {
            LOGGER.fine("Starting Prometheus HttpServer on port " + port);

            new HTTPServer(port);

        } catch (Exception e) { //widen scope in case of ClassNotFoundException on non oracle/sun JVM
            LOGGER.log(WARNING, "Unable to register Prometheus HttpServer on port " + port, e);
        }
    }

    private void addJVMMetrics(Map<String, Object> configuration) {
        if (!configuration.containsKey("jvm")) {
            return;
        }
        Set<String> jvmMetrics = new HashSet<String>((List<String>)configuration.get("jvm"));
        if (jvmMetrics.contains("gc")) {
            new GarbageCollectorExports().register();
        }

        if (jvmMetrics.contains("threads")) {
            new ThreadExports().register();
        }

        if (jvmMetrics.contains("memory")) {
            new MemoryPoolsExports().register();
        }

        if (jvmMetrics.contains("classloader")) {
            new ClassLoadingExports().register();
        }
    }

    void reset() {
        CollectorRegistry.defaultRegistry.clear();
    }
}