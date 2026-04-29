package com.beyondtoursseoul.bts.service.auth;

import com.beyondtoursseoul.bts.domain.Profile;
import com.beyondtoursseoul.bts.dto.auth.AuthResponse;
import com.beyondtoursseoul.bts.dto.auth.LoginRequest;
import com.beyondtoursseoul.bts.dto.auth.MeResponse;
import com.beyondtoursseoul.bts.dto.auth.SignupRequest;
import com.beyondtoursseoul.bts.repository.ProfileRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupabaseAuthService implements AuthService {

    private final ProfileRepository profileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    @Value("${SUPABASE_URL}")
    private String supabaseUrl;

    @Value("${SUPABASE_PUBLISHABLE_KEY}")
    private String publishableKey;

//
    @Override
    public AuthResponse signup(SignupRequest request) {
        String rawResponse = restClient.post()
                .uri(supabaseUrl + "/auth/v1/signup")
                .header("apikey", publishableKey)
                .header("Authorization", "Bearer " + publishableKey)
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(rawResponse);

            if (root.isMissingNode() || root.isNull()) {
                throw new IllegalStateException("회원가입 응답이 올바르지 않습니다.");
            }

            String accessToken = root.path("access_token").isMissingNode() || root.path("access_token").isNull()
                    ? null
                    : root.path("access_token").asText();

            String refreshToken = root.path("refresh_token").isMissingNode() || root.path("refresh_token").isNull()
                    ? null
                    : root.path("refresh_token").asText();

            String tokenType = root.path("token_type").isMissingNode() || root.path("token_type").isNull()
                    ? null
                    : root.path("token_type").asText();

            Long expiresIn = root.path("expires_in").isMissingNode() || root.path("expires_in").isNull()
                    ? null
                    : root.path("expires_in").asLong();

            String userId = root.path("id").asText(null);
            String email = root.path("email").asText(null);

            String message = accessToken == null
                    ? "회원가입 완료. 이메일 인증 후 로그인하세요."
                    : "회원가입 및 로그인 완료";

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    tokenType,
                    expiresIn,
                    userId,
                    email,
                    message
            );
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("회원가입 응답 파싱에 실패했습니다.", e);
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        SupabaseAuthResult result = restClient.post()
                .uri(supabaseUrl + "/auth/v1/token?grant_type=password")
                .header("apikey", publishableKey)
                .header("Authorization", "Bearer " + publishableKey)
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve().body(SupabaseAuthResult.class);

        if (result == null || result.getUser() == null) {
            throw new IllegalStateException("로그인 응답이 올바르지 않습니다.");
        }

        return new AuthResponse(
                result.getAccessToken(),
                result.getRefreshToken(),
                result.getTokenType(),
                result.getExpiresIn(),
                result.getUser().getId(),
                result.getUser().getEmail(),
                "로그인 성공"
        );
    }

    @Override
    public MeResponse me(Jwt jwt) {
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String role = jwt.getClaimAsString("role");

        Profile profile = profileRepository.findById(UUID.fromString(userId)).orElse(null);

        return new MeResponse(
                userId,
                email,
                role,
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getPreferredLanguage() : null,
                profile != null ? profile.getVisitCount() : null,
                profile != null ? profile.getLocalPreference() : null
        );
    }

    @Getter
    @NoArgsConstructor
    private static class SupabaseAuthResult {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private Long expiresIn;

        private SupabaseUser user;
    }

    @Getter
    @NoArgsConstructor
    private static class SupabaseUser {
        private String id;
        private String email;
    }
}
