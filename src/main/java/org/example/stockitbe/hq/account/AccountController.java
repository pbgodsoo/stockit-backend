package org.example.stockitbe.hq.account;

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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hq/account")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/pending")
    public ResponseEntity<BaseResponse<Page<AccountDto.PendingRes>>> getPendingAccounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(1, size), 100);   // 1~100 보정 (DoS 방어)
        Pageable pageable = PageRequest.of(Math.max(0, page), safeSize,
                Sort.by(Sort.Direction.ASC, "appliedAt"));
        return ResponseEntity.ok(BaseResponse.success(
                accountService.getPendingAccounts(keyword, pageable)
        ));
    }


    @GetMapping
    public ResponseEntity<BaseResponse<Page<AccountDto.PendingRes>>> getAllAccounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), safeSize,
                Sort.by(Sort.Direction.DESC, "appliedAt"));
        return ResponseEntity.ok(BaseResponse.success(
                accountService.getAllAccounts(keyword, role, status, pageable)
        ));
    }



    @PostMapping("/{id}/approve")
    public ResponseEntity<BaseResponse<AccountDto.ProcessRes>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.success(accountService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<BaseResponse<AccountDto.ProcessRes>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.success(accountService.reject(id)));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<BaseResponse<AccountDto.ProcessRes>> withdraw(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.success(accountService.withdraw(id)));
    }

}
