package com.picktory.domain.user.service;

import com.picktory.config.jwt.JwTokenDto;
import com.picktory.config.jwt.JwtTokenProvider;
import com.picktory.domain.user.dto.*;
import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.repository.UserRepository;
import com.picktory.common.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
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

    private static final String KAKAO_AUTH_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_API_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    @Transactional
    public UserLoginResponse login(String code) {
        try {
            log.info("Starting login process with code: {}", code);

            String kakaoAccessToken = getKakaoAccessToken(code);
            log.info("Received Kakao access token successfully");

            KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(kakaoAccessToken);
            log.info("Retrieved Kakao user info for id: {}", kakaoUserInfo.getId());

            User user = userRepository.findByKakaoId(kakaoUserInfo.getId())
                    .map(foundUser -> {
                        log.info("Found existing user: {}", foundUser.getId());
                        if (foundUser.isDeleted()) {
                            throw new IllegalStateException(BaseResponseStatus.ALREADY_DELETED_USER.getMessage());
                        }
                        return foundUser;
                    })
                    .orElseGet(() -> {
                        log.info("Creating new user for Kakao id: {}", kakaoUserInfo.getId());
                        return createUser(kakaoUserInfo);
                    });

            JwTokenDto tokenDto = jwtTokenProvider.generateToken(user.getId().toString());
            log.info("Login successful for user: {}", user.getId());

            return new UserLoginResponse(tokenDto.getAccessToken(), tokenDto.getRefreshToken());

        } catch (IllegalStateException e) {
            log.error("Login failed with IllegalStateException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            throw new IllegalStateException(BaseResponseStatus.SERVER_ERROR.getMessage());
        }
    }

    private String getKakaoAccessToken(String code) {
        log.debug("Requesting Kakao access token with code: {}", code);
        String url = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        params.add("client_secret", adminKey);

        log.debug("Kakao token request params - client_id: {}, redirect_uri: {}", clientId, redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(url, request, KakaoTokenResponse.class);
            log.info("Kakao token response status: {}", response.getStatusCode());

            if (response.getBody() == null || response.getBody().getAccess_token() == null) {
                log.error("Failed to get Kakao access token: empty response");
                throw new IllegalStateException("카카오 로그인 처리 중 오류가 발생했습니다.");
            }

            return response.getBody().getAccess_token();

        } catch (HttpClientErrorException e) {
            log.error("Kakao token error - Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("다시 로그인해 주세요.");
        } catch (Exception e) {
            log.error("Failed to get Kakao access token", e);
            throw new IllegalStateException("카카오 로그인 처리 중 오류가 발생했습니다.");
        }
    }

    private KakaoUserInfo getKakaoUserInfo(String accessToken) {
        try {
            log.debug("Requesting Kakao user info with access token");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                    KAKAO_API_URL,
                    HttpMethod.GET,
                    request,
                    KakaoUserInfo.class
            );

            if (response.getBody() == null) {
                log.error("Kakao user info response is empty");
                throw new IllegalStateException(BaseResponseStatus.KAKAO_API_ERROR.getMessage());
            }

            log.info("카카오 유저 정보를 성공적으로 받음");
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("Failed to get Kakao user info - Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("카카오 사용자 정보 조회 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("Unexpected error while getting Kakao user info", e);
            throw new IllegalStateException("카카오 사용자 정보 조회 중 오류가 발생했습니다.");
        }
    }

    public UserResponse getMyInfo() {
        User user = getCurrentActiveUser();
        log.debug("Retrieved user info: userId={}", user.getId());
        return UserResponse.from(user);
    }

    @Transactional
    public void withdraw() {
        User user = getCurrentActiveUser();
        log.info("Processing withdrawal for user: {}", user.getId());

        try {
            unlinkKakaoAccount(user.getKakaoId());
            user.delete();
            log.info("User successfully withdrawn: {}", user.getId());

        } catch (Exception e) {
            log.error("Failed to withdraw user: {}", user.getId(), e);
            throw new IllegalStateException(BaseResponseStatus.KAKAO_API_ERROR.getMessage());
        }
    }

    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            log.info("User logged out: {}", authentication.getName());
        }
        SecurityContextHolder.clearContext();
    }

    private User getCurrentActiveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found");
            throw new IllegalStateException(BaseResponseStatus.INVALID_JWT.getMessage());
        }

        Long userId = Long.parseLong(authentication.getName());
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> {
                    log.warn("User not found or deleted: {}", userId);
                    return new IllegalStateException(BaseResponseStatus.USER_NOT_FOUND.getMessage());
                });
    }

    private User createUser(KakaoUserInfo userInfo) {
        User newUser = User.builder()
                .kakaoId(userInfo.getId())
                .nickname(userInfo.getKakaoAccount().getProfile().getNickname())
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("New user created: {}", savedUser.getId());
        return savedUser;
    }

    private void unlinkKakaoAccount(Long kakaoId) {
        try {
            log.debug("Unlinking Kakao account: {}", kakaoId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "KakaoAK " + adminKey);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("target_id_type", "user_id");
            params.add("target_id", String.valueOf(kakaoId));

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            restTemplate.postForEntity(KAKAO_UNLINK_URL, request, String.class);

            log.info("Successfully unlinked Kakao account: {}", kakaoId);

        } catch (Exception e) {
            log.error("Failed to unlink Kakao account: {}", kakaoId, e);
            throw new IllegalStateException("카카오 계정 연결 해제 중 오류가 발생했습니다.");
        }
    }
}