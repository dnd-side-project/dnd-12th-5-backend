package com.picktory.config.jwt;

import lombok.Getter;

import java.util.Date;

@Getter
public class TokenDto {
    private String grantType;
    private String accessToken;
    private String refreshToken;
    private Date accessTokenExpiresIn;
}
