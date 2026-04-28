package org.example.stockitbe.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BaseResponseStatus {
    // 2000번대 성공
    SUCCESS(true, 2000, "요청 성공"),

    // 3000번대 클라이언트 입력 오류, 입력값 검증 오류
    JWT_EXPIRED(false, 3001, "JWT 토큰 만료"),
    JWT_INVALID(false, 3002, "JWT 토큰 유효하지 않음"),
    SIGNUP_DUPLICATE_EMAIL(false, 3003, "중복된 이메일"),
    SIGNUP_INVALID_PASSWORD(false, 3004, "비밀번호는 대소문자, 숫자, 특수문자 포함"),
    SIGNUP_INVALID_UUID(false, 3005, "유효하지 않은 인증값"),
    USER_NOT_FOUND(false, 3006, "존재하지 않는 사용자입니다."),
    PASSWORD_WRONG(false, 3007, "비밀번호가 일치하지 않습니다."),

    // 4000번대 실패
    REQUEST_ERROR(false, 4001, "입력값이 잘못되었습니다."),
    NOT_FOUND_DATA(false, 4002, "데이터를 찾을 수 없습니다."),
    ALREADY_JOINED(false, 4003, "이미 참여 중인 모집글이 있습니다."),
    RECRUIT_FULL(false, 4004, "모집이 마감되었거나 인원이 초과되었습니다."),
    ALREADY_CALLED(false, 4005, "이미 기사님 호출이 완료되어 나갈 수 없습니다."),
    LOGIN_FAILED(false, 4006, "로그인에 실패하였습니다."),
    DRIVER_ROLE_REQUIRED(false, 4007, "드라이버 계정만 로그인할 수 있습니다."),
    ADMIN_ONLY_ACCESS(false, 4008, "관리자 계정만 공지사항 작업을 할 수 있습니다."),

    // 4100번대~ 결제 관련
    PAYMENT_UNAUTHENTICATED_USER(false, 4100, "인증받지 않은 사용자입니다."),
    PAYMENT_ENROLL_INVALID_USER(false, 4101, "결제 수단을 등록할 수 없는 사용자입니다."),
    PAYMENT_ENROLL_INVALID_CUSTOMER_KEY(false, 4102, "고객 키가 일치하지 않습니다."),
    PAYMENT_BILLING_NOT_EXIST(false, 4103, "존재하지 않는 결제 수단입니다."),
    PAYMENT_BILLING_INVALID_OWNER(false, 4104, "결제 수단의 소유자가 아닙니다."),
    PAYMENT_BILLING_REQUIRED(false, 4105, "최소 1개의 결제 수단이 필요합니다."),
    PAYMENT_DEFAULT_BILLING_REQUIRED(false, 4106, "기본 결제 수단이 존재하지 않습니다."),

    // 4200번대~ 공급처별 제품 (CEN-027~031, 033)
    VENDOR_NOT_FOUND(false, 4200, "거래처를 찾을 수 없습니다."),
    VENDOR_PRODUCT_NOT_FOUND(false, 4201, "공급처별 제품 계약을 찾을 수 없습니다."),
    DUPLICATE_VENDOR_PRODUCT_CODE(false, 4202, "이미 등록된 거래처-제품 코드 조합입니다."),
    CATEGORY_NOT_FOUND(false, 4203, "카테고리를 찾을 수 없습니다."),
    DUPLICATE_CATEGORY_NAME(false, 4204, "동일한 범위에 같은 카테고리명이 이미 존재합니다."),
    CATEGORY_PARENT_REQUIRED(false, 4205, "소분류 등록 시 상위 카테고리가 필요합니다."),
    CATEGORY_PARENT_NOT_FOUND(false, 4206, "상위 카테고리를 찾을 수 없습니다."),
    CATEGORY_PARENT_NOT_ROOT(false, 4207, "상위 카테고리는 대분류만 지정할 수 있습니다."),
    CATEGORY_ROOT_PARENT_DISALLOWED(false, 4208, "대분류는 상위 카테고리를 가질 수 없습니다."),
    CATEGORY_DELETE_HAS_CHILDREN(false, 4209, "하위 카테고리가 존재하여 삭제할 수 없습니다."),

    // 4300번대~ 본사 발주 (CEN-035~040)
    PURCHASE_ORDER_NOT_FOUND(false, 4300, "본사 발주를 찾을 수 없습니다."),
    PURCHASE_ORDER_INVALID_STATUS_TRANSITION(false, 4301, "허용되지 않는 발주 상태 전환입니다."),
    PURCHASE_ORDER_EMPTY_ITEMS(false, 4302, "발주 품목이 비어 있습니다."),
    PURCHASE_ORDER_VENDOR_PRODUCT_MISMATCH(false, 4303, "발주 거래처와 품목의 거래처가 일치하지 않습니다."),
    PURCHASE_ORDER_CANCEL_REASON_REQUIRED(false, 4304, "발주 취소 사유는 필수입니다."),

    // 5000번대 실패
    FAIL(false, 5000, "요청 실패");

    private final boolean success;
    private final int code;
    private final String message;
}
