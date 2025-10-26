package com.example.demo.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.dto.command.ApplyVoucherCommand;
import com.example.demo.application.ports.input.ApplyVoucherUseCase;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.application.ports.output.repository.VoucherCheckPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.order.Voucher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApplyVoucherUseCaseImpl implements ApplyVoucherUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final VoucherCheckPort voucherCheckPort; 

    @Override
    @Transactional
    public void applyVoucher(ApplyVoucherCommand command) {
        // 1. Tải Order
        OrderId orderId = new OrderId(command.getOrderId());
        Order order = orderRepositoryPort.findById(orderId);
            // .orElseThrow(() -> new OrderDomainException("Order not found with ID: " + orderId.value()));
            
        // 2. Kiểm tra và lấy thông tin Voucher (qua Output Port)
        Voucher voucher = voucherCheckPort.validateAndGetVoucher(command.getVoucherCode());
        
        // 3. Áp dụng Logic Nghiệp vụ (Domain Logic)
        order.applyDiscount(voucher);

        // 4. Lưu thay đổi
        orderRepositoryPort.save(order);
    }
}