package com.example.demo.application.ports.output.repository;

import com.example.demo.domain.valueobject.order.Voucher;

public interface VoucherCheckPort {
    Voucher validateAndGetVoucher(String voucherCode);
}