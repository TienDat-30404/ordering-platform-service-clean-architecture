package com.example.demo.application.ports.output.monitoring;

public interface OrderMonitoringPort {
    void recordOrderCreation(String status);
}
