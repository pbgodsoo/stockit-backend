package org.example.stockitbe.user.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserDto {

    // 회원가입 신청 요청
    @Getter
    @NoArgsConstructor
    public static class SignupReq {
        private String name;
        private String email;
        private String password;
        private String phoneNumber;
        private String locationCode;
        private String locationName;
        private String applicationReason;
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
    public static class SignupRes {
        private Long id;
        private String employeeCode;
        private String name;
        private String email;
        private String locationCode;
        private String locationName;
        private UserRole role;
        private UserStatus userStatus;
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
}
