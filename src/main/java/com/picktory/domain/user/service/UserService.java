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

    @Value("${kakao.redirect.uri}")
    private String redirectUri;

    @Transactional
    public UserLoginResponse login(String code) {
        String kakaoAccessToken = getKakaoAccessToken(code);
        KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(kakaoAccessToken);

        User user = userRepository.findByKakaoId(String.valueOf(kakaoUserInfo.getId()))
                .map(foundUser -> {
                    if (foundUser.isDeleted()) {
                        throw new IllegalStateException(BaseResponseStatus.USER_NOT_FOUND.getMessage());
                    }
                    return foundUser;
                })
                .orElseGet(() -> createUser(kakaoUserInfo));

        JwTokenDto tokenDto = jwtTokenProvider.generateToken(user.getId().toString());
        return new UserLoginResponse(tokenDto.getAccessToken(), tokenDto.getRefreshToken());
    }

    public UserResponse getMyInfo() {
        User user = getCurrentActiveUser();
        return UserResponse.from(user);
    }

    @Transactional
    public void withdraw() {
        User user = getCurrentActiveUser();
        user.delete();
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }

    private User getCurrentActiveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(BaseResponseStatus.INVALID_JWT.getMessage());
        }

        Long currentUserId = Long.parseLong(authentication.getName());
        return userRepository.findByIdAndIsDeletedFalse(currentUserId)
                .orElseThrow(() -> new IllegalStateException(BaseResponseStatus.USER_NOT_FOUND.getMessage()));
    }

    private User createUser(KakaoUserInfo userInfo) {
        return userRepository.save(
                User.builder()
                        .kakaoId(String.valueOf(userInfo.getId()))
                        .nickname(userInfo.getKakaoAccount().getProfile().getNickname())
                        .build()
        );
    }

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
}