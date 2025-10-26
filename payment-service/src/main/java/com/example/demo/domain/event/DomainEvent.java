package com.example.demo.domain.event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class DomainEvent implements Serializable {
    private String eventId;
    private String aggregateId;
    private LocalDateTime timestamp;
    private String eventType;

    public DomainEvent(String aggregateId, String eventType) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
    }
}
