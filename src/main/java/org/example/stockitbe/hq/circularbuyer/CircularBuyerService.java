package org.example.stockitbe.hq.circularbuyer;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyer;
import org.example.stockitbe.hq.circularbuyer.model.CircularBuyerDto;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerListView;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerRepository;
import org.example.stockitbe.hq.circularbuyer.sync.CircularBuyerEsSyncService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CircularBuyerService {

    private static final Set<String> ALLOWED_MATERIAL_FITS = Set.of(
            "natural-single", "synthetic", "blended"
    );

    private static final Set<String> ALLOWED_PARTNER_TYPES = Set.of(
            "local_small", "social_enterprise", "general"
    );

    private static final String CODE_PREFIX = "RCV-";
    private static final int CODE_NUMBER_WIDTH = 5;
    private static final int CODE_NUMBER_MAX = 99_999_999;

    private final CircularBuyerRepository circularBuyerRepository;
    private final CircularBuyerEmbeddingService embeddingService;
    private final CircularBuyerEsSearchService esSearchService;
    private final CircularBuyerEsSyncService esSyncService;

    // 4만 건+ 환경에서 페이지 없는 전체 조회는 응답 불가 수준의 부하. 최대 500건으로 제한.
    private static final int FIND_ALL_HARD_LIMIT = 500;

    @Transactional(readOnly = true)
    public List<CircularBuyerDto.ListRes> findAll(String keyword, String materialFit, String partnerType) {
        return findPage(keyword, materialFit, partnerType, PageRequest.of(0, FIND_ALL_HARD_LIMIT))
                .getContent();
    }

    @Transactional(readOnly = true)
    public CircularBuyerDto.PageRes findPage(String keyword, String materialFit, String partnerType, Pageable pageable) {
        if (materialFit != null && !materialFit.isBlank()) {
            validateMaterialFit(materialFit);
        }
        if (partnerType != null && !partnerType.isBlank()) {
            validatePartnerType(partnerType);
        }
        try {
            return esSearchService.findPage(keyword, materialFit, partnerType, pageable);
        } catch (Exception e) {
            log.warn("순환재고 거래처 ES 조회 실패 — RDB fallback 수행. reason={}", e.getMessage());
        }
        return findPageFromRdb(keyword, materialFit, partnerType, pageable);
    }

    private CircularBuyerDto.PageRes findPageFromRdb(String keyword, String materialFit, String partnerType, Pageable pageable) {
        // embedding 컬럼 제외 JPQL 프로젝션 사용 — 행당 ~23KB JSON 역직렬화 방지.
        String kw = (keyword != null && !keyword.isBlank())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        String mf = (materialFit != null && !materialFit.isBlank()) ? materialFit : null;
        String pt = (partnerType != null && !partnerType.isBlank()) ? partnerType : null;
        Page<CircularBuyerListView> page = circularBuyerRepository.findPageWithoutEmbedding(kw, mf, pt, pageable);
        return CircularBuyerDto.PageRes.builder()
                .content(page.getContent().stream().map(this::fromView).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    private CircularBuyerDto.ListRes fromView(CircularBuyerListView v) {
        return CircularBuyerDto.ListRes.builder()
                .code(v.getCode())
                .companyName(v.getCompanyName())
                .industryGroup(v.getIndustryGroup())
                .factoryProduct(v.getFactoryProduct())
                .description(v.getDescription())
                .primaryMaterialFit(v.getPrimaryMaterialFit())
                .managerName(v.getManagerName())
                .phone(v.getPhone())
                .address(v.getAddress())
                .partnerType(v.getPartnerType())
                .build();
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
        runAfterCommit(() -> esSyncService.syncUpsert(saved));
        return CircularBuyerDto.DetailRes.from(saved);
    }

    /**
     * 자동 코드 부여 — RCV-{NNNNN} 5자리 zero-padded.
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
                req.getFactoryProduct(),
                req.getDescription(),
                req.getPrimaryMaterialFit(),
                req.getManagerName(),
                req.getPhone(),
                req.getAddress(),
                req.getPartnerType()
        );
        // ADR-021 — 의미 필드 중 하나라도 변경된 경우에만 임베딩 재생성. managerName/phone 만 바뀌면 OpenAI 콜 절약.
        if (rebuildEmbedding) {
            embeddingService.embedAndApply(v);
        }
        runAfterCommit(() -> esSyncService.syncUpsert(v));
        return CircularBuyerDto.DetailRes.from(v);
    }

    @Transactional
    public void delete(String code) {
        CircularBuyer v = lookup(code);
        String buyerCode = v.getCode();
        circularBuyerRepository.delete(v);
        runAfterCommit(() -> esSyncService.syncDelete(buyerCode));
    }

    /** primaryMaterialFit 별 전체 건수 — 통계 카드용. */
    @Transactional(readOnly = true)
    public Map<String, Long> countByMaterialFit() {
        return circularBuyerRepository.countGroupByMaterialFit().stream()
                .collect(Collectors.toMap(
                        v -> v.getMaterialFit() != null ? v.getMaterialFit() : "unknown",
                        v -> v.getCount() != null ? v.getCount() : 0L
                ));
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
                || changed(v.getAddress(), req.getAddress())
                || changed(v.getDescription(), req.getDescription())
                || listChanged(v.getFactoryProduct(), req.getFactoryProduct());
    }

    private static boolean changed(String oldValue, String newValue) {
        if (newValue == null) return false;
        return !Objects.equals(oldValue, newValue);
    }

    private static boolean listChanged(List<String> oldList, List<String> newList) {
        if (newList == null) return false;
        return !Objects.equals(oldList, newList);
    }

    private static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    /**
     * 동적 필터 — keyword(companyName/code/managerName 부분일치 OR) + primaryMaterialFit equal + partnerType equal.
     * 정렬: companyName ASC.
     */
    private Specification<CircularBuyer> buildSpec(String keyword, String materialFit, String partnerType, boolean sortByCompanyName) {
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
            if (partnerType != null && !partnerType.isBlank()) {
                predicates.add(cb.equal(root.get("partnerType"), partnerType));
            }
            if (sortByCompanyName) {
                query.orderBy(cb.asc(root.get("companyName")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
