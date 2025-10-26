package com.example.user_service.domain.entity;

import com.example.user_service.domain.valueobject.RoleId;

public class Role {
    private RoleId id;
    private String name;

    public Role(RoleId id, String name) {
        this.id = id;
        this.name = name;
    }

    public RoleId getId() {
        return id;
    }
    public String getName() {
        return name;
    }


    @Override
    public String toString() {
        return "Restaurant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
