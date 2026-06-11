package com.example.airticket.dto.response;

import com.example.airticket.entity.User;

import java.time.LocalDateTime;

public class AuthResponse {
    public Integer userId;
    public String loginAccount;
    public String userName;
    public String userType;
    public String memberLevel;
    public Integer points;
    public String token;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public static AuthResponse from(User user, String token) {
        AuthResponse response = new AuthResponse();
        response.userId = user.userId;
        response.loginAccount = user.loginAccount;
        response.userName = user.userName;
        response.userType = user.userType.name();
        response.memberLevel = user.memberLevel.name();
        response.points = user.points;
        response.token = token;
        response.createdAt = user.createdAt;
        response.updatedAt = user.updatedAt;
        return response;
    }
}
