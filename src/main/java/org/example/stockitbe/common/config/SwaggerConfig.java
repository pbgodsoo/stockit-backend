package org.example.stockitbe.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String JWT_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stockit API")
                        .description("Stockit 재고 관리 시스템 REST API. Swagger 테스트는 로그인 응답의 accessToken을 BearerAuth에 입력해 실행합니다.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(JWT_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(JWT_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("0. 전체 API")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("1. 사용자 인증")
                .pathsToMatch("/api/user/**", "/api/public/**")
                .build();
    }

    @Bean
    public GroupedOpenApi storeApi() {
        return GroupedOpenApi.builder()
                .group("2. 매장")
                .pathsToMatch("/api/store/**")
                .build();
    }

    @Bean
    public GroupedOpenApi hqApi() {
        return GroupedOpenApi.builder()
                .group("3. 본사 (HQ)")
                .pathsToMatch(
                        "/api/hq/**",
                        "/api/vendors", "/api/vendors/**",
                        "/api/vendor-products", "/api/vendor-products/**",
                        "/api/notifications", "/api/notifications/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi warehouseApi() {
        return GroupedOpenApi.builder()
                .group("4. 창고")
                .pathsToMatch("/api/warehouse/**")
                .build();
    }
}
