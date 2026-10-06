package vn.baotinmanhhai.kioskadmin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.dto.LoginRequest;
import vn.baotinmanhhai.kioskadmin.dto.LoginResponse;
import vn.baotinmanhhai.kioskadmin.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
