package com.picktory.domain.auth.userdetails;

import com.picktory.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security에서 사용자 인증 정보를 담는 클래스
 * UserDetails 인터페이스를 구현하여 Security Context에서 인증 정보로 사용됨
 */
@Getter
public class PrincipalDetails implements UserDetails {

    private final User user;

    public PrincipalDetails(User user) {
        this.user = user;
    }

    /**
     * 사용자 ID 반환
     *
     * @return 사용자 ID
     */
    public Long getId() {
        return user.getId();
    }

    /**
     * 사용자의 권한 정보 반환
     *
     * @return 권한 정보 컬렉션
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 현재는 단순히 ROLE_USER 권한만 부여
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * 사용자 비밀번호 반환 (소셜 로그인이므로 null 반환)
     *
     * @return null (비밀번호 없음)
     */
    @Override
    public String getPassword() {
        return null; // 소셜 로그인 사용자는 비밀번호 없음
    }

    /**
     * 사용자 식별자 반환 (ID를 문자열로 변환)
     *
     * @return 사용자 ID 문자열
     */
    @Override
    public String getUsername() {
        return user.getId().toString();
    }

    /**
     * 계정 만료 여부 확인
     *
     * @return 계정 만료 여부 (만료되지 않았으면 true)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 검사 없음
    }

    /**
     * 계정 잠금 여부 확인
     *
     * @return 계정 잠금 여부 (잠기지 않았으면 true)
     */
    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠금 검사 없음
    }

    /**
     * 자격 증명 만료 여부 확인
     *
     * @return 자격 증명 만료 여부 (만료되지 않았으면 true)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 자격 증명 만료 검사 없음
    }

    /**
     * 계정 활성화 여부 확인
     *
     * @return 계정 활성화 여부 (활성화되었으면 true)
     */
    @Override
    public boolean isEnabled() {
        return !user.isDeleted(); // 삭제된 사용자는 비활성화
    }
}