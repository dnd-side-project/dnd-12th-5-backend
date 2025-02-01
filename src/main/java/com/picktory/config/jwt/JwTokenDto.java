package com.picktory.config.jwt;

import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Date;

@Getter
public class JwTokenDto {
    private String grantType;
    private String accessToken;
    private String refreshToken;
    private Date accessTokenExpiresIn;

    public JwTokenDto(String grantType, String accessToken, String refreshToken, Date accessTokenExpiresIn){
        validateTokenData(accessToken,refreshToken);
        this.grantType = grantType;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresIn = accessTokenExpiresIn;
    }

    private void validateTokenData(String accessToken, String refreshToken) {
        if (StringUtils.isEmpty(accessToken)) {
            throw new IllegalArgumentException("Access Token cannot be empty");
        }
        if (StringUtils.isEmpty(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token cannot be empty");
        }
    }
}
