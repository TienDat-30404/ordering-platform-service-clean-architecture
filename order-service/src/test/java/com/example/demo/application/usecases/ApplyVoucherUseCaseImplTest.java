package com.example.demo.application.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.application.dto.command.ApplyVoucherCommand;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.application.ports.output.repository.VoucherCheckPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.order.Voucher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ApplyVoucherUseCaseImpl.
 * - Dùng Mockito để mock các phụ thuộc (repository và voucher service)
 * - Kiểm tra các tương tác và luồng nghiệp vụ chính.
 */
@ExtendWith(MockitoExtension.class)
class ApplyVoucherUseCaseImplTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private VoucherCheckPort voucherCheckPort;

    @InjectMocks
    private ApplyVoucherUseCaseImpl useCase;

    private ApplyVoucherCommand command;
    private final Long orderId = 123L;
    private final String voucherCode = "SALE50";

    @BeforeEach
    void setup() {
      command = ApplyVoucherCommand.builder()
        .orderId(orderId)
        .voucherCode(voucherCode)
        .build();
    }

    @Test
    void applyVoucher_successfulFlow() {
        // Arrange
        Order order = org.mockito.Mockito.mock(Order.class);
        Voucher voucher = org.mockito.Mockito.mock(Voucher.class);

        // Giả lập repository trả về Order có tồn tại
        when(orderRepositoryPort.findById(any(OrderId.class))).thenReturn(order);
        // Giả lập dịch vụ external trả về voucher hợp lệ
        when(voucherCheckPort.validateAndGetVoucher(voucherCode)).thenReturn(voucher);

        // Act
        useCase.applyVoucher(command);

        // Assert
        // Kiểm tra luồng logic chính đã được thực hiện đúng
        verify(orderRepositoryPort).findById(any(OrderId.class));
        verify(voucherCheckPort).validateAndGetVoucher(voucherCode);
        verify(order).applyDiscount(voucher);
        verify(orderRepositoryPort).save(order);
    }

    @Test
    void applyVoucher_whenOrderNotFound_shouldThrowException() {
        // Arrange
        when(orderRepositoryPort.findById(any(OrderId.class))).thenReturn(null);

        // Act & Assert
        // Vì code hiện tại chưa có .orElseThrow(), ta kiểm tra NullPointerException.
        // Sau này nếu bạn thêm ngoại lệ domain riêng, có thể thay dòng này bằng assertThrows(OrderDomainException.class, ...)
        assertThrows(NullPointerException.class, () -> useCase.applyVoucher(command));
    }
}
