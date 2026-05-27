package org.example.stockitbe.hq.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.account.model.AccountDto;
import org.example.stockitbe.user.model.entity.UserRole;
import org.example.stockitbe.user.model.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "본사 - 계정 관리", description = "회원 목록 조회 · 가입 신청 승인/거절 · 회원 탈퇴 처리 API (USER-005~007, 009)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hq/account")
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "가입 대기 회원 목록 조회",
            description = "PENDING 상태의 회원을 페이지네이션으로 조회합니다. 정렬: 신청일 오름차순.")
    @GetMapping("/pending")
    public ResponseEntity<BaseResponse<Page<AccountDto.PendingRes>>> getPendingAccounts(
            @Parameter(description = "이름/이메일 검색 키워드", example = "")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1~100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(1, size), 100);   // 1~100 보정 (DoS 방어)
        Pageable pageable = PageRequest.of(Math.max(0, page), safeSize,
                Sort.by(Sort.Direction.ASC, "appliedAt"));
        return ResponseEntity.ok(BaseResponse.success(
                accountService.getPendingAccounts(keyword, pageable)
        ));
    }


    @Operation(summary = "전체 회원 목록 조회",
            description = "권한/상태/키워드 필터로 회원 목록을 조회합니다. 정렬: 신청일 내림차순.")
    @GetMapping
    public ResponseEntity<BaseResponse<Page<AccountDto.PendingRes>>> getAllAccounts(
            @Parameter(description = "이름/이메일 검색 키워드", example = "")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "권한 필터 (HQ/STORE/WAREHOUSE)", example = "STORE")
            @RequestParam(required = false) UserRole role,
            @Parameter(description = "상태 필터 (PENDING/APPROVED/REJECTED/WITHDRAWN)", example = "APPROVED")
            @RequestParam(required = false) UserStatus status,
            @Parameter(description = "페이지 번호 (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1~100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), safeSize,
                Sort.by(Sort.Direction.DESC, "appliedAt"));
        return ResponseEntity.ok(BaseResponse.success(
                accountService.getAllAccounts(keyword, role, status, pageable)
        ));
    }



    @Operation(summary = "가입 신청 승인",
            description = "대기 중인 회원을 승인하고 사원코드를 자동 발급합니다 (HQ→hq0001, STORE→st0001, WAREHOUSE→wh0001).")
    @PostMapping("/{id}/approve")
    public ResponseEntity<BaseResponse<AccountDto.ProcessRes>> approve(
            @Parameter(description = "회원 ID", example = "5") @PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.success(accountService.approve(id)));
    }

    @Operation(summary = "가입 신청 거절",
            description = "대기 중인 회원의 가입 신청을 거절합니다.")
    @PostMapping("/{id}/reject")
    public ResponseEntity<BaseResponse<AccountDto.ProcessRes>> reject(
            @Parameter(description = "회원 ID", example = "5") @PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.success(accountService.reject(id)));
    }

    @Operation(summary = "회원 탈퇴 처리",
            description = "회원 상태를 WITHDRAWN으로 변경하고 모든 Refresh Token을 무효화합니다.")
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<BaseResponse<AccountDto.ProcessRes>> withdraw(
            @Parameter(description = "회원 ID", example = "5") @PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.success(accountService.withdraw(id)));
    }

}
