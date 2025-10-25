package com.example.common_dtos.enums;

/**
 * Đại diện trạng thái tiến trình Saga orchestration toàn hệ thống.
 * Dùng cho logging, trace hoặc dashboard.
 */
public enum SagaStatus {
    STARTED,                     // saga bắt đầu từ Order
    RESTAURANT_VALIDATION_OK,    // Restaurant xác nhận menu hợp lệ
    RESTAURANT_VALIDATION_FAIL,  // Restaurant báo lỗi menu
    PAYMENT_AUTHORIZED,          // Payment đã authorize
    PAYMENT_FAILED,              // Payment bị từ chối
    COMPLETED,                   // toàn bộ saga kết thúc thành công
    COMPENSATING,                // đang rollback
    COMPENSATED,                 // rollback hoàn tất
    CANCELLED,                   // saga bị hủy
    UNKNOWN;                     // trạng thái không xác định

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == COMPENSATED;
    }
}
