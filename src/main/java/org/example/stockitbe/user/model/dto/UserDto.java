package org.example.stockitbe.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.stockitbe.user.model.entity.User;
import org.example.stockitbe.user.model.entity.UserRole;
import org.example.stockitbe.user.model.entity.UserStatus;

import java.time.LocalDateTime;

public class UserDto {

    // 회원가입 신청 요청
    @Getter
    @NoArgsConstructor
    @Schema(description = "회원가입 신청 요청")
    public static class SignupReq {
        @Schema(description = "이름", example = "테스트유저", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @Schema(description = "이메일 (영문+숫자, 중복 불가)", example = "swagger-test01@stockit.com", requiredMode = Schema.RequiredMode.REQUIRED)
        private String email;

        @Schema(description = "비밀번호 (대소문자+숫자+특수문자 8자 이상)", example = "Stockit!2026", requiredMode = Schema.RequiredMode.REQUIRED)
        private String password;

        @Schema(description = "전화번호 (하이픈 없이 010+8자리)", example = "01099990001", requiredMode = Schema.RequiredMode.REQUIRED)
        private String phoneNumber;

        @Schema(description = "지점 코드", example = "ST001", requiredMode = Schema.RequiredMode.REQUIRED)
        private String locationCode;

        @Schema(description = "지점명", example = "테스트 매장", requiredMode = Schema.RequiredMode.REQUIRED)
        private String locationName;

        @Schema(description = "가입 신청 사유 (선택)", example = "Swagger 테스트용 신청")
        private String applicationReason;

        @Schema(description = "권한", example = "STORE", allowableValues = {"HQ", "STORE", "WAREHOUSE"})
        private UserRole role;

        public User toEntity(String encodedPassword) {
            return User.builder()
                    .employeeCode(null)
                    .password(encodedPassword)
                    .name(name)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .locationCode(locationCode)
                    .locationName(locationName)
                    .applicationReason(applicationReason)
                    .role(role)
                    .status(UserStatus.PENDING)
                    .appliedAt(LocalDateTime.now())
                    .processedAt(null)
                    .build();
        }
    }

    // 회원 정보 응답
    @Getter
    @Builder
    @Schema(description = "회원가입 응답")
    public static class SignupRes {
        @Schema(description = "회원 PK", example = "5")
        private Long id;

        @Schema(description = "사원코드 (승인 전 null)", example = "null")
        private String employeeCode;

        @Schema(description = "이름", example = "테스트유저")
        private String name;

        @Schema(description = "이메일", example = "test01@stockit.com")
        private String email;

        @Schema(description = "지점 코드", example = "ST001")
        private String locationCode;

        @Schema(description = "지점명", example = "테스트 매장")
        private String locationName;

        @Schema(description = "권한", example = "STORE")
        private UserRole role;

        @Schema(description = "회원 상태", example = "PENDING")
        private UserStatus userStatus;

        @Schema(description = "신청 일시", example = "2026-05-27T09:00:00.000+09:00")
        private LocalDateTime appliedAt;

        public static SignupRes from(User entity) {
            return SignupRes.builder()
                    .id(entity.getId())
                    .employeeCode(entity.getEmployeeCode())
                    .name(entity.getName())
                    .email(entity.getEmail())
                    .locationCode(entity.getLocationCode())
                    .locationName(entity.getLocationName())
                    .role(entity.getRole())
                    .userStatus(entity.getStatus())
                    .appliedAt(entity.getAppliedAt())
                    .build();
        }
    }

    //  로그인 요청
    @Getter
    @NoArgsConstructor
    @Schema(description = "로그인 요청")
    public static class LoginReq {
        @Schema(description = "사원코드 (본사: hq0001 / 매장: st0001 / 창고: wh0001)", example = "hq0001", requiredMode = Schema.RequiredMode.REQUIRED)
        private String employeeCode;

        @Schema(description = "비밀번호", example = "Stockit!2026", requiredMode = Schema.RequiredMode.REQUIRED)
        private String password;
    }

    //  로그인 응답
    @Getter
    @Builder
    @Schema(description = "로그인 응답")
    public static class LoginRes {
        @Schema(description = "사원코드", example = "hq0001")
        private String employeeCode;

        @Schema(description = "이름", example = "홍길동")
        private String name;

        @Schema(description = "권한", example = "HQ")
        private UserRole role;

        @Schema(description = "지점 코드", example = "HQ001")
        private String locationCode;

        @Schema(description = "지점명", example = "본사")
        private String locationName;
        /** Swagger UI 테스트용 — 이 값을 복사해 'Authorize' 버튼에 입력하세요. 브라우저는 쿠키를 사용합니다. */
        private String accessToken;
    }

    //  마이페이지
    @Getter
    @Builder
    @Schema(description = "마이페이지 응답")
    public static class MypageRes {
        @Schema(description = "회원 PK", example = "1")
        private Long id;

        @Schema(description = "사원코드", example = "hq0001")
        private String employeeCode;

        @Schema(description = "이름", example = "홍길동")
        private String name;

        @Schema(description = "이메일", example = "hong@stockit.com")
        private String email;

        @Schema(description = "지점 코드", example = "HQ001")
        private String locationCode;

        @Schema(description = "지점명", example = "본사")
        private String locationName;

        @Schema(description = "권한", example = "HQ")
        private UserRole role;

        @Schema(description = "회원 상태", example = "APPROVED")
        private UserStatus status;

        @Schema(description = "전화번호", example = "01012345678")
        private String phoneNumber;

        public static MypageRes from(User entity) {
            return MypageRes.builder()
                    .id(entity.getId())
                    .employeeCode(entity.getEmployeeCode())
                    .name(entity.getName())
                    .email(entity.getEmail())
                    .locationCode(entity.getLocationCode())
                    .locationName(entity.getLocationName())
                    .role(entity.getRole())
                    .status(entity.getStatus())
                    .phoneNumber(entity.getPhoneNumber())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "전화번호 변경 요청")
    public static class UpdatePhoneReq {
        @Schema(description = "새 전화번호 (하이픈 없이 010+8자리)", example = "01098765432", requiredMode = Schema.RequiredMode.REQUIRED)
        private String phoneNumber;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "비밀번호 변경 요청")
    public static class UpdatePasswordReq {
        @Schema(description = "현재 비밀번호", example = "Stockit!2026", requiredMode = Schema.RequiredMode.REQUIRED)
        private String currentPassword;

        @Schema(description = "새 비밀번호 (대소문자+숫자+특수문자 8자 이상)", example = "NewPass1234!", requiredMode = Schema.RequiredMode.REQUIRED)
        private String newPassword;
    }


}
