package com.beyondtoursseoul.bts.config;

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
                        // 회원가입/로그인 API는 인증 없이 접근 가능해야 한다.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/signup")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/google")
                        .permitAll()
                        // 서버 상태 확인용 경로는 공개한다.
                        .requestMatchers(HttpMethod.GET, "/health")
                        .permitAll()
                        // 현재 로그인 사용자 확인 API는 JWT 인증이 필요하다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me")
                        .authenticated()
                        // swagger 경로 허용
                        .requestMatchers(
                                HttpMethod.GET,
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // 아직 분류하지 않은 기존 API는 임시로 열어둔다.
                        .anyRequest()
                        .permitAll()
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
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://beyond-tours-seoul-bts-front.vercel.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
