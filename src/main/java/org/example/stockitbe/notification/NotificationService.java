package org.example.stockitbe.notification;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.notification.event.NotificationEvent;
import org.example.stockitbe.notification.model.dto.NotificationDto;
import org.example.stockitbe.notification.model.entity.Notification;
import org.example.stockitbe.notification.model.entity.NotificationRead;
import org.example.stockitbe.notification.repository.NotificationReadRepository;
import org.example.stockitbe.notification.repository.NotificationRepository;
import org.example.stockitbe.user.UserRepository;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.example.stockitbe.user.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;   // 개별 읽음 추적용 (B3 해결)
    private final NotificationSseService sseService;
    private final UserRepository userRepository;

    public SseEmitter subscribe(AuthUserDetails me) {
        User u = findUserOrThrow(me);
        return sseService.subscribe(u.getId(), u.getRole().name(), u.getLocationCode());
    }

    @Transactional(readOnly = true)
    public NotificationDto.NotificationListRes list(AuthUserDetails me, int page, int size, Boolean unreadOnly) {
        User u = findUserOrThrow(me);
        Date activatedAt = getActivatedAt(u);                                  // 본인 활성화 시점 — 권한 기반 알림 노출 기준선
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        // unreadOnly=true 면 NOT EXISTS 기반 쿼리로 페이지 크기 정확 보장 (V1 stream filter 의 버그 해결)
        Page<NotificationRepository.ReceivableRow> p = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findReceivableUnreadV2(
                        u.getId(), u.getRole().name(), u.getLocationCode(), activatedAt, pageable)
                : notificationRepository.findReceivableV2(
                        u.getId(), u.getRole().name(), u.getLocationCode(), activatedAt, pageable);

        // projection 의 readAt (본인 기준) 을 DTO 로 매핑 — null 이면 미읽음
        var items = p.getContent().stream()
                .map(row -> NotificationDto.NotificationRes.from(row.getN(), row.getReadAt()))
                .toList();

        return NotificationDto.NotificationListRes.builder()
                .items(items)
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .page(p.getNumber())
                .size(p.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public NotificationDto.UnreadCountRes unreadCount(AuthUserDetails me) {
        User u = findUserOrThrow(me);
        Date activatedAt = getActivatedAt(u);
        long count = notificationRepository.countUnreadReceivableV2(
                u.getId(), u.getRole().name(), u.getLocationCode(), activatedAt);
        return NotificationDto.UnreadCountRes.builder().count(count).build();
    }

    @Transactional
    public void read(Long id, AuthUserDetails me) {
        User u = findUserOrThrow(me);
        Notification n = findReceivableOrThrow(id, u);                         // 본인 수신분 검증 (broadcast + 직접 수신 모두 허용)

        // 권한 기반 (broadcast) 알림은 본인 활성화 시점 이전 발행분이면 접근 거부.
        // 직접 수신 (targetUserId == 본인) 은 시점 무관 (개인 알림은 시점 이후 발행밖에 없음).
        if (n.getTargetUserId() == null && n.getCreatedAt().before(getActivatedAt(u))) {
            throw BaseException.from(BaseResponseStatus.NOTIFICATION_FORBIDDEN);
        }

        // 이미 본인 읽음 행이 있으면 무시 — idempotent (한 알림 두 번 클릭 OK)
        if (notificationReadRepository.existsByNotificationIdAndUserId(id, u.getId())) {
            return;
        }
        notificationReadRepository.save(NotificationRead.builder()
                .notificationId(id)
                .userId(u.getId())
                .readAt(new Date())
                .build());
    }

    @Transactional
    public void readAll(AuthUserDetails me) {
        User u = findUserOrThrow(me);
        Date activatedAt = getActivatedAt(u);
        // 본인 수신분 중 아직 안 읽은 알림에 대해 notification_read 행 일괄 생성.
        // INSERT IGNORE 로 동시 호출/이미 읽은 알림에 안전.
        notificationRepository.markAllReceivableReadV2(
                u.getId(), u.getRole().name(), u.getLocationCode(), activatedAt, new Date());
    }

    /** 이벤트 리스너에서 호출 — 알림 1건 영속화 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification persist(NotificationEvent e) {
        return notificationRepository.save(Notification.builder()
                .type(e.getType())
                .severity(e.getSeverity())
                .title(e.getTitle())
                .message(e.getMessage())
                .targetRole(e.getTargetRole() != null ? e.getTargetRole().name() : null)
                .targetLocationCode(e.getTargetLocationCode())
                .targetUserId(e.getTargetUserId())
                .refType(e.getRefType())
                .refId(e.getRefId())
                .build());
    }

    // ───────────── private helper ─────────────

    // 사용하는 메서드: subscribe, list, unreadCount, read, readAll
    // 인증 정보(employeeCode)에서 User 엔티티 로드
    private User findUserOrThrow(AuthUserDetails me) {
        return userRepository.findByEmployeeCode(me.getEmployeeCode())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.USER_NOT_FOUND));
    }

    // 사용하는 메서드: read
    // 본인 수신분이 맞는지 검증 후 엔티티 반환
    private Notification findReceivableOrThrow(Long id, User u) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.NOTIFICATION_NOT_FOUND));
        boolean directly = n.getTargetUserId() != null && n.getTargetUserId().equals(u.getId());
        boolean byRole = n.getTargetRole() != null
                && n.getTargetRole().equals(u.getRole().name())
                && (n.getTargetLocationCode() == null
                || n.getTargetLocationCode().equals(u.getLocationCode()));
        if (!directly && !byRole) {
            throw BaseException.from(BaseResponseStatus.NOTIFICATION_FORBIDDEN);
        }
        return n;
    }

    // 사용하는 메서드: list, unreadCount, read, readAll
    // 권한 기반 알림에 한해 본인 활성화 시점 (processedAt) 이후 발행분만 노출하기 위한 기준선 변환.
    // User.processedAt 은 LocalDateTime — Notification.createDate (Date) 와 비교 위해 Date 로 변환.
    // null 인 경우 fallback: Date(0) — 모든 알림 보임 (시스템 초기 admin / 기존 데이터 호환)
    private Date getActivatedAt(User u) {
        return u.getProcessedAt() != null
                ? Date.from(u.getProcessedAt().atZone(ZoneId.systemDefault()).toInstant())
                : new Date(0);
    }
}
