package org.example.stockitbe.user.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    PENDING("대기"),
    APPROVED("승인 완료"),
    REJECTED("거절"),
    WITHDRAWN("탈퇴");

    private final String description;
}
