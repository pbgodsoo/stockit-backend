package org.example.stockitbe.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.notification.model.dto.NotificationDto;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "시스템 - 알림", description = "SSE 실시간 알림 구독/푸시 · 본인 수신 알림 목록 · 읽음 처리 API (SYS-004, SYS-005)")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    // SSE 구독
    @Operation(summary = "SSE 알림 스트림 구독",
            description = "본인 권한에 해당하는 알림을 실시간으로 수신합니다. 응답은 text/event-stream.")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AuthUserDetails me,
                                 HttpServletResponse response) {
        // SSE 실시간 전송 보장 — nginx/istio 등 프록시의 응답 버퍼링 비활성화.
        // X-Accel-Buffering: no → ingress-nginx 가 chunked response 를 즉시 클라이언트로 흘려보냄.
        // 없으면 nginx 가 데이터를 모아두려고 해서 heartbeat (:ping) 도달 지연 → 클라이언트 끊김 인식.
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        // 1. 요청 받기 2. 서비스 호출 3. text/event-stream 반환
        return service.subscribe(me);
    }

    // SSE 명시적 종료 — FE 가 페이지 unload (탭 닫기/F5/창 닫기) 시 navigator.sendBeacon 으로 호출.
    // TCP FIN 도달 못 하는 unload 상황에서도 BE Map 에서 즉시 정리되게 함.
    // sendBeacon 은 POST 만 지원 → DELETE 가 아닌 POST 채택.
    @Operation(summary = "SSE 세션 명시적 종료",
            description = "페이지 unload 시 sendBeacon으로 호출됩니다. sessionId의 owner만 종료 가능.")
    @PostMapping("/stream/{sessionId}/close")
    public BaseResponse<Void> closeStream(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "SSE 세션 UUID (구독 시 connect 이벤트로 받은 값)",
                    example = "0db77e3c-ac0e-479c-b2ec-92759ba1ecde")
            @PathVariable String sessionId) {
        // 1. 요청 받기 2. 서비스 호출 (내부에서 sessionId owner 검증) 3. 응답 반환
        service.unsubscribeStream(sessionId, me);
        return BaseResponse.success(null);
    }

    // 본인 수신 알림 목록
    @Operation(summary = "본인 수신 알림 목록 조회",
            description = "페이지네이션으로 본인 알림을 조회합니다. unreadOnly=true 이면 미확인만 반환.")
    @GetMapping
    public BaseResponse<NotificationDto.NotificationListRes> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "페이지 번호 (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "미확인만 조회 여부", example = "false")
            @RequestParam(required = false) Boolean unreadOnly) {
        // 1. 요청 받기 2. 서비스 호출 3. 응답 반환
        return BaseResponse.success(service.list(me, page, size, unreadOnly));
    }

    // 미읽음 카운트
    @Operation(summary = "미읽음 알림 개수 조회",
            description = "헤더 종 아이콘 미확인 카운트에 사용됩니다.")
    @GetMapping("/unread-count")
    public BaseResponse<NotificationDto.UnreadCountRes> unreadCount(
            @AuthenticationPrincipal AuthUserDetails me) {
        // 1. 요청 받기 2. 서비스 호출 3. 응답 반환
        return BaseResponse.success(service.unreadCount(me));
    }

    // 단건 읽음
    @Operation(summary = "단건 알림 읽음 처리",
            description = "알림 1건을 읽음 상태로 변경합니다. 같은 알림 두 번 호출해도 안전 (idempotent).")
    @PatchMapping("/{id}/read")
    public BaseResponse<Void> read(@AuthenticationPrincipal AuthUserDetails me,
                                   @Parameter(description = "알림 ID", example = "1")
                                   @PathVariable Long id) {
        // 1. 요청 받기 2. 서비스 호출 3. 응답 반환
        service.read(id, me);
        return BaseResponse.success(null);
    }

    // 본인 미읽음 전체 읽음
    @Operation(summary = "본인 미확인 알림 전체 읽음 처리",
            description = "본인의 미확인 알림을 일괄 읽음 처리합니다.")
    @PatchMapping("/read-all")
    public BaseResponse<Void> readAll(@AuthenticationPrincipal AuthUserDetails me) {
        // 1. 요청 받기 2. 서비스 호출 3. 응답 반환
        service.readAll(me);
        return BaseResponse.success(null);
    }
}
