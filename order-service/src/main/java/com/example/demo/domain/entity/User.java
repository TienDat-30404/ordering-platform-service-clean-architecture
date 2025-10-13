package com.example.demo.domain.entity;

import com.example.demo.domain.valueobject.user.UserId;

public class User {
    private UserId id;
    private String userName;
    private String password;

    public User(UserId id, String userName, String password) {
        this.id = id;
        this.userName = userName;
        this.password = password;
    }

    public UserId getUserId() {
        return id;
    }
    public String userName() {
        return userName;
    }
    public String password() {
        return password;
    }
}
