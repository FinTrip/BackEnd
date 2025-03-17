package org.example.backend.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public class JwtAuthentication implements Authentication {
    private final String userEmail;
    private boolean authenticated = true;

    public JwtAuthentication(String userEmail) {
        this.userEmail = userEmail;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // Không có quyền cụ thể
    }

    @Override
    public Object getCredentials() {
        return null; // Không có thông tin xác thực cụ thể
    }

    @Override
    public Object getDetails() {
        return null; // Không có thông tin chi tiết
    }

    @Override
    public Object getPrincipal() {
        return userEmail; // Trả về email của người dùng
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated; // Trạng thái xác thực
    }

    @Override
    public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
        this.authenticated = authenticated; // Thiết lập trạng thái xác thực
    }

    @Override
    public String getName() {
        return userEmail; // Trả về tên người dùng
    }
} 