package com.example.demo.application.ports.input;

import com.example.demo.application.dto.command.ApplyVoucherCommand;

public interface ApplyVoucherUseCase {
    void applyVoucher(ApplyVoucherCommand command);
}
