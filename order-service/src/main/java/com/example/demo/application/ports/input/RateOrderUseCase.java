
package com.example.demo.application.ports.input;

import com.example.demo.application.dto.command.RateOrderCommand;
import com.example.demo.domain.valueobject.user.UserId;

public interface RateOrderUseCase {
    void rateOrder(RateOrderCommand command, UserId userId);
}