package org.example.stockitbe.hq.circularbuyer;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyerDto;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CircularBuyerService {

    private static final Set<String> ALLOWED_MATERIAL_FITS = Set.of(
            "natural-single", "synthetic", "blended"
    );

    private static final Set<String> ALLOWED_PARTNER_TYPES = Set.of(
            "local_small", "social_enterprise", "general"
    );

    private static final String CODE_PREFIX = "RCV-";
    private static final int CODE_NUMBER_WIDTH = 3;
    private static final int CODE_NUMBER_MAX = 999;

    private final CircularBuyerRepository circularBuyerRepository;
    private final CircularBuyerEmbeddingService embeddingService;

    @Transactional(readOnly = true)
    public List<CircularBuyerDto.ListRes> findAll(String keyword, String materialFit) {
        if (materialFit != null && !materialFit.isBlank()) {
            validateMaterialFit(materialFit);
        }
        Specification<CircularBuyer> spec = buildSpec(keyword, materialFit);
        return circularBuyerRepository.findAll(spec).stream()
                .map(CircularBuyerDto.ListRes::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CircularBuyerDto.DetailRes findByCode(String code) {
        CircularBuyer v = lookup(code);
        return CircularBuyerDto.DetailRes.from(v);
    }

    @Transactional
    public CircularBuyerDto.DetailRes create(CircularBuyerDto.CreateReq req) {
        validateMaterialFit(req.getPrimaryMaterialFit());
        validatePartnerType(req.getPartnerType());
        String nextCode = nextCircularBuyerCode();
        CircularBuyer saved;
        try {
            saved = circularBuyerRepository.save(req.toEntity(nextCode));
        } catch (DataIntegrityViolationException e) {
            // PESSIMISTIC_WRITE 가 잡지 못한 가장자리 케이스(빈 테이블 동시 호출 등) 안전망.
            throw BaseException.from(BaseResponseStatus.CONCURRENT_CIRCULAR_BUYER_REGISTRATION);
        }
        // ADR-021 — 등록 시 항상 임베딩 생성. 실패해도 등록은 성공.
        embeddingService.embedAndApply(saved);
        return CircularBuyerDto.DetailRes.from(saved);
    }

    /**
     * 자동 코드 부여 — RCV-{NNN} 3자리 zero-padded.
     * PESSIMISTIC_WRITE 로 마지막 row 락을 잡아 동시 등록 시 한 트랜잭션이 끝날 때까지 다른 트랜잭션 대기.
     */
    private String nextCircularBuyerCode() {
        List<CircularBuyer> top = circularBuyerRepository.findAllOrderByCodeDescForUpdate(PageRequest.of(0, 1));
        int next = top.isEmpty() ? 1 : parseCodeNumber(top.get(0).getCode()) + 1;
        if (next > CODE_NUMBER_MAX) {
            throw BaseException.from(BaseResponseStatus.CIRCULAR_BUYER_CODE_EXHAUSTED);
        }
        return String.format("%s%0" + CODE_NUMBER_WIDTH + "d", CODE_PREFIX, next);
    }

    private static int parseCodeNumber(String code) {
        if (code == null || !code.startsWith(CODE_PREFIX)) return 0;
        try {
            return Integer.parseInt(code.substring(CODE_PREFIX.length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Transactional
    public CircularBuyerDto.DetailRes update(String code, CircularBuyerDto.UpdateReq req) {
        if (req.getPrimaryMaterialFit() != null) {
            validateMaterialFit(req.getPrimaryMaterialFit());
        }
        if (req.getPartnerType() != null) {
            validatePartnerType(req.getPartnerType());
        }
        CircularBuyer v = lookup(code);
        boolean rebuildEmbedding = isSemanticFieldChange(v, req);
        v.updateProfile(
                req.getCompanyName(),
                req.getIndustryGroup(),
                req.getProductTypes(),
                req.getProductNote(),
                req.getDescription(),
                req.getPrimaryMaterialFit(),
                req.getManagerName(),
                req.getPhone(),
                req.getPartnerType()
        );
        // ADR-021 — 의미 필드 중 하나라도 변경된 경우에만 임베딩 재생성. managerName/phone 만 바뀌면 OpenAI 콜 절약.
        if (rebuildEmbedding) {
            embeddingService.embedAndApply(v);
        }
        return CircularBuyerDto.DetailRes.from(v);
    }

    @Transactional
    public void delete(String code) {
        CircularBuyer v = lookup(code);
        circularBuyerRepository.delete(v);
    }

    /**
     * embedding == null 인 거래처 일괄 임베딩. 시드 30건 backfill 용.
     */
    @Transactional
    public int backfillEmbeddings() {
        return embeddingService.backfillNullEmbeddings();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private CircularBuyer lookup(String code) {
        return circularBuyerRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.CIRCULAR_BUYER_NOT_FOUND));
    }

    private void validateMaterialFit(String materialFit) {
        if (!ALLOWED_MATERIAL_FITS.contains(materialFit)) {
            throw BaseException.from(BaseResponseStatus.INVALID_MATERIAL_FIT);
        }
    }

    private void validatePartnerType(String partnerType) {
        if (!ALLOWED_PARTNER_TYPES.contains(partnerType)) {
            throw BaseException.from(BaseResponseStatus.INVALID_PARTNER_TYPE);
        }
    }

    /**
     * UpdateReq 의 PATCH 시멘틱 — null 필드는 변경 안 한 것으로 본다.
     * 같은 값으로 갱신 (예: 'Cotton' -> 'Cotton') 도 변경 X 로 판단 → 임베딩 재생성 절약.
     */
    private boolean isSemanticFieldChange(CircularBuyer v, CircularBuyerDto.UpdateReq req) {
        return changed(v.getCompanyName(), req.getCompanyName())
                || changed(v.getIndustryGroup(), req.getIndustryGroup())
                || changed(v.getPrimaryMaterialFit(), req.getPrimaryMaterialFit())
                || changed(v.getProductNote(), req.getProductNote())
                || changed(v.getDescription(), req.getDescription())
                || listChanged(v.getProductTypes(), req.getProductTypes());
    }

    private static boolean changed(String oldValue, String newValue) {
        if (newValue == null) return false;
        return !Objects.equals(oldValue, newValue);
    }

    private static boolean listChanged(List<String> oldList, List<String> newList) {
        if (newList == null) return false;
        return !Objects.equals(oldList, newList);
    }

    /**
     * 동적 필터 — keyword(companyName/code/managerName 부분일치 OR) + primaryMaterialFit equal.
     * 정렬: companyName ASC.
     */
    private Specification<CircularBuyer> buildSpec(String keyword, String materialFit) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                Predicate byCompany = cb.like(cb.lower(root.get("companyName")), like);
                Predicate byCode = cb.like(cb.lower(root.get("code")), like);
                Predicate byManager = cb.like(cb.lower(root.get("managerName")), like);
                predicates.add(cb.or(byCompany, byCode, byManager));
            }
            if (materialFit != null && !materialFit.isBlank()) {
                predicates.add(cb.equal(root.get("primaryMaterialFit"), materialFit));
            }
            query.orderBy(cb.asc(root.get("companyName")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
