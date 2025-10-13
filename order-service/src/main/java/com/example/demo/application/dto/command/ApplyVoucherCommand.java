package com.example.demo.application.dto.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplyVoucherCommand {
    private Long orderId;
    private String voucherCode;
}