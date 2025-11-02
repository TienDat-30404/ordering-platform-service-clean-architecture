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
    COMPENSATION_STARTED,        // alias cho đang rollback (tên mới)
    COMPENSATED,                 // rollback hoàn tất
    CANCELLED,                   // saga bị hủy
    UNKNOWN,
    PREPARING;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == COMPENSATED;
    }
}
