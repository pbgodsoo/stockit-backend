package org.example.stockitbe.notification;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.notification.event.NotificationEvent;
import org.example.stockitbe.notification.model.dto.NotificationDto;
import org.example.stockitbe.notification.model.entity.Notification;
import org.example.stockitbe.notification.repository.NotificationRepository;
import org.example.stockitbe.user.UserRepository;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.example.stockitbe.user.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.transaction.annotation.Propagation;


import java.util.Date;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSseService sseService;
    private final UserRepository userRepository;

    public SseEmitter subscribe(AuthUserDetails me) {
        User u = findUserOrThrow(me);
        return sseService.subscribe(u.getId(), u.getRole().name(), u.getLocationCode());
    }

    @Transactional(readOnly = true)
    public NotificationDto.NotificationListRes list(AuthUserDetails me, int page, int size, Boolean unreadOnly) {
        User u = findUserOrThrow(me);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Notification> p = notificationRepository.findReceivable(
                u.getId(), u.getRole().name(), u.getLocationCode(), pageable);

        var items = p.getContent().stream()
                .filter(n -> !Boolean.TRUE.equals(unreadOnly) || !n.isRead())
                .map(NotificationDto.NotificationRes::from)
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
        long count = notificationRepository.countUnreadReceivable(
                u.getId(), u.getRole().name(), u.getLocationCode());
        return NotificationDto.UnreadCountRes.builder().count(count).build();
    }

    @Transactional
    public void read(Long id, AuthUserDetails me) {
        User u = findUserOrThrow(me);
        Notification n = findReceivableOrThrow(id, u);
        if (n.isRead()) return;
        n.markAsRead(new Date());
    }

    @Transactional
    public void readAll(AuthUserDetails me) {
        User u = findUserOrThrow(me);
        notificationRepository.markAllReceivableRead(
                u.getId(), u.getRole().name(), u.getLocationCode(), new Date());
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
}