package com.beyondtoursseoul.bts.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${frontend.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // JWT 기반 API 서버이므로 CSRF는 비활성화한다.
                .csrf(csrf -> csrf.disable())
                // 프론트와 백엔드 포트가 다를 때 브라우저 요청을 허용한다.
                .cors(Customizer.withDefaults())
                // 서버 세션을 사용하지 않고, 매 요청의 Bearer Token으로 인증한다.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. 공개 접근 허용 API
                        .requestMatchers("/api/v1/auth/signup", "/api/v1/auth/login", "/api/v1/auth/google").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // 2. 인증이 필수인 API (코스 저장, 조회 등)
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/courses/saved").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses/*/save").authenticated()

                        // 3. 그 외 (관광지 조회 등은 일단 허용하되, 인증 정보가 있으면 사용 가능)
                        .anyRequest().permitAll()
                )
                // Supabase가 발급한 JWT를 Resource Server 방식으로 검증한다.
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 로컬 Vue 개발 서버에서 오는 요청을 허용한다.
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
