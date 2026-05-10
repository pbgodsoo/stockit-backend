package org.example.stockitbe.store.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.store.order.model.StoreOrderBatchScope;
import org.example.stockitbe.store.order.model.StoreOrderBatchTriggerType;
import org.example.stockitbe.store.order.model.StoreOrderStatus;
import org.example.stockitbe.store.order.model.dto.StoreOrderBatchDto;
import org.example.stockitbe.store.order.model.entity.StoreOrderHeader;
import org.example.stockitbe.store.order.repository.StoreOrderHeaderRepository;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreOrderBatchApproveService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StoreOrderHeaderRepository headerRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final StoreOrderService storeOrderService;

    // 발주 수동 배치
    @Transactional
    public StoreOrderBatchDto.RunRes runManual(StoreOrderBatchDto.RunReq req, AuthUserDetails me) {
        StoreOrderBatchScope scope = req == null || req.getMode() == null ? StoreOrderBatchScope.ALL : req.getMode();
        String runId = UUID.randomUUID().toString();
        String actorId = me == null ? "" : me.getEmployeeCode();
        String actorName = me == null ? "SYSTEM" : me.getName();

        List<StoreOrderHeader> targets = switch (scope) {
            case ALL -> headerRepository.findAllByStatusOrderByRequestedAtAscIdAsc(StoreOrderStatus.REQUESTED);
            case STORE -> {
                String storeCode = trimToNull(req == null ? null : req.getStoreCode());
                if (storeCode == null) throw BaseException.from(BaseResponseStatus.STORE_ORDER_BATCH_STORE_CODE_REQUIRED);
                Long storeId = infrastructureRepository.findByCodeAndLocationType(storeCode, LocationType.STORE)
                        .map(Infrastructure::getId)
                        .orElseThrow(() -> BaseException.from(BaseResponseStatus.STORE_ORDER_STORE_NOT_FOUND));
                yield headerRepository.findAllByStatusAndStoreIdOrderByRequestedAtAscIdAsc(StoreOrderStatus.REQUESTED, storeId);
            }
            default -> throw BaseException.from(BaseResponseStatus.STORE_ORDER_BATCH_SCOPE_INVALID);
        };

        log.info("[STORE-ORDER-BATCH] start runId={} trigger=MANUAL scope={} storeCode={} requested={}",
                runId, scope, req == null ? null : req.getStoreCode(), targets.size());

        List<StoreOrderBatchDto.ItemRes> itemResults = new ArrayList<>();
        int success = 0;
        for (StoreOrderHeader header : targets) {
            try {
                storeOrderService.approveByBatch(
                        header.getOrderNo(),
                        actorId,
                        actorName,
                        "본사 수동 배치 처리"
                );
                success++;
                itemResults.add(StoreOrderBatchDto.ItemRes.builder()
                        .orderNo(header.getOrderNo())
                        .result("SUCCESS")
                        .code(BaseResponseStatus.SUCCESS.getCode())
                        .message("APPROVED")
                        .build());
            } catch (Exception e) {
                BaseResponseStatus status = extractStatus(e);
                itemResults.add(StoreOrderBatchDto.ItemRes.builder()
                        .orderNo(header.getOrderNo())
                        .result("FAIL")
                        .code(status.getCode())
                        .message(status.getMessage())
                        .build());
                log.warn("[STORE-ORDER-BATCH] fail runId={} orderNo={} code={} msg={}",
                        runId, header.getOrderNo(), status.getCode(), status.getMessage(), e);
            }
        }

        int fail = targets.size() - success;
        log.info("[STORE-ORDER-BATCH] end runId={} trigger=MANUAL scope={} success={} fail={}",
                runId, scope, success, fail);

        return StoreOrderBatchDto.RunRes.builder()
                .runId(runId)
                .triggerType(StoreOrderBatchTriggerType.MANUAL)
                .scope(scope)
                .storeCode(req == null ? null : req.getStoreCode())
                .requestedCount(targets.size())
                .successCount(success)
                .failCount(fail)
                .results(itemResults)
                .build();
    }

    // 발주 자동 배치
    @Transactional
    public StoreOrderBatchDto.RunRes runAutoDaily() {
        String runId = UUID.randomUUID().toString();
        Date[] range = previousDayRange();

        List<StoreOrderHeader> targets = headerRepository.findAllByStatusAndRequestedAtBetweenOrderByRequestedAtDescIdDesc(
                StoreOrderStatus.REQUESTED, range[0], range[1]
        );

        log.info("[STORE-ORDER-BATCH] start runId={} trigger=AUTO from={} to={} requested={}",
                runId, range[0], range[1], targets.size());

        List<StoreOrderBatchDto.ItemRes> itemResults = new ArrayList<>();
        int success = 0;
        for (StoreOrderHeader header : targets) {
            try {
                storeOrderService.approveByBatch(
                        header.getOrderNo(),
                        "SYSTEM_BATCH",
                        "SYSTEM_BATCH",
                        "AUTO_BATCH_APPROVE"
                );
                success++;
                itemResults.add(StoreOrderBatchDto.ItemRes.builder()
                        .orderNo(header.getOrderNo())
                        .result("SUCCESS")
                        .code(BaseResponseStatus.SUCCESS.getCode())
                        .message("APPROVED")
                        .build());
            } catch (Exception e) {
                BaseResponseStatus status = extractStatus(e);
                itemResults.add(StoreOrderBatchDto.ItemRes.builder()
                        .orderNo(header.getOrderNo())
                        .result("FAIL")
                        .code(status.getCode())
                        .message(status.getMessage())
                        .build());
                log.warn("[STORE-ORDER-BATCH] fail runId={} orderNo={} code={} msg={}",
                        runId, header.getOrderNo(), status.getCode(), status.getMessage(), e);
            }
        }

        int fail = targets.size() - success;
        log.info("[STORE-ORDER-BATCH] end runId={} trigger=AUTO success={} fail={}", runId, success, fail);

        return StoreOrderBatchDto.RunRes.builder()
                .runId(runId)
                .triggerType(StoreOrderBatchTriggerType.AUTO)
                .scope(StoreOrderBatchScope.ALL)
                .storeCode(null)
                .requestedCount(targets.size())
                .successCount(success)
                .failCount(fail)
                .results(itemResults)
                .build();
    }

    // 승인 대기 발주건이 있는 매장 조회
    @Transactional(readOnly = true)
    public List<StoreOrderBatchDto.PendingStoreRes> listPendingStores() {
        List<StoreOrderHeaderRepository.PendingStoreProjection> rows =
                headerRepository.countPendingByStore(StoreOrderStatus.REQUESTED);
        if (rows.isEmpty()) return List.of();

        List<Long> storeIds = rows.stream().map(StoreOrderHeaderRepository.PendingStoreProjection::getStoreId).toList();
        Map<Long, Infrastructure> infraById = new HashMap<>();
        for (Infrastructure infra : infrastructureRepository.findAllById(storeIds)) {
            infraById.put(infra.getId(), infra);
        }

        List<StoreOrderBatchDto.PendingStoreRes> result = new ArrayList<>();
        for (StoreOrderHeaderRepository.PendingStoreProjection row : rows) {
            Infrastructure store = infraById.get(row.getStoreId());
            if (store == null) continue;
            result.add(StoreOrderBatchDto.PendingStoreRes.builder()
                    .storeCode(store.getCode())
                    .storeName(store.getName())
                    .region(store.getRegion())
                    .requestedCount(row.getRequestedCount() == null ? 0 : row.getRequestedCount().intValue())
                    .build());
        }

        result.sort(Comparator.comparing(StoreOrderBatchDto.PendingStoreRes::getRequestedCount).reversed()
                .thenComparing(StoreOrderBatchDto.PendingStoreRes::getStoreName));
        return result;
    }

    // ---------------------------- 내부 메서드 --------------------------------

    private BaseResponseStatus extractStatus(Exception e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof BaseException be && be.getStatus() != null) return be.getStatus();
            cur = cur.getCause();
        }
        return BaseResponseStatus.FAIL;
    }

    private Date[] previousDayRange() {
        LocalDate prev = LocalDate.now(KST).minusDays(1);
        ZonedDateTime start = prev.atStartOfDay(KST);
        ZonedDateTime end = prev.plusDays(1).atStartOfDay(KST).minusNanos(1);
        return new Date[]{Date.from(start.toInstant()), Date.from(end.toInstant())};
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
