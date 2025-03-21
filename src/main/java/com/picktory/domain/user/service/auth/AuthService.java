package com.picktory.domain.user.service.auth;

import com.picktory.config.jwt.JwtTokenProvider;
import com.picktory.config.jwt.dto.TokenDto;
import com.picktory.domain.oauth.dto.KakaoUserInfo;
import com.picktory.domain.refreshToken.entity.RefreshToken;
import com.picktory.domain.refreshToken.service.RefreshTokenService;
import com.picktory.domain.user.dto.UserLoginResponse;
import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoService kakaoService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    /**
     * 카카오 소셜 로그인을 처리합니다.
     *
     * @param code 카카오 인증 코드
     * @return 로그인 응답 (JWT 토큰 포함)
     */
    @Transactional
    public UserLoginResponse loginWithKakao(String code) {
        try {
            log.info("Starting Kakao login process with code: {}", code);

            // 1. 카카오 액세스 토큰 요청
            String kakaoAccessToken = kakaoService.getKakaoAccessToken(code);
            log.info("Received Kakao access token successfully");

            // 2. 카카오 사용자 정보 요청
            KakaoUserInfo kakaoUserInfo = kakaoService.getKakaoUserInfo(kakaoAccessToken);
            log.info("Retrieved Kakao user info for id: {}", kakaoUserInfo.getId());

            // 3. 사용자 정보 조회 또는 생성
            User user = findOrCreateUser(kakaoUserInfo);

            // 4. JWT 토큰 발급
            TokenDto tokenDto = jwtTokenProvider.generateToken(user.getId().toString());
            log.info("Login successful for user: {}", user.getId());

            // 5. 리프레시 토큰을 DB에 저장
            LocalDateTime expiryDate = tokenDto.getAccessTokenExpiresIn()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            refreshTokenService.createRefreshToken(
                    user.getId().toString(),
                    tokenDto.getRefreshToken(),
                    expiryDate
            );

            return new UserLoginResponse(tokenDto.getAccessToken(), tokenDto.getRefreshToken());

        } catch (IllegalStateException e) {
            log.error("Login failed with IllegalStateException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            throw new IllegalStateException("서버 오류가 발생했습니다.");
        }
    }

    /**
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다.
     *
     * @param refreshToken 리프레시 토큰
     * @return 새로운 로그인 응답 (JWT 토큰 포함)
     */
    @Transactional
    public UserLoginResponse refreshToken(String refreshToken) {
        try {
            // 1. 리프레시 토큰 검증
            RefreshToken refreshTokenEntity = refreshTokenService.findByToken(refreshToken)
                    .orElseThrow(() -> new IllegalStateException("유효하지 않은 리프레시 토큰입니다."));

            // 2. 만료 여부 확인
            refreshTokenService.verifyExpiration(refreshTokenEntity);

            // 3. 사용자 조회
            String userId = refreshTokenEntity.getUserId();
            User user = userRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

            // 4. 새 액세스 토큰 발급
            TokenDto tokenDto = jwtTokenProvider.generateToken(userId);

            // 5. 리프레시 토큰 업데이트
            LocalDateTime expiryDate = tokenDto.getAccessTokenExpiresIn()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            refreshTokenService.createRefreshToken(userId, tokenDto.getRefreshToken(), expiryDate);

            return new UserLoginResponse(tokenDto.getAccessToken(), tokenDto.getRefreshToken());
        } catch (Exception e) {
            log.error("Error while refreshing token", e);
            throw new IllegalStateException("토큰 갱신 중 오류가 발생했습니다.");
        }
    }

    /**
     * 카카오 사용자 정보를 기반으로 사용자를 조회하거나 생성합니다.
     *
     * @param kakaoUserInfo 카카오 사용자 정보
     * @return 사용자 엔티티
     */
    private User findOrCreateUser(KakaoUserInfo kakaoUserInfo) {
        return userRepository.findByKakaoId(kakaoUserInfo.getId())
                .map(foundUser -> {
                    log.info("Found existing user: {}", foundUser.getId());
                    if (foundUser.isDeleted()) {
                        throw new IllegalStateException("이미 탈퇴한 사용자입니다.");
                    }
                    return foundUser;
                })
                .orElseGet(() -> {
                    log.info("Creating new user for Kakao id: {}", kakaoUserInfo.getId());
                    User newUser = User.builder()
                            .kakaoId(kakaoUserInfo.getId())
                            .nickname(kakaoUserInfo.getKakaoAccount().getProfile().getNickname())
                            .build();

                    User savedUser = userRepository.save(newUser);
                    log.info("New user created: {}", savedUser.getId());
                    return savedUser;
                });
    }
}