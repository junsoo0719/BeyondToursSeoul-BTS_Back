package com.beyondtoursseoul.bts.controller;

import com.beyondtoursseoul.bts.dto.auth.AuthResponse;
import com.beyondtoursseoul.bts.dto.auth.LoginRequest;
import com.beyondtoursseoul.bts.dto.auth.MeResponse;
import com.beyondtoursseoul.bts.dto.auth.SignupRequest;
import com.beyondtoursseoul.bts.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "회원가입, 로그인, Google 로그인 시작, 현재 사용자 조회 API")
public class AuthController {
    private final AuthService authService;

    @Operation(
            summary = "이메일 회원가입",
            description = "이메일과 비밀번호로 사용자를 회원가입시킵니다. 이메일 인증 설정에 따라 토큰 없이 가입 안내 메시지를 반환할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "429", description = "이메일 발송 제한 초과"),
            @ApiResponse(responseCode = "500", description = "회원가입 응답 처리 실패")
    })
    @PostMapping("/signup")
    public AuthResponse signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @Operation(
            summary = "이메일 로그인",
            description = "이메일과 비밀번호로 로그인하고 access token을 발급합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이메일 인증 미완료"),
            @ApiResponse(responseCode = "500", description = "로그인 처리 실패")
    })
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(
            summary = "Google 로그인 시작",
            description = "백엔드가 Supabase Google OAuth 시작 URL로 redirect 합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Google 로그인 페이지로 redirect"),
            @ApiResponse(responseCode = "500", description = "Google 로그인 시작 URL 생성 실패")
    })
    @GetMapping("/google")
    public ResponseEntity<Void> googleLogin() {
        URI googleLoginUrl = authService.getGoogleLoginUrl();

        return ResponseEntity.status(302)
                .location(googleLoginUrl)
                .build();
    }

    @Operation(
            summary = "현재 사용자 정보 조회",
            description = "JWT를 기반으로 현재 로그인한 사용자의 정보와 profiles 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "현재 사용자 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰 또는 인증 실패")
    })
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(jwt);
    }
}
