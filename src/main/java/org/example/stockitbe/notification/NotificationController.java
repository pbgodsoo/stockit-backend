package org.example.stockitbe.notification;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.notification.model.dto.NotificationDto;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    // SSE 구독
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AuthUserDetails me) {
        // 1. 요청 받기 2. 서비스 호출 3. text/event-stream 반환
        return service.subscribe(me);
    }

    // SSE 명시적 종료 — FE 가 페이지 unload (탭 닫기/F5/창 닫기) 시 navigator.sendBeacon 으로 호출.
    // TCP FIN 도달 못 하는 unload 상황에서도 BE Map 에서 즉시 정리되게 함.
    // sendBeacon 은 POST 만 지원 → DELETE 가 아닌 POST 채택.
    @PostMapping("/stream/{sessionId}/close")
    public BaseResponse<Void> closeStream(
            @AuthenticationPrincipal AuthUserDetails me,
            @PathVariable String sessionId) {
        // 1. 요청 받기 2. 서비스 호출 (내부에서 sessionId owner 검증) 3. 응답 반환
        service.unsubscribeStream(sessionId, me);
        return BaseResponse.success(null);
    }

    // 본인 수신 알림 목록
    @GetMapping
    public BaseResponse<NotificationDto.NotificationListRes> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly) {
        // 1. 요청 받기 2. 서비스 호출 3. 응답 반환
        return BaseResponse.success(service.list(me, page, size, unreadOnly));
    }

    // 미읽음 카운트
    @GetMapping("/unread-count")
    public BaseResponse<NotificationDto.UnreadCountRes> unreadCount(
            @AuthenticationPrincipal AuthUserDetails me) {
        // 1. 요청 받기 2. 서비스 호출 3. 응답 반환
        return BaseResponse.success(service.unreadCount(me));
    }

    // 단건 읽음
    @PatchMapping("/{id}/read")
    public BaseResponse<Void> read(@AuthenticationPrincipal AuthUserDetails me,
                                   @PathVariable Long id) {
        // 1. 요청 받기 2. 서비스 호출 3. 응답 반환
        service.read(id, me);
        return BaseResponse.success(null);
    }

    // 본인 미읽음 전체 읽음
    @PatchMapping("/read-all")
    public BaseResponse<Void> readAll(@AuthenticationPrincipal AuthUserDetails me) {
        // 1. 요청 받기 2. 서비스 호출 3. 응답 반환
        service.readAll(me);
        return BaseResponse.success(null);
    }
}
