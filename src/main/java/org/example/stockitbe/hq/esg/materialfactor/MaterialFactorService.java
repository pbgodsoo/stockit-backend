package org.example.stockitbe.hq.esg.materialfactor;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.esg.materialfactor.model.MaterialFactorDto;
import org.example.stockitbe.hq.product.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 소재 환산 계수 마스터 조회 서비스.
 *  - ESG 점수/탄소 절감량 산식의 입력값(SSOT) 제공.
 *  - FE 가 첫 ESG 진입 시 1회 호출 → store 캐싱.
 *  - 본사 운영자가 material 테이블의 carbon_factor 만 갱신하면 즉시 반영 (FE 재배포 불필요).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialFactorService {

    private final MaterialRepository materialRepository;

    /**
     * active=true 인 소재 마스터 전체를 code 오름차순으로 조회.
     *  - BLEND 행 포함 (혼방 거래의 fallback factor)
     *  - 비활성 소재(active=false)는 응답에서 제외 → 운영 단계에서 deprecated 소재 자연 차단
     */
    public MaterialFactorDto.Response getActiveFactors() {
        List<MaterialFactorDto.Item> items = materialRepository.findAllByActiveTrueOrderByCodeAsc().stream()
                .map(m -> MaterialFactorDto.Item.builder()
                        .code(m.getCode())
                        .label(m.getNameKo())
                        .group(m.getMaterialGroup())
                        .factor(m.getCarbonFactor())
                        .build())
                .toList();
        return MaterialFactorDto.Response.builder()
                .factors(items)
                .build();
    }
}
