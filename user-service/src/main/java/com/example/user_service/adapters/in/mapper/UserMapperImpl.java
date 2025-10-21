package com.example.user_service.adapters.in.mapper;

import org.springframework.stereotype.Component;

import com.example.user_service.application.dto.command.RegisterCommand;
import com.example.user_service.application.dto.output.UserResponse;
import com.example.user_service.application.dto.output.RoleResponse;
import com.example.user_service.application.mapper.UserMapper;
import com.example.user_service.domain.entity.Role;
import com.example.user_service.domain.entity.User;

@Component
public class UserMapperImpl implements UserMapper {
     public UserResponse toDTO(User user) {
        UserResponse response = new UserResponse();
        
        response.setId(user.getId().value());
        response.setName(user.getName());
        response.setUserName(user.getUserName());
        System.out.println("ttttttttttttttttttt" + user);
        if(user.getRole() != null) {
            System.out.println("32222222222222222222222222222222222222");
            Role role = user.getRole();
            response.setRole(new RoleResponse(role.getId().value(), role.getName()));
        }
        return response;
     }
    public User toDomain(RegisterCommand command, Role role) {
        return new User(
            command.getName(), 
            command.getUserName(), 
            command.getPassword(),
            role
        );
    }
}
