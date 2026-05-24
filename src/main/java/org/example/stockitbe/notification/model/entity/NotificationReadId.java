package org.example.stockitbe.notification.model.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// NotificationRead 의 복합 PK 클래스 — (notification_id, user_id)
// @IdClass 사용 시 PK 가 여러 컬럼이면 동일 필드명/타입을 갖는 별도 클래스 필요.
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NotificationReadId implements Serializable {
    private Long notificationId;
    private Long userId;
}
