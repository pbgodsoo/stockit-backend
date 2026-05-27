package org.example.stockitbe.hq.account.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.stockitbe.user.model.entity.User;
import org.example.stockitbe.user.model.entity.UserRole;
import org.example.stockitbe.user.model.entity.UserStatus;

import java.time.LocalDateTime;

public class AccountDto {

    //  대기 회원 목록 응답
    @Getter
    @Builder
    @Schema(description = "회원 정보 응답 (대기/전체 목록 공용)")
    public static class PendingRes {
        @Schema(description = "회원 PK", example = "5")
        private Long id;
        @Schema(description = "사원코드 (PENDING은 null)", example = "st0001")
        private String employeeCode;
        @Schema(description = "이름", example = "테스트유저")
        private String name;
        @Schema(description = "이메일", example = "swagger-test01@stockit.com")
        private String email;
        @Schema(description = "전화번호", example = "01099990001")
        private String phoneNumber;
        @Schema(description = "지점 코드", example = "ST001")
        private String locationCode;
        @Schema(description = "지점명", example = "테스트 매장")
        private String locationName;
        @Schema(description = "가입 신청 사유", example = "Swagger 테스트용 신청")
        private String applicationReason;
        @Schema(description = "권한 (HQ/STORE/WAREHOUSE)", example = "STORE")
        private UserRole role;
        @Schema(description = "회원 상태", example = "PENDING",
                allowableValues = {"PENDING", "APPROVED", "REJECTED", "WITHDRAWN"})
        private UserStatus status;
        @Schema(description = "신청 일시")
        private LocalDateTime appliedAt;
        @Schema(description = "처리 일시 (PENDING은 null)")
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
    @Schema(description = "회원 처리 결과 응답 (승인/거절/탈퇴 공용)")
    public static class ProcessRes {
        @Schema(description = "회원 PK", example = "5")
        private Long id;
        @Schema(description = "발급된 사원코드", example = "st0001")
        private String employeeCode;
        @Schema(description = "이름", example = "테스트유저")
        private String name;
        @Schema(description = "권한", example = "STORE")
        private UserRole role;
        @Schema(description = "변경된 상태", example = "APPROVED")
        private UserStatus status;
        @Schema(description = "처리 일시")
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
