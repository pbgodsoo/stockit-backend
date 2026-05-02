package org.example.stockitbe.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, updatable = false)
    private String employeeCode;   // 로그인용 사원코드

    @Column(nullable = false)
    private String password;    // 로그인 비밀번호

    @Column(nullable = false)
    private String name;        // 회원가입용 이름

    @Column(nullable = false)
    private String email;       // 회원가입용 이메일

    @Column(nullable = false)
    private String phoneNumber;  // 회원가입용 전화번호

    @Column(nullable = false)
    private String locationCode;  // 회원가입용 지점 코드

    @Column(nullable = false)
    private String locationName;  // 회원가입용 지점명

    private String applicationReason; // 신청 사유(가입 신청 시 입력)

    @Enumerated(EnumType.STRING)
    private UserRole role;                  // 권한

    @Enumerated(EnumType.STRING)
    private UserStatus status;    // 회원가입 시 상태(대기, 승인, 거절)

    private LocalDateTime appliedAt;  // 회원가입 신청일시

    private LocalDateTime processedAt; // 본사 관리자의 회원가입 처리일시

}
