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
    private Date refreshTokenExpiresIn;

    public static TokenDto of(String grantType, String accessToken, String refreshToken, Date accessTokenExpiresIn, Date refreshTokenExpiresIn) {
        TokenDto token = TokenDto.builder()
                .grantType(grantType)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(accessTokenExpiresIn)
                .refreshTokenExpiresIn(refreshTokenExpiresIn)
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