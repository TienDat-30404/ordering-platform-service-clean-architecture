package com.example.demo.application.ports.input;


import com.example.demo.application.dto.command.RemoveItemsCommand;
import com.example.demo.application.dto.output.TrackOrderResponse;

public interface RemoveItemsUseCase {
    TrackOrderResponse removeItems(RemoveItemsCommand command);
}
