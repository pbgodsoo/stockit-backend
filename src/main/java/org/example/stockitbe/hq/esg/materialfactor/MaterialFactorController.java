package org.example.stockitbe.hq.esg.materialfactor;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.esg.materialfactor.model.MaterialFactorDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 소재 환산 계수 마스터 조회 API.
 *  - FE ESG 페이지(대시보드 / 점수 페이지)가 진입 시 1회 호출
 *  - 응답 형태: BaseResponse<MaterialFactorDto.Response>
 */
@RestController
@RequestMapping("/api/hq/esg")
@RequiredArgsConstructor
public class MaterialFactorController {

    private final MaterialFactorService service;

    /**
     * active 소재 환산 계수 전체 조회.
     *  - 정렬: code ASC
     *  - 인증: ESG 페이지 진입자 (본사/매장 관리자) 누구나 호출 가능
     */
    @GetMapping("/material-factors")
    public ResponseEntity<BaseResponse<MaterialFactorDto.Response>> getFactors() {
        return ResponseEntity.ok(BaseResponse.success(service.getActiveFactors()));
    }
}
