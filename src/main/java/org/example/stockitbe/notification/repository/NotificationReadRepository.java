package org.example.stockitbe.notification.repository;

import org.example.stockitbe.notification.model.entity.NotificationRead;
import org.example.stockitbe.notification.model.entity.NotificationReadId;
import org.springframework.data.jpa.repository.JpaRepository;

// 사용자별 알림 읽음 매핑 Repository.
// 단건 읽음 처리 시 중복 INSERT 방지용 exists 체크 메서드 제공.
// 전체 읽음 / 미읽음 카운트 / 목록 조회는 NotificationRepository 에서 JOIN 으로 처리.
public interface NotificationReadRepository
        extends JpaRepository<NotificationRead, NotificationReadId> {

    boolean existsByNotificationIdAndUserId(Long notificationId, Long userId);
}
