package com.picktory.domain.user.controller;

import com.picktory.common.BaseResponse;
import com.picktory.domain.user.dto.UserResponse;
import com.picktory.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회 API
     *
     * @return 사용자 정보 응답
     */
    @GetMapping("/user/me")
    public ResponseEntity<BaseResponse<UserResponse>> getMyInfo() {
        log.info("Get my info request received");
        UserResponse response = userService.getMyInfo();
        return ResponseEntity.ok(BaseResponse.success(response, "내 정보 조회 성공"));
    }

    /**
     * 회원 탈퇴 API
     *
     * @return 성공 응답
     */
    @DeleteMapping("/user/me")
    public ResponseEntity<BaseResponse<Void>> withdraw() {
        log.info("Withdraw request received");
        userService.withdraw();
        return ResponseEntity.ok(BaseResponse.success(null, "회원 탈퇴 성공"));
    }

    /**
     * 로그아웃 API
     *
     * @return 성공 응답
     */
    @PostMapping("/oauth/logout")
    public ResponseEntity<BaseResponse<Void>> logout() {
        log.info("Logout request received");
        userService.logout();
        return ResponseEntity.ok(BaseResponse.success(null, "로그아웃 성공"));
    }
}