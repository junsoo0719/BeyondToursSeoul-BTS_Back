package com.beyondtoursseoul.bts.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String JWT_SCHEME_NAME = "jwtAuth";

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Beyond Tours Seoul API")
                .description("서울을 여행하는 외국인들을 위한 관광 도우미 BTS API 명세서")
                .version("1.0.0");

        // JWT 인증 설정
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(JWT_SCHEME_NAME);
        Components components = new Components()
                .addSecuritySchemes(JWT_SCHEME_NAME, new SecurityScheme().name(JWT_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(info)
                .addServersItem(new Server().url("/")) // 앞에 url을 기준으로 요청
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
