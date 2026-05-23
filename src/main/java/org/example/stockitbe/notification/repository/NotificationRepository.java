package org.example.stockitbe.notification.repository;

import org.example.stockitbe.notification.model.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

// 알림 본문 + 사용자별 읽음 상태 조회/처리 Repository.
// 개별 읽음 추적 (notification_read 매핑 테이블) 기반.
//  - LEFT JOIN 으로 본인 readAt 동반 반환 (projection)
//  - 권한 기반 알림은 본인 processedAt(activatedAt) 이후 발행분만 노출
//  - 직접 수신 (targetUserId = me) 은 activatedAt 조건 무시 (개인 알림)
//  - 전체 읽음은 INSERT IGNORE 로 idempotent
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // projection — Notification 본문 + 본인 readAt (null 이면 미읽음).
    interface ReceivableRow {
        Notification getN();
        Date getReadAt();
    }

    // 본인 수신분 전체 (읽음/미읽음 모두) + 본인 readAt 동반.
    // unreadOnly=true 인 경우는 findReceivableUnreadV2 사용 (페이지 크기 정확).
    @Query("""
        SELECT n AS n, r.readAt AS readAt
        FROM Notification n
        LEFT JOIN NotificationRead r
               ON r.notificationId = n.id AND r.userId = :userId
        WHERE n.targetUserId = :userId
           OR (n.targetRole = :role
               AND (n.targetLocationCode IS NULL OR n.targetLocationCode = :locationCode)
               AND n.createdAt >= :activatedAt)
        ORDER BY n.id DESC
    """)
    Page<ReceivableRow> findReceivableV2(@Param("userId") Long userId,
                                        @Param("role") String role,
                                        @Param("locationCode") String locationCode,
                                        @Param("activatedAt") Date activatedAt,
                                        Pageable pageable);

    // 본인 수신분 중 미읽음만. LEFT JOIN + IS NULL 패턴으로 페이지 크기 정확 보장.
    @Query("""
        SELECT n AS n, r.readAt AS readAt
        FROM Notification n
        LEFT JOIN NotificationRead r
               ON r.notificationId = n.id AND r.userId = :userId
        WHERE r.notificationId IS NULL
          AND (n.targetUserId = :userId
               OR (n.targetRole = :role
                   AND (n.targetLocationCode IS NULL OR n.targetLocationCode = :locationCode)
                   AND n.createdAt >= :activatedAt))
        ORDER BY n.id DESC
    """)
    Page<ReceivableRow> findReceivableUnreadV2(@Param("userId") Long userId,
                                               @Param("role") String role,
                                               @Param("locationCode") String locationCode,
                                               @Param("activatedAt") Date activatedAt,
                                               Pageable pageable);

    // 미읽음 카운트. NOT EXISTS 로 본인 읽음 기록이 없는 알림 세기.
    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE (n.targetUserId = :userId
               OR (n.targetRole = :role
                   AND (n.targetLocationCode IS NULL OR n.targetLocationCode = :locationCode)
                   AND n.createdAt >= :activatedAt))
          AND NOT EXISTS (
              SELECT 1 FROM NotificationRead r
              WHERE r.notificationId = n.id AND r.userId = :userId)
    """)
    long countUnreadReceivableV2(@Param("userId") Long userId,
                                 @Param("role") String role,
                                 @Param("locationCode") String locationCode,
                                 @Param("activatedAt") Date activatedAt);

    // 전체 읽음 — 본인 수신분 중 아직 안 읽은 알림에 대해 notification_read 행 일괄 생성.
    // INSERT IGNORE: PK 충돌 시 무시 (동시 호출/중복 호출에 안전).
    // MariaDB 전용 구문 — native query.
    @Modifying
    @Query(value = """
        INSERT IGNORE INTO notification_read (notification_id, user_id, read_at)
        SELECT n.id, :userId, :now
        FROM notification n
        WHERE (n.target_user_id = :userId
               OR (n.target_role = :role
                   AND (n.target_location_code IS NULL OR n.target_location_code = :locationCode)
                   AND n.create_date >= :activatedAt))
          AND NOT EXISTS (
              SELECT 1 FROM notification_read r
              WHERE r.notification_id = n.id AND r.user_id = :userId)
    """, nativeQuery = true)
    int markAllReceivableReadV2(@Param("userId") Long userId,
                                @Param("role") String role,
                                @Param("locationCode") String locationCode,
                                @Param("activatedAt") Date activatedAt,
                                @Param("now") Date now);
}
