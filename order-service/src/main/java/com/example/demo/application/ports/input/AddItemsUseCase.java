package com.example.demo.application.ports.input;

import com.example.demo.application.dto.command.AddItemsCommand;
import com.example.demo.application.dto.output.TrackOrderResponse;

public interface AddItemsUseCase {
    TrackOrderResponse addItems(AddItemsCommand command);
}
