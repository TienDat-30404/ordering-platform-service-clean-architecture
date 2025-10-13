package com.example.demo.application.ports.output;

import java.util.List;

import com.example.demo.domain.entity.MenuItem;

public interface MenuItemRepositoryPort {
    List<MenuItem> findAllById(List<Long> menuItemIds);
}
