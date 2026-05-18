package org.example.stockitbe.notification.repository;

import org.example.stockitbe.notification.model.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        SELECT n FROM Notification n
        WHERE n.targetUserId = :userId
           OR (n.targetRole = :role
               AND (n.targetLocationCode IS NULL OR n.targetLocationCode = :locationCode))
        ORDER BY n.id DESC
    """)
    Page<Notification> findReceivable(@Param("userId") Long userId,
                                      @Param("role") String role,
                                      @Param("locationCode") String locationCode,
                                      Pageable pageable);

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.read = false
          AND (n.targetUserId = :userId
               OR (n.targetRole = :role
                   AND (n.targetLocationCode IS NULL OR n.targetLocationCode = :locationCode)))
    """)
    long countUnreadReceivable(@Param("userId") Long userId,
                               @Param("role") String role,
                               @Param("locationCode") String locationCode);

    @Modifying
    @Query("""
        UPDATE Notification n SET n.read = true, n.readAt = :now
        WHERE n.read = false
          AND (n.targetUserId = :userId
               OR (n.targetRole = :role
                   AND (n.targetLocationCode IS NULL OR n.targetLocationCode = :locationCode)))
    """)
    int markAllReceivableRead(@Param("userId") Long userId,
                              @Param("role") String role,
                              @Param("locationCode") String locationCode,
                              @Param("now") Date now);
}