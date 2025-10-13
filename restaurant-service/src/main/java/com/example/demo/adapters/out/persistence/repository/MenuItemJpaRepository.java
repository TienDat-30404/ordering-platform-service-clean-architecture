package com.example.demo.adapters.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.adapters.out.persistence.entity.MenuItemJpaEntity;

public interface MenuItemJpaRepository extends JpaRepository<MenuItemJpaEntity, Long> {

}
