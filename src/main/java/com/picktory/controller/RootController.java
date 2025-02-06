package com.picktory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RootController {
    // Home 엔드포인트 추가: /api/v1/ 로 접근 시 반환
    @GetMapping("/")
    public String home() {
        return "Welcome to Picktory API";
    }
}
