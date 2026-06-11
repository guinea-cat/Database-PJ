package com.example.airticket.controller;

import com.example.airticket.common.ApiResponse;
import com.example.airticket.entity.MealOption;
import com.example.airticket.exception.BusinessException;
import com.example.airticket.repository.MealOptionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meal")
public class MealController {
    private final MealOptionRepository mealRepository;

    public MealController(MealOptionRepository mealRepository) {
        this.mealRepository = mealRepository;
    }

    @GetMapping("/list")
    public ApiResponse<List<MealOption>> list() {
        return ApiResponse.success(mealRepository.findAll());
    }

    @GetMapping("/detail")
    public ApiResponse<MealOption> detail(@RequestParam Integer mealId) {
        return ApiResponse.success(mealRepository.findById(mealId)
                .orElseThrow(() -> new BusinessException(40408, "餐食不存在")));
    }
}
