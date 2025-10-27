package com.example.demo.payment;

/**
 * Trạng thái thanh toán chuẩn hoá, tương thích với luồng hiện tại.
 * Có thể dùng cho entity/log hoặc mapping kết quả PaymentService.
 */
public enum PaymentStatus {
    PENDING,     // mới nhận lệnh AUTHORIZE_PAYMENT, đang xử lý
    AUTHORIZED,  // uỷ quyền/thu tiền thành công
    FAILED,      // uỷ quyền thất bại
    REFUNDED,    // đã hoàn tiền/void (compensation)
    CANCELED;    // huỷ giao dịch theo yêu cầu (không phải refund)

    public boolean isTerminal() {
        return this == AUTHORIZED || this == FAILED || this == REFUNDED || this == CANCELED;
    }

    public boolean isSuccess() {
        return this == AUTHORIZED;
    }
}
