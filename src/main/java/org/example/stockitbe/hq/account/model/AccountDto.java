package org.example.stockitbe.hq.account.model;

import lombok.Builder;
import lombok.Getter;
import org.example.stockitbe.user.model.User;
import org.example.stockitbe.user.model.UserRole;
import org.example.stockitbe.user.model.UserStatus;

import java.time.LocalDateTime;

public class AccountDto {

    //  대기 회원 목록 응답
    @Getter
    @Builder
    public static class PendingRes {
        private Long id;
        private String employeeCode;
        private String name;
        private String email;
        private String phoneNumber;
        private String locationCode;
        private String locationName;
        private String applicationReason;
        private UserRole role;
        private UserStatus status;
        private LocalDateTime appliedAt;
        private LocalDateTime processedAt;

        public static PendingRes from(User entity) {
            return PendingRes.builder()
                    .id(entity.getId())
                    .employeeCode(entity.getEmployeeCode())
                    .name(entity.getName())
                    .email(entity.getEmail())
                    .phoneNumber(entity.getPhoneNumber())
                    .locationCode(entity.getLocationCode())
                    .locationName(entity.getLocationName())
                    .applicationReason(entity.getApplicationReason())
                    .role(entity.getRole())
                    .status(entity.getStatus())
                    .appliedAt(entity.getAppliedAt())
                    .processedAt(entity.getProcessedAt())
                    .build();
        }
    }

    //  처리(승인/거절) 결과 응답
    @Getter
    @Builder
    public static class ProcessRes {
        private Long id;
        private String employeeCode;
        private String name;
        private UserRole role;
        private UserStatus status;
        private LocalDateTime processedAt;

        public static ProcessRes from(User entity) {
            return ProcessRes.builder()
                    .id(entity.getId())
                    .employeeCode(entity.getEmployeeCode())
                    .name(entity.getName())
                    .role(entity.getRole())
                    .status(entity.getStatus())
                    .processedAt(entity.getProcessedAt())
                    .build();
        }
    }
}
