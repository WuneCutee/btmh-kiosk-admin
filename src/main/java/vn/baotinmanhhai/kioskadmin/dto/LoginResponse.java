package vn.baotinmanhhai.kioskadmin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String token;
    private String username;
    private long expiresInMs;
}
