package com.example.demo.application.usecases;

import com.example.demo.domain.valueobject.order.OrderStatus;
import com.example.demo.application.ports.input.UpdateOrderStatusUseCase;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCaseImpl implements UpdateOrderStatusUseCase {

    private final OrderRepositoryPort orderRepository;

    @Override
    public void setStatus(String orderId, OrderStatus status) {
        try {
            // repository của bạn đã có, giả sử có hàm updateStatus(orderId, status)
            orderRepository.updateStatus(orderId, status);
            log.info("[ORDER] {} -> {}", orderId, status);
        } catch (Exception e) {
            log.error("[ORDER] update status failed {} -> {} err={}", orderId, status, e.toString(), e);
            throw e;
        }
    }
}
