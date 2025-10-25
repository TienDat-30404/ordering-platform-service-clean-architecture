package com.example.demo.domain.valueobject.order;

public enum OrderStatus {

    // Trạng thái theo yêu cầu
    PENDING("PENDING"),        // tạo đơn xong, đang chạy saga (validate/payment)
    APPROVED("APPROVED"),      // qua bước check/validate OK (ví dụ: restaurant validate OK)
    PAID("PAID"),              // thanh toán đã AUTHORIzed/đã thu
    COMPLETED("COMPLETED"),    // hoàn tất (giao xong / kết thúc quy trình)
    CANCELLING("CANCELLING"),  // đang bù trừ/huỷ (reverse payment, rollback)
    CANCELLED("CANCELLED");    // đã huỷ xong

    private final String displayValue;

    OrderStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getName() {
        return displayValue;
    }

    // (tuỳ chọn) tiện ích nhỏ cho kiểm tra kết thúc
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
