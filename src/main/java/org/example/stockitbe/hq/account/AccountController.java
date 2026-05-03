package org.example.stockitbe.hq.account;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.account.model.AccountDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hq/account")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/pending")
    public ResponseEntity<BaseResponse<List<AccountDto.PendingRes>>> getPendingAccounts() {
        return ResponseEntity.ok(BaseResponse.success(accountService.getPendingAccounts()));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<AccountDto.PendingRes>>> getAllAccounts() {
        return ResponseEntity.ok(BaseResponse.success(accountService.getAllAccounts()));
    }


    @PostMapping("/{id}/approve")
    public ResponseEntity<BaseResponse<AccountDto.ProcessRes>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.success(accountService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<BaseResponse<AccountDto.ProcessRes>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.success(accountService.reject(id)));
    }

}
