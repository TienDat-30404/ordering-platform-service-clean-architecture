package com.example.common_dtos.enums;

/**
 * Trạng thái giao dịch thanh toán dùng chung cho toàn hệ thống.
 * Dùng giữa Order / Payment / Orchestrator qua common_dtos.
 *
 * Flow:
 * STARTED → PENDING
 * PENDING → AUTHORIZED / FAILED
 * AUTHORIZED → REFUND_REQUESTED → REFUND_COMPLETED / CANCELED
 */
public enum PaymentStatus {

    /** Giao dịch vừa khởi tạo, chưa xử lý. */
    PENDING,

    /** Thanh toán đã authorize/hold thành công. */
    AUTHORIZED,

    /** Thanh toán thất bại (lỗi gateway, vượt hạn mức, v.v.). */
    FAILED,

    /** Đã tạo yêu cầu hoàn tiền/huỷ hold, đang chờ xử lý. */
    REFUND_REQUESTED,

    /** Hoàn tiền/huỷ hold đã xử lý xong. */
    REFUND_COMPLETED,

    /** Hủy thủ công hoặc rollback toàn bộ saga. */
    CANCELED;

    /** Giao dịch đã kết thúc vòng đời? */
    public boolean isTerminal() {
        return switch (this) {
            case FAILED, REFUND_COMPLETED, CANCELED -> true;
            default -> false;
        };
    }

    /** Giao dịch kết thúc do lỗi/rollback? */
    public boolean isFailure() {
        return this == FAILED || this == CANCELED;
    }
}
