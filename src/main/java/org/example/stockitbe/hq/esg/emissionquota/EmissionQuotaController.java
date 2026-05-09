package org.example.stockitbe.hq.esg.emissionquota;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.esg.emissionquota.model.EmissionQuotaDto;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hq/esg/quota")
@RequiredArgsConstructor
public class EmissionQuotaController {

    private final EmissionQuotaService service;

    /** 현재 회계연도 할당량/YTD/경고 임계 조회 */
    @GetMapping
    public BaseResponse<EmissionQuotaDto.Response> getQuota(
            @RequestParam(required = false) Integer year
    ) {
        return BaseResponse.success(service.getQuota(year));
    }

    /** 본사 관리자 수기 입력 — 수정 버튼 → 저장 */
    @PutMapping
    public BaseResponse<EmissionQuotaDto.Response> updateQuota(
            @RequestParam(required = false) Integer year,
            @RequestBody EmissionQuotaDto.UpdateRequest req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        String employeeCode = (user != null) ? user.getEmployeeCode() : "unknown";
        return BaseResponse.success(service.updateQuota(year, req, employeeCode));
    }
}
