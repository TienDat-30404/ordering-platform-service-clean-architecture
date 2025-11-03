    package com.example.common_dtos.enums;

    /**
     * Trạng thái vòng đời của đơn hàng ở cấp hệ thống (Order Service).
     * Tương thích với luồng orchestrated saga hiện tại.
     */
    public enum OrderStatus {
        PENDING,      // tạo đơn xong, đang chạy saga (validate/payment)
        APPROVED,     // qua bước kiểm tra/validate OK (nhà hàng xác nhận menu hợp lệ)
        PAID,         // thanh toán đã được authorize/captured
        COMPLETED,    // kết thúc thành công
        CANCELLING,   // đang bù trừ (reverse payment, rollback)
        CANCELLED,    // đã huỷ
        PREPARING;

        public boolean isTerminal() {
            return this == COMPLETED || this == CANCELLED;
        }
    }
