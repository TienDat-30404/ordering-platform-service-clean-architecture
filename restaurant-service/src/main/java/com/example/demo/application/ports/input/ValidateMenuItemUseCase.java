package com.example.demo.application.ports.input;

import java.util.List;

import com.example.common_dtos.dto.ItemValidationRequest;
import com.example.common_dtos.dto.ItemValidationResponse;


public interface ValidateMenuItemUseCase {
    List<ItemValidationResponse> validateItems(ItemValidationRequest request);
}
