package com.example.demo.application.ports.output.OrderPublisher;

import java.util.Map;

public interface OrderEventPublisher {
    void publish(String topic, String key, String jsonPayload, Map<String, String> headers);
}