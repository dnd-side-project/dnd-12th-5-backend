package com.picktory.domain.user.service;

import com.picktory.config.jwt.JwTokenDto;
import com.picktory.config.jwt.JwtTokenProvider;
import com.picktory.domain.user.dto.*;
import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.repository.UserRepository;
import com.picktory.common.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    @Value("${kakao.client.id}")
    private String clientId;

    @Value("${kakao.client.secret}")
    private String adminKey;

    @Value("${kakao.redirect.uri}")
    private String redirectUri;

    // 카카오 소셜 로그인
    @Transactional
    public UserLoginResponse login(String code) {
        // 카카오 액세스 토큰 발급
        String kakaoAccessToken = getKakaoAccessToken(code);
        // 카카오 사용자 정보 조회
        KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(kakaoAccessToken);

        // 기존 회원인 경우 로그인, 신규 회원인 경우 회원가입 처리
        User user = userRepository.findByKakaoId(String.valueOf(kakaoUserInfo.getId()))
                .map(foundUser -> {
                    if (foundUser.isDeleted()) {
                        throw new IllegalStateException(BaseResponseStatus.USER_NOT_FOUND.getMessage());
                    }
                    return foundUser;
                })
                .orElseGet(() -> createUser(kakaoUserInfo));

        // JWT 토큰 발급
        JwTokenDto tokenDto = jwtTokenProvider.generateToken(user.getId().toString());
        return new UserLoginResponse(tokenDto.getAccessToken(), tokenDto.getRefreshToken());
    }

    // 내 정보 조회
    public UserResponse getMyInfo() {
        User user = getCurrentActiveUser();
        return UserResponse.from(user);
    }

    // 회원 탈퇴
    @Transactional
    public void withdraw() {
        User user = getCurrentActiveUser();

        try {
            // 카카오 연결끊기 API 호출
            unlinkKakaoAccount(user.getKakaoId());

            // 사용자 계정 삭제 처리
            user.delete();
        } catch (Exception e) {
            throw new IllegalStateException(BaseResponseStatus.KAKAO_API_ERROR.getMessage());
        }
    }

    // 로그아웃
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    // 현재 로그인한 사용자 조회
    public User getCurrentActiveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(BaseResponseStatus.INVALID_JWT.getMessage());
        }

        Long currentUserId = Long.parseLong(authentication.getName());
        return userRepository.findByIdAndIsDeletedFalse(currentUserId)
                .orElseThrow(() -> new IllegalStateException(BaseResponseStatus.USER_NOT_FOUND.getMessage()));
    }

    // 신규 사용자 생성
    private User createUser(KakaoUserInfo userInfo) {
        return userRepository.save(
                User.builder()
                        .kakaoId(String.valueOf(userInfo.getId()))
                        .nickname(userInfo.getKakaoAccount().getProfile().getNickname())
                        .build()
        );
    }

    // 카카오 액세스 토큰 발급
    private String getKakaoAccessToken(String code) {
        String url = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(url, request, KakaoTokenResponse.class);

        return response.getBody().getAccess_token();
    }

    // 카카오 사용자 정보 조회
    private KakaoUserInfo getKakaoUserInfo(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                KakaoUserInfo.class
        );

        return response.getBody();
    }

    // 카카오 계정 연결끊기
    private void unlinkKakaoAccount(String kakaoId) {
        String url = "https://kapi.kakao.com/v1/user/unlink";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "KakaoAK " + adminKey);  // Admin 키로 인증

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target_id_type", "user_id");
        params.add("target_id", kakaoId);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        restTemplate.postForEntity(url, request, String.class);
    }
}