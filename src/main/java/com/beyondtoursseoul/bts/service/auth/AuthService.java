package com.beyondtoursseoul.bts.service.auth;

import com.beyondtoursseoul.bts.dto.auth.AuthResponse;
import com.beyondtoursseoul.bts.dto.auth.LoginRequest;
import com.beyondtoursseoul.bts.dto.auth.MeResponse;
import com.beyondtoursseoul.bts.dto.auth.SignupRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    URI getGoogleLoginUrl(String redirectTo, String origin, String referer);

    MeResponse me(Jwt jwt);

    MeResponse updateNickname(Jwt jwt, String nickname);

    MeResponse updateProfile(Jwt jwt, String nickname, String localPreference, String preferredLanguage);
}
