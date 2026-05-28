package org.example.stockitbe.common.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공개 헬스 - 버전", description = "비로그인 헬스체크용 — 현재 배포 API 버전 식별자 반환")
@RestController
@RequestMapping("/api/public")
public class VersionController {

    @Operation(
            summary = "API 버전 조회",
            description = "현재 배포된 API 의 버전 문자열을 반환한다. 비로그인 호출 가능 (헬스체크·블루그린 라우팅 검증용).",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "현재 API 버전",
                    content = @Content(
                            mediaType = "text/plain;charset=UTF-8",
                            schema = @Schema(type = "string", example = "v1"),
                            examples = @ExampleObject(value = "v1")
                    )
            )
    )
    @GetMapping(value = "/version", produces = "text/plain;charset=UTF-8")
    public String version() {
        return "v1";
    }
}
