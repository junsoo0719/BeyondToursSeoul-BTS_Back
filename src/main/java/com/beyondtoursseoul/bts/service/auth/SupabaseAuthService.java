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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
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

    // Google 로그인 완료 후 프론트가 다시 돌아올 callback URL이다.
    @Value("${FRONTEND_AUTH_CALLBACK_URL}")
    private String frontendAuthCallbackUrl;

    @Value("${frontend.allowed-origins:http://localhost:5173}")
    private String frontendAllowedOrigins;

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
    public URI getGoogleLoginUrl(String redirectTo, String origin, String referer) {
        return UriComponentsBuilder.fromUriString(supabaseUrl)
                .path("/auth/v1/authorize")
                .queryParam("provider", "google")
                .queryParam("redirect_to", resolveFrontendAuthCallbackUrl(redirectTo, origin, referer))
                .build()
                .encode()
                .toUri();
    }

    private URI resolveFrontendAuthCallbackUrl(String redirectTo, String origin, String referer) {
        if (isAllowedCallbackUrl(redirectTo)) {
            return URI.create(redirectTo);
        }

        String requestOrigin = extractOrigin(origin);
        if (requestOrigin == null) {
            requestOrigin = extractOrigin(referer);
        }

        if (requestOrigin != null && isAllowedOrigin(requestOrigin)) {
            return URI.create(requestOrigin + callbackPathAndQuery());
        }

        return URI.create(frontendAuthCallbackUrl);
    }

    private boolean isAllowedCallbackUrl(String callbackUrl) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            return false;
        }

        String callbackOrigin = extractOrigin(callbackUrl);
        return callbackOrigin != null && isAllowedOrigin(callbackOrigin);
    }

    private boolean isAllowedOrigin(String origin) {
        return allowedOrigins().contains(origin);
    }

    private Set<String> allowedOrigins() {
        Set<String> origins = new LinkedHashSet<>();
        Arrays.stream(frontendAllowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::stripTrailingSlash)
                .forEach(origins::add);

        String fallbackOrigin = extractOrigin(frontendAuthCallbackUrl);
        if (fallbackOrigin != null) {
            origins.add(fallbackOrigin);
        }

        return origins;
    }

    private String extractOrigin(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }

            String origin = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() != -1) {
                origin += ":" + uri.getPort();
            }
            return origin;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String callbackPathAndQuery() {
        URI fallbackUri = URI.create(frontendAuthCallbackUrl);
        String path = fallbackUri.getRawPath() == null || fallbackUri.getRawPath().isBlank()
                ? "/"
                : fallbackUri.getRawPath();

        if (fallbackUri.getRawQuery() != null && !fallbackUri.getRawQuery().isBlank()) {
            return path + "?" + fallbackUri.getRawQuery();
        }

        return path;
    }

    private String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
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

    @Override
    public MeResponse updateNickname(Jwt jwt, String nickname) {
        return updateProfile(jwt, nickname, null, null);
    }

    @Override
    public MeResponse updateProfile(Jwt jwt, String nickname, String localPreference, String preferredLanguage) {
        String userId = jwt.getSubject();
        UUID id = UUID.fromString(userId);
        Profile profile = profileRepository.findById(id)
                .orElseGet(() -> Profile.createForUser(id));

        if (nickname != null) {
            String normalized = nickname.trim();
            if (normalized.isBlank()) {
                throw new IllegalArgumentException("닉네임을 입력해 주세요.");
            }
            if (normalized.length() > 20) {
                throw new IllegalArgumentException("닉네임은 20자 이내로 입력해 주세요.");
            }
            profile.updateNickname(normalized);
        }

        if (localPreference != null) {
            String normalizedPref = localPreference.trim();
            if (!allowedLocalPreferences().contains(normalizedPref)) {
                throw new IllegalArgumentException("지원하지 않는 페르소나입니다.");
            }
            profile.updateLocalPreference(normalizedPref);
        }

        if (preferredLanguage != null) {
            String normalizedLang = preferredLanguage.trim();
            if (!allowedPreferredLanguages().contains(normalizedLang)) {
                throw new IllegalArgumentException("지원하지 않는 언어입니다.");
            }
            profile.updatePreferredLanguage(normalizedLang);
        }

        profileRepository.save(profile);
        return me(jwt);
    }

    private Set<String> allowedLocalPreferences() {
        return new HashSet<>(Arrays.asList(
                "main100",
                "main70",
                "balanced",
                "local70",
                "local100"
        ));
    }

    private Set<String> allowedPreferredLanguages() {
        return new HashSet<>(Arrays.asList(
                "한국어",
                "English",
                "日本語",
                "中文"
        ));
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
