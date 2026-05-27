package org.example.stockitbe.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String COOKIE_AUTH = "AtokenCookie";

    @Bean
    public OpenAPI stockitOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stockit API")
                        .description("Stockit 백엔드 API 문서 — 사용자 인증 · 알림 · 계정 관리")
                        .version("v1.0"))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("Atoken")
                                .description("로그인 후 발급된 Atoken 쿠키 값을 입력하세요.")
                        )
                );
    }
}
