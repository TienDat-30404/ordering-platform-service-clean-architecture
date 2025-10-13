package com.example.demo.adapters.out.persistence.adapter;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.example.demo.application.ports.output.repository.VoucherCheckPort;
import com.example.demo.domain.valueobject.order.Voucher;

@Service
public class VoucherCheckAdapter implements VoucherCheckPort {
    @Override
    public Voucher validateAndGetVoucher(String voucherCode) {
        // Đây là nơi logic kiểm tra DB/API bên ngoài xảy ra.
        if ("CODE100K".equalsIgnoreCase(voucherCode)) {
            return new Voucher("CODE100K", new BigDecimal("100000.00"), false);
        }
        if ("SALE20PERCENT".equalsIgnoreCase(voucherCode)) {
            throw new RuntimeException("Percentage discount not supported in this simple example.");
        }   
        throw new RuntimeException("Invalid or expired voucher code: " + voucherCode);
    }
}