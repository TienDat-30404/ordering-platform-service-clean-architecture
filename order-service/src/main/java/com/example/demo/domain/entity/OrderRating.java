package com.example.demo.domain.entity;

import java.time.Instant;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.order.OrderRatingId;
import com.example.demo.domain.valueobject.order.OrderStatus;
import com.example.demo.domain.valueobject.order.RatingScore;
import com.example.demo.domain.valueobject.user.UserId;

public class OrderRating {
    private OrderRatingId id; // ID của bản ghi đánh giá
    private OrderId orderId;
    private final UserId customerId;
    private RatingScore score;
    private String comment;
    private final Instant createdAt;

    public OrderRating(OrderId orderId, UserId customerId, RatingScore score, String comment) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.score = score;
        this.comment = comment;
        this.createdAt = Instant.now();
    }

    // Constructor cho việc tái tạo từ Persistence
    public OrderRating(OrderRatingId id, UserId customerId, RatingScore score, String comment, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.score = score;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public OrderRatingId getId() {return id;}
    public OrderId getOrderId() { return orderId; }
    public UserId getCustomerId() { return customerId; }
    public RatingScore getScore() { return score; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt;}


}