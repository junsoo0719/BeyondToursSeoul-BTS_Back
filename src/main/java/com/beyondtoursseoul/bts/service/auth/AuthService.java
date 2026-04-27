package com.beyondtoursseoul.bts.service.auth;

import com.beyondtoursseoul.bts.dto.auth.AuthResponse;
import com.beyondtoursseoul.bts.dto.auth.LoginRequest;
import com.beyondtoursseoul.bts.dto.auth.MeResponse;
import com.beyondtoursseoul.bts.dto.auth.SignupRequest;
import org.springframework.security.oauth2.jwt.Jwt;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    MeResponse me(Jwt jwt);
}
