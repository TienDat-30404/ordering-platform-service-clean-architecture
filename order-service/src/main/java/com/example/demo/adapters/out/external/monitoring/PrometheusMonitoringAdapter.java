package com.example.demo.adapters.out.external.monitoring;


import org.springframework.stereotype.Service;

import com.example.demo.application.ports.output.monitoring.OrderMonitoringPort;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;


@Service
public class PrometheusMonitoringAdapter implements OrderMonitoringPort {

    private final MeterRegistry registry;

    public PrometheusMonitoringAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordOrderCreation(String status) {
        Counter counter = Counter
            .builder("orders_total")
            .description("Total number of orders processed, labeled by status.")
            .tags(Tags.of(
                "job", "order-service",
                "status", status.toUpperCase() 
            ))
            .register(this.registry);
            
        counter.increment();
    }
}