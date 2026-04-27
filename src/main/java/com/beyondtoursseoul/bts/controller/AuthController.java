package com.beyondtoursseoul.bts.controller;

import com.beyondtoursseoul.bts.dto.auth.AuthResponse;
import com.beyondtoursseoul.bts.dto.auth.LoginRequest;
import com.beyondtoursseoul.bts.dto.auth.MeResponse;
import com.beyondtoursseoul.bts.dto.auth.SignupRequest;
import com.beyondtoursseoul.bts.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public AuthResponse signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(jwt);
    }
}
