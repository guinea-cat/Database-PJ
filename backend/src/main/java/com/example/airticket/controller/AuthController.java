package com.example.airticket.controller;

import com.example.airticket.common.ApiResponse;
import com.example.airticket.dto.request.IdRequest;
import com.example.airticket.dto.request.LoginRequest;
import com.example.airticket.dto.request.RegisterRequest;
import com.example.airticket.dto.response.AuthResponse;
import com.example.airticket.entity.User;
import com.example.airticket.repository.UserRepository;
import com.example.airticket.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ApiResponse.success(AuthResponse.from(user, "LOCAL-" + user.userId));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = authService.login(request);
        return ApiResponse.success(AuthResponse.from(user, "LOCAL-" + user.userId));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success(null);
    }

    @PostMapping("/cancel")
    public ApiResponse<Void> cancel(@RequestBody IdRequest request) {
        authService.cancelAccount(request.userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<AuthResponse> me(@RequestParam Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return ApiResponse.success(AuthResponse.from(user, "LOCAL-" + user.userId));
    }
}
