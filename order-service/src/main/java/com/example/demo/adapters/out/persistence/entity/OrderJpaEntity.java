package com.example.demo.adapters.out.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemJpaEntity> items = new ArrayList<>();
    private BigDecimal amount;

    @Column(name = "voucher_code")
    private String voucherCode;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "final_price")
    private BigDecimal finalPrice;

    @Column(name = "has_been_rated", nullable = false)
    private Boolean hasBeenRated;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;
}