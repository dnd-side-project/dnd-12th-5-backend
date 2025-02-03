package com.picktory.domain.user.controller;

import com.picktory.domain.user.dto.UserLoginRequest;
import com.picktory.domain.user.dto.UserLoginResponse;
import com.picktory.domain.user.dto.UserResponse;
import com.picktory.domain.user.service.UserService;
import com.picktory.common.BaseResponse;
import com.picktory.common.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/oauth/login")
    public BaseResponse<UserLoginResponse> login(@RequestBody UserLoginRequest request) {
        UserLoginResponse response = userService.login(request.getCode());
        return new BaseResponse<>(response);
    }

    @GetMapping("/user/me")
    public BaseResponse<UserResponse> getMyInfo() {
        UserResponse response = userService.getMyInfo();
        return new BaseResponse<>(response);
    }

    @DeleteMapping("/user/me")
    public BaseResponse<Void> withdraw() {
        userService.withdraw();
        return new BaseResponse<>(BaseResponseStatus.SUCCESS);
    }

    @PostMapping("/oauth/logout")
    public BaseResponse<Void> logout() {
        userService.logout();
        return new BaseResponse<>(BaseResponseStatus.SUCCESS);
    }
}