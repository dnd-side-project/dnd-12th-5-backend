package com.picktory.domain.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoUserInfo {
    /**
     * 카카오 사용자 ID
     */
    private Long id;

    /**
     * 카카오 계정 정보
     */
    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    /**
     * 카카오 계정 정보를 담는 내부 클래스
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KakaoAccount {
        /**
         * 프로필 정보
         */
        private Profile profile;

        /**
         * 프로필 정보를 담는 내부 클래스
         */
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Profile {
            /**
             * 사용자 닉네임
             */
            private String nickname;
        }
    }
}