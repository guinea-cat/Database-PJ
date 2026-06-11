package com.example.airticket.controller;

import com.example.airticket.common.ApiResponse;
import com.example.airticket.entity.User;
import com.example.airticket.exception.BusinessException;
import com.example.airticket.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/member")
public class MemberController {
    private final UserRepository userRepository;

    public MemberController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping({"/profile", "/points"})
    public ApiResponse<Map<String, Object>> profile(@RequestParam Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.userId);
        data.put("loginAccount", user.loginAccount);
        data.put("userName", user.userName);
        data.put("memberLevel", user.memberLevel.name());
        data.put("points", user.points);
        data.put("vipThreshold", 1000);
        data.put("vipDiscountRate", 0.9);
        return ApiResponse.success(data);
    }
}
