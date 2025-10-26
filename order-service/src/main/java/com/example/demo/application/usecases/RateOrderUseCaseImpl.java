package com.example.demo.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.dto.command.RateOrderCommand;
import com.example.demo.application.ports.input.RateOrderUseCase;
import com.example.demo.application.ports.output.repository.OrderRatingRepositoryPort;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.entity.OrderRating;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.order.RatingScore;
import com.example.demo.domain.valueobject.user.UserId;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RateOrderUseCaseImpl implements RateOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderRatingRepositoryPort ratingRepositoryPort;

    @Override
    @Transactional
    public void rateOrder(RateOrderCommand command, UserId userId) {
        // 1. Tải Order
        OrderId orderId = new OrderId(command.getOrderId());
        Order order = orderRepositoryPort.findById(orderId);

        // 2. Áp dụng Logic Nghiệp vụ (Domain Logic)
        order.validateForRating(); // Kiểm tra trạng thái APPROVED và chưa rated

        // 3. Tạo OrderRating Domain Entity
        RatingScore score = new RatingScore(command.getScore());

        OrderRating rating = new OrderRating(
                orderId,
                userId,
                score,
                command.getComment());
        
        // 4. Lưu OrderRating và cập nhật trạng thái Order
        ratingRepositoryPort.save(rating);

        order.setHasBeenRated(true); // Đánh dấu đã rated
        orderRepositoryPort.save(order);
    }
}