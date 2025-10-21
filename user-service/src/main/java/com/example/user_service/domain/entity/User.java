package com.example.user_service.domain.entity;


import com.example.user_service.domain.valueobject.UserId;


public class User {
    private UserId id;
    private String name;
    private String userName;
    private String password;
    private Role role;

    public User(UserId id, String name, String userName, String password, Role role) {
        this.id = id;
        this.name = name;
        this.userName = userName;
        this.password = password;
        this.role = role;
    }

    public User(String name, String userName, String password, Role role) {
        this.name = name;
        this.userName = userName;
        this.password = password;
        this.role = role;
    }

    public UserId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", userName=" + userName +
                '}';
    }

}
