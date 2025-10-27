package com.example.common_dtos.enums;

/**
 * Trạng thái giao dịch thanh toán tại Payment Service.
 * Dùng chung cho toàn hệ thống (chia sẻ qua common_dtos).
 *
 * Mỗi trạng thái mapping trực tiếp với SagaStatus:
 * STARTED → PENDING → AUTHORIZED / FAILED → REFUNDED / CANCELED
 */
public enum PaymentStatus {

    /**
     * Giao dịch vừa khởi tạo, chưa xử lý.
     * (SagaStatus.STARTED)
     */
    PENDING,

    /**
     * Thanh toán được chấp thuận (đã authorize).
     * (SagaStatus.PAYMENT_AUTHORIZED)
     */
    AUTHORIZED,

    /**
     * Thanh toán bị từ chối (ví dụ: vượt hạn mức, lỗi cổng thanh toán, v.v.)
     * (SagaStatus.PAYMENT_FAILED)
     */
    FAILED,

    /**
     * Giao dịch đã hoàn tiền (refund).
     * (SagaStatus.COMPENSATED)
     */
    REFUNDED,

    /**
     * Giao dịch bị hủy thủ công hoặc rollback toàn bộ saga.
     * (SagaStatus.CANCELLED)
     */
    CANCELED;

    /** Xác định xem giao dịch đã ở trạng thái kết thúc. */
    public boolean isTerminal() {
        return switch (this) {
            case FAILED, REFUNDED, CANCELED -> true;
            default -> false;
        };
    }

    /** Xác định xem giao dịch là lỗi / rollback. */
    public boolean isFailure() {
        return this == FAILED || this == CANCELED;
    }
}
