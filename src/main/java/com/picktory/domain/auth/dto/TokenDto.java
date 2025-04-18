package com.picktory.domain.auth.dto;

import com.picktory.common.exception.BaseException;
import com.picktory.common.BaseResponseStatus;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Date;

@Getter
@Builder
public class TokenDto {
    private String grantType;
    private String accessToken;
    private String refreshToken;
    private Date accessTokenExpiresIn;

    public static TokenDto of(String grantType, String accessToken, String refreshToken, Date expiresIn) {
        TokenDto token = TokenDto.builder()
                .grantType(grantType)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(expiresIn)
                .build();

        token.validate();
        return token;
    }

    public void validate() {
        if (!StringUtils.hasText(accessToken)) {
            throw new BaseException(BaseResponseStatus.INVALID_ACCESS_TOKEN);
        }
        if (!StringUtils.hasText(refreshToken)) {
            throw new BaseException(BaseResponseStatus.INVALID_REFRESH_TOKEN);
        }
    }
}