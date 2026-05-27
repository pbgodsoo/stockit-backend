package org.example.stockitbe.warehouse.inbound;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.user.model.entity.AuthUserDetails;
import org.example.stockitbe.warehouse.inbound.model.WhInboundDto;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundHeader;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * WHS-005/007/008 — 창고 관리자 입고 컨트롤러 (ERP 표준 모델).
 *
 * status 파라미터는 String — inbound 자체 status enum 이 없고 PO/outbound 의 status 를
 * join 결과로 받기 때문 (READY_TO_SHIP/IN_TRANSIT/ARRIVED/COMPLETED 문자열).
 */
@Tag(name = "창고 - 입고 관리", description = "창고 관리자 입고 관리 API (WHS-005/007/008) — ERP 표준 GRN 모델. 입고 상태는 PO/outbound join 으로 노출")
@RestController
@RequestMapping("/api/warehouse/inbound")
@RequiredArgsConstructor
public class WhInboundController {

    private final WhInboundService whInboundService;

    @Operation(
            summary = "입고 목록 조회",
            description = "로그인한 창고 관리자의 창고를 기준으로 상태·기간 필터를 적용한 입고 헤더 목록. status 는 source 도메인(PO/outbound) join 결과 (READY_TO_SHIP/IN_TRANSIT/ARRIVED/COMPLETED)."
    )
    @GetMapping
    public BaseResponse<List<WhInboundDto.ListRes>> list(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "입고 상태 (READY_TO_SHIP/IN_TRANSIT/ARRIVED/COMPLETED)") @RequestParam(required = false) String status,
            @Parameter(description = "조회 기간 시작 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "조회 기간 종료 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return BaseResponse.success(whInboundService.findAll(me, status, from, to));
    }

    @Operation(
            summary = "입고 단건 상세 조회",
            description = "입고 코드(WIB-{YYYYMMDD}-{NNNNN}) 로 헤더 + 라인 + source 도메인 status 를 반환."
    )
    @GetMapping("/{inboundCode}")
    public BaseResponse<WhInboundDto.DetailRes> detail(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "입고 코드 (WIB-{YYYYMMDD}-{NNNNN})") @PathVariable String inboundCode) {
        return BaseResponse.success(whInboundService.findByCode(me, inboundCode));
    }

    @Operation(
            summary = "입고 확정 (실재고 인식)",
            description = "ARRIVED 상태 입고를 COMPLETED 로 전환. inventory.markPhysical 호출 — 실재고 + 처리 (ADR-024). PO mirror 도 함께 COMPLETED 전환."
    )
    @PostMapping("/{inboundCode}/confirm")
    public BaseResponse<WhInboundDto.DetailRes> confirm(
            @AuthenticationPrincipal AuthUserDetails me,
            @Parameter(description = "입고 코드") @PathVariable String inboundCode,
            @RequestBody(required = false) WhInboundDto.ConfirmReq req) {
        WhInboundHeader saved = whInboundService.confirmInbound(inboundCode, me);
        return BaseResponse.success(whInboundService.findByCode(me, saved.getInboundCode()));
    }

    @Operation(
            summary = "기존 발주 → 입고 백필",
            description = "이미 진행 중인 PO 들에 대해 누락된 입고 헤더를 생성. 데이터 정합성 복구용 (운영 핫픽스)."
    )
    @PostMapping("/backfill")
    public BaseResponse<WhInboundDto.BackfillRes> backfill() {
        return BaseResponse.success(whInboundService.backfillFromPurchaseOrders());
    }
}
