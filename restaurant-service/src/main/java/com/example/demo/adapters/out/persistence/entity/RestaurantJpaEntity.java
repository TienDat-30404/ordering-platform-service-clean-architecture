package com.example.demo.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "restaurants")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "rating", nullable = false)
    private BigDecimal rating;

    @Column(name = "totalRatings")
    private Integer totalRatings;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MenuItemJpaEntity> menu = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}