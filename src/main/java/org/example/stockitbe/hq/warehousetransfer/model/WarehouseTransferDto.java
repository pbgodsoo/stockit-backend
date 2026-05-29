package org.example.stockitbe.hq.warehousetransfer.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.Date;
import java.util.List;

public class WarehouseTransferDto {

    // 재고이동 실행 요청 DTO (헤더 성격)
    @Schema(description = "창고간 재고이동 실행 요청")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecuteReq {
        @Schema(description = "이동 요청자. 미지정 시 본사 관리자로 처리", example = "본사 관리자")
        private String requestedBy;

        @Schema(description = "재고이동 요청 라인 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        @Valid
        @NotEmpty
        private List<ExecuteLineReq> lines;

        public WarehouseTransferHeader toEntity(ExecuteHeaderContext context) {
            return WarehouseTransferHeader.builder()
                    .transferNo(context.getTransferNo())
                    .fromWarehouseId(context.getFromWarehouseId())
                    .toWarehouseId(context.getToWarehouseId())
                    .status(context.getStatus())
                    .requestedBy(context.getRequestedBy())
                    .requestedAt(context.getRequestedAt())
                    .reasonSummary(context.getReasonSummary())
                    .memoSummary(context.getMemoSummary())
                    .build();
        }
    }

    // 재고이동 실행 요청 라인 DTO (SKU 단위)
    @Schema(description = "창고간 재고이동 실행 요청 라인")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecuteLineReq {
        @Schema(description = "프론트엔드 라인 식별자", example = "line-1")
        private String lineId;

        @Schema(description = "이동할 SKU 코드", example = "SKU-TOP-SS-001-BLK-M", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String skuCode;

        @Schema(description = "출발 창고 코드", example = "WH-SL-0001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String fromWarehouseCode;

        @Schema(description = "도착 창고 코드", example = "WH-SL-0002", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String toWarehouseCode;

        @Schema(description = "이동 수량", example = "10", minimum = "1")
        @Min(1)
        private Integer qty;

        @Schema(description = "이동 사유", example = "창고별 재고 불균형 조정")
        private String reason;
        @Schema(description = "라인 메모", example = "긴급 보충")
        private String memo;

        public WarehouseTransferItem toEntity(ExecuteLineContext context) {
            return WarehouseTransferItem.builder()
                    .header(context.getHeader())
                    .skuId(context.getSkuId())
                    .quantity(this.qty)
                    .reason(this.reason)
                    .memo(this.memo)
                    .fromAvailableBefore(context.getFromAvailableBefore())
                    .toAvailableBefore(context.getToAvailableBefore())
                    .fromAvailableAfter(context.getFromAvailableAfter())
                    .toAvailableAfter(context.getToAvailableAfter())
                    .build();
        }
    }

    // 재고이동 실행 성공 라인 응답 DTO
    @Schema(description = "창고간 재고이동 실행 성공 라인 응답")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExecuteLineResultRes {
        @Schema(description = "프론트엔드 라인 식별자", example = "line-1")
        private String lineId;
        @Schema(description = "SKU 코드", example = "SKU-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "출발 창고 코드", example = "WH-SL-0001")
        private String fromWarehouseCode;
        @Schema(description = "도착 창고 코드", example = "WH-SL-0002")
        private String toWarehouseCode;
        @Schema(description = "이동 수량", example = "10")
        private Integer qty;
        @Schema(description = "라인 처리 성공 여부", example = "true")
        private Boolean success;
        @Schema(description = "처리 메시지", example = "SUCCESS")
        private String message;
        @Schema(description = "생성된 이동번호", example = "WTF-20260527-00001")
        private String transferNo;

        public static ExecuteLineResultRes from(ExecuteLineReq req, String transferNo, String message) {
            return ExecuteLineResultRes.builder()
                    .lineId(req.getLineId())
                    .skuCode(req.getSkuCode())
                    .fromWarehouseCode(req.getFromWarehouseCode())
                    .toWarehouseCode(req.getToWarehouseCode())
                    .qty(req.getQty())
                    .success(true)
                    .message(message)
                    .transferNo(transferNo)
                    .build();
        }
    }

    // 재고이동 실행 성공 라우트(이동지시서) 요약 응답 DTO
    @Schema(description = "창고간 재고이동 생성 라우트 요약")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExecuteTransferRes {
        @Schema(description = "창고간 재고이동 번호", example = "WTF-20260527-00001")
        private String transferNo;
        @Schema(description = "출발 창고 코드", example = "WH-SL-0001")
        private String fromWarehouseCode;
        @Schema(description = "출발 창고명", example = "서울 물류창고")
        private String fromWarehouseName;
        @Schema(description = "도착 창고 코드", example = "WH-SL-0002")
        private String toWarehouseCode;
        @Schema(description = "도착 창고명", example = "부산 물류창고")
        private String toWarehouseName;
        @Schema(description = "재고이동 상태", example = "READY_TO_SHIP")
        private String status;
        @Schema(description = "이동 SKU 종류 수", example = "2")
        private Integer skuCount;
        @Schema(description = "총 이동 수량", example = "30")
        private Integer totalQty;

        public static ExecuteTransferRes from(
                WarehouseTransferHeader header,
                String fromWarehouseCode,
                String fromWarehouseName,
                String toWarehouseCode,
                String toWarehouseName,
                Integer skuCount,
                Integer totalQty
        ) {
            return ExecuteTransferRes.builder()
                    .transferNo(header.getTransferNo())
                    .fromWarehouseCode(fromWarehouseCode)
                    .fromWarehouseName(fromWarehouseName)
                    .toWarehouseCode(toWarehouseCode)
                    .toWarehouseName(toWarehouseName)
                    .status(header.getStatus().name())
                    .skuCount(skuCount)
                    .totalQty(totalQty)
                    .build();
        }
    }

    // 재고이동 실행 종합 응답 DTO (성공/실패 집계)
    @Schema(description = "창고간 재고이동 실행 종합 응답")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExecuteRes {
        @Schema(description = "요청 라인 수", example = "3")
        private Integer requestedCount;
        @Schema(description = "성공 라인 수", example = "2")
        private Integer successCount;
        @Schema(description = "실패 라우트 수", example = "1")
        private Integer failureCount;
        @Schema(description = "성공 라인 처리 결과")
        private List<ExecuteLineResultRes> lineResults;
        @Schema(description = "생성된 이동 지시 목록")
        private List<ExecuteTransferRes> createdTransfers;
        // 부분성공 지원: 실패 라우트 목록
        @Schema(description = "실패 라우트 목록")
        private List<ExecuteFailedRouteRes> failedTransfers;

        public static ExecuteRes from(
                Integer requestedCount,
                List<ExecuteLineResultRes> lineResults,
                List<ExecuteTransferRes> createdTransfers,
                List<ExecuteFailedRouteRes> failedTransfers
        ) {
            return ExecuteRes.builder()
                    .requestedCount(requestedCount)
                    .successCount(lineResults == null ? 0 : lineResults.size())
                    .failureCount(failedTransfers == null ? 0 : failedTransfers.size())
                    .lineResults(lineResults)
                    .createdTransfers(createdTransfers)
                    .failedTransfers(failedTransfers)
                    .build();
        }
    }

    // 재고이동 실행 실패 라우트 응답 DTO
    @Schema(description = "창고간 재고이동 실행 실패 라우트")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExecuteFailedRouteRes {
        // 실패한 라우트(출발/도착 창고) 식별값
        @Schema(description = "출발 창고 코드", example = "WH-SL-0001")
        private String fromWarehouseCode;
        @Schema(description = "도착 창고 코드", example = "WH-SL-0002")
        private String toWarehouseCode;
        // 실패 사유 표준 코드/메시지
        @Schema(description = "실패 코드", example = "2000")
        private Integer errorCode;
        @Schema(description = "실패 메시지", example = "요청 값을 확인해주세요.")
        private String errorMessage;
        // 라우트 내 실패 라인 상세
        @Schema(description = "실패 라인 목록")
        private List<ExecuteLineFailureRes> failedLines;

        public static ExecuteFailedRouteRes from(
                String fromWarehouseCode,
                String toWarehouseCode,
                Integer errorCode,
                String errorMessage,
                List<ExecuteLineFailureRes> failedLines
        ) {
            return ExecuteFailedRouteRes.builder()
                    .fromWarehouseCode(fromWarehouseCode)
                    .toWarehouseCode(toWarehouseCode)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .failedLines(failedLines)
                    .build();
        }
    }

    // 재고이동 실행 실패 라인 응답 DTO
    @Schema(description = "창고간 재고이동 실행 실패 라인")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExecuteLineFailureRes {
        @Schema(description = "프론트엔드 라인 식별자", example = "line-1")
        private String lineId;
        @Schema(description = "SKU 코드", example = "SKU-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "요청 수량", example = "10")
        private Integer qty;
        @Schema(description = "실패 사유", example = "출발 창고 가용재고가 부족합니다.")
        private String reason;

        public static ExecuteLineFailureRes from(ExecuteLineReq req, String reason) {
            return ExecuteLineFailureRes.builder()
                    .lineId(req.getLineId())
                    .skuCode(req.getSkuCode())
                    .qty(req.getQty())
                    .reason(reason)
                    .build();
        }
    }

    // 재고이동 목록 1행 응답 DTO
    @Schema(description = "창고간 재고이동 목록 행")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class TransferListItemRes {
        @Schema(description = "창고간 재고이동 번호", example = "WTF-20260527-00001")
        private String transferNo;
        @Schema(description = "출발 창고 코드", example = "WH-SL-0001")
        private String fromWarehouseCode;
        @Schema(description = "출발 창고명", example = "서울 물류창고")
        private String fromWarehouseName;
        @Schema(description = "도착 창고 코드", example = "WH-SL-0002")
        private String toWarehouseCode;
        @Schema(description = "도착 창고명", example = "부산 물류창고")
        private String toWarehouseName;
        @Schema(description = "요청자", example = "본사 관리자")
        private String requestedBy;
        @Schema(description = "요청 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date requestedAt;
        @Schema(description = "재고이동 상태. READY_TO_SHIP, IN_TRANSIT, ARRIVED 중 하나", example = "READY_TO_SHIP")
        private String status;
        @Schema(description = "이동 라인 목록")
        private List<TransferLineRes> lines;
        @Schema(description = "SKU 종류 수", example = "2")
        private Integer skuCount;
        @Schema(description = "총 이동 수량", example = "30")
        private Integer totalQty;
        @Schema(description = "서로 다른 이동 사유 수", example = "1")
        private Integer reasonCount;
        @Schema(description = "메모가 있는 라인 수", example = "2")
        private Integer memoCount;

        public static TransferListItemRes from(
                WarehouseTransferHeader header,
                String fromWarehouseCode,
                String fromWarehouseName,
                String toWarehouseCode,
                String toWarehouseName,
                List<TransferLineRes> lines,
                Integer skuCount,
                Integer totalQty,
                Integer reasonCount,
                Integer memoCount
        ) {
            return TransferListItemRes.builder()
                    .transferNo(header.getTransferNo())
                    .fromWarehouseCode(fromWarehouseCode)
                    .fromWarehouseName(fromWarehouseName)
                    .toWarehouseCode(toWarehouseCode)
                    .toWarehouseName(toWarehouseName)
                    .requestedBy(header.getRequestedBy())
                    .requestedAt(header.getRequestedAt())
                    .status(header.getStatus().name())
                    .lines(lines)
                    .skuCount(skuCount)
                    .totalQty(totalQty)
                    .reasonCount(reasonCount)
                    .memoCount(memoCount)
                    .build();
        }
    }

    // 재고이동 상세 응답 DTO
    @Schema(description = "창고간 재고이동 상세 응답")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class TransferDetailRes {
        @Schema(description = "창고간 재고이동 번호", example = "WTF-20260527-00001")
        private String transferNo;
        @Schema(description = "출발 창고 코드", example = "WH-SL-0001")
        private String fromWarehouseCode;
        @Schema(description = "출발 창고명", example = "서울 물류창고")
        private String fromWarehouseName;
        @Schema(description = "도착 창고 코드", example = "WH-SL-0002")
        private String toWarehouseCode;
        @Schema(description = "도착 창고명", example = "부산 물류창고")
        private String toWarehouseName;
        @Schema(description = "요청자", example = "본사 관리자")
        private String requestedBy;
        @Schema(description = "요청 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date requestedAt;
        @Schema(description = "재고이동 상태. READY_TO_SHIP, IN_TRANSIT, ARRIVED 중 하나", example = "READY_TO_SHIP")
        private String status;
        @Schema(description = "이동 라인 목록")
        private List<TransferLineRes> lines;
        @Schema(description = "SKU 종류 수", example = "2")
        private Integer skuCount;
        @Schema(description = "총 이동 수량", example = "30")
        private Integer totalQty;
        @Schema(description = "서로 다른 이동 사유 수", example = "1")
        private Integer reasonCount;
        @Schema(description = "메모가 있는 라인 수", example = "2")
        private Integer memoCount;

        public static TransferDetailRes from(
                WarehouseTransferHeader header,
                String fromWarehouseCode,
                String fromWarehouseName,
                String toWarehouseCode,
                String toWarehouseName,
                List<TransferLineRes> lines,
                Integer skuCount,
                Integer totalQty,
                Integer reasonCount,
                Integer memoCount
        ) {
            return TransferDetailRes.builder()
                    .transferNo(header.getTransferNo())
                    .fromWarehouseCode(fromWarehouseCode)
                    .fromWarehouseName(fromWarehouseName)
                    .toWarehouseCode(toWarehouseCode)
                    .toWarehouseName(toWarehouseName)
                    .requestedBy(header.getRequestedBy())
                    .requestedAt(header.getRequestedAt())
                    .status(header.getStatus().name())
                    .lines(lines)
                    .skuCount(skuCount)
                    .totalQty(totalQty)
                    .reasonCount(reasonCount)
                    .memoCount(memoCount)
                    .build();
        }
    }

    // 재고이동 상세/목록 공통 라인 응답 DTO
    @Schema(description = "창고간 재고이동 라인 응답")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class TransferLineRes {
        @Schema(description = "SKU 코드", example = "SKU-TOP-SS-001-BLK-M")
        private String skuCode;
        @Schema(description = "품목 코드", example = "PRD-TOP-SS-001")
        private String itemCode;
        @Schema(description = "품목명", example = "폴리에스터 자켓")
        private String itemName;
        @Schema(description = "색상", example = "BLACK")
        private String color;
        @Schema(description = "사이즈", example = "M")
        private String size;
        @Schema(description = "이동 수량", example = "10")
        private Integer qty;
        @Schema(description = "이동 사유", example = "창고별 재고 불균형 조정")
        private String reason;
        @Schema(description = "라인 메모", example = "긴급 보충")
        private String memo;
        @Schema(description = "출발 창고 이동 전 가용재고", example = "100")
        private Integer fromStockBefore;
        @Schema(description = "도착 창고 이동 전 가용재고", example = "20")
        private Integer toStockBefore;
        @Schema(description = "출발 창고 이동 후 예상 가용재고", example = "90")
        private Integer fromStockAfter;
        @Schema(description = "도착 창고 이동 후 예상 가용재고", example = "30")
        private Integer toStockAfter;

        public static TransferLineRes from(
                WarehouseTransferItem item,
                String skuCode,
                String itemCode,
                String itemName,
                String color,
                String size
        ) {
            return TransferLineRes.builder()
                    .skuCode(skuCode)
                    .itemCode(itemCode)
                    .itemName(itemName)
                    .color(color)
                    .size(size)
                    .qty(item.getQuantity())
                    .reason(item.getReason())
                    .memo(item.getMemo())
                    .fromStockBefore(item.getFromAvailableBefore())
                    .toStockBefore(item.getToAvailableBefore())
                    .fromStockAfter(item.getFromAvailableAfter())
                    .toStockAfter(item.getToAvailableAfter())
                    .build();
        }
    }

    // SKU 기준 창고별 재고 분포 응답 DTO
    @Schema(description = "SKU 기준 창고별 재고 분포")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class WarehouseSkuDistributionRes {
        @Schema(description = "창고 코드", example = "WH-SL-0001")
        private String warehouseCode;
        @Schema(description = "창고명", example = "서울 물류창고")
        private String warehouseName;
        @Schema(description = "창고 지역", example = "서울")
        private String location;
        @Schema(description = "실재고 수량", example = "120")
        private Integer onHandStock;
        @Schema(description = "예약재고 수량", example = "20")
        private Integer reservedStock;
        @Schema(description = "가용재고 수량", example = "100")
        private Integer availableStock;
        @Schema(description = "안전재고 수량", example = "50")
        private Integer safetyStock;
        @Schema(description = "재고 상태", example = "정상")
        private String status;
        @Schema(description = "재고 최신 수정 일시", example = "2026-05-27T09:00:00.000+09:00")
        private Date updatedAt;

        public static WarehouseSkuDistributionRes from(
                String warehouseCode,
                String warehouseName,
                String location,
                Integer onHandStock,
                Integer reservedStock,
                Integer availableStock,
                Integer safetyStock,
                String status,
                Date updatedAt
        ) {
            return WarehouseSkuDistributionRes.builder()
                    .warehouseCode(warehouseCode)
                    .warehouseName(warehouseName)
                    .location(location)
                    .onHandStock(onHandStock)
                    .reservedStock(reservedStock)
                    .availableStock(availableStock)
                    .safetyStock(safetyStock)
                    .status(status)
                    .updatedAt(updatedAt)
                    .build();
        }
    }

    // ExecuteReq -> WarehouseTransferHeader 변환용 컨텍스트 DTO
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ExecuteHeaderContext {
        private String transferNo;
        private Long fromWarehouseId;
        private Long toWarehouseId;
        private WarehouseTransferStatus status;
        private String requestedBy;
        private Date requestedAt;
        private String reasonSummary;
        private String memoSummary;
    }

    // ExecuteLineReq -> WarehouseTransferItem 변환용 컨텍스트 DTO
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ExecuteLineContext {
        private WarehouseTransferHeader header;
        private Long skuId;
        private Integer fromAvailableBefore;
        private Integer toAvailableBefore;
        private Integer fromAvailableAfter;
        private Integer toAvailableAfter;
    }
}
