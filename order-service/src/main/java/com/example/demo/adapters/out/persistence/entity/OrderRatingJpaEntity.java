package com.example.demo.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "order_ratings")
@Data
public class OrderRatingJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "order_id") 
    private OrderJpaEntity order;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "score", nullable = false)
    private Integer score; // 1-5

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}