package org.example.stockitbe.hq.warehousetransfer.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.Date;
import java.util.List;

public class WarehouseTransferDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecuteReq {
        private String requestedBy;

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

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecuteLineReq {
        private String lineId;

        @NotBlank
        private String skuCode;

        @NotBlank
        private String fromWarehouseCode;

        @NotBlank
        private String toWarehouseCode;

        @Min(1)
        private Integer qty;

        private String reason;
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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExecuteLineResultRes {
        private String lineId;
        private String skuCode;
        private String fromWarehouseCode;
        private String toWarehouseCode;
        private Integer qty;
        private Boolean success;
        private String message;
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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExecuteTransferRes {
        private String transferNo;
        private String fromWarehouseCode;
        private String fromWarehouseName;
        private String toWarehouseCode;
        private String toWarehouseName;
        private String status;
        private Integer skuCount;
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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExecuteRes {
        private Integer requestedCount;
        private Integer successCount;
        private Integer failureCount;
        private List<ExecuteLineResultRes> lineResults;
        private List<ExecuteTransferRes> createdTransfers;
        // 부분성공 지원: 실패 라우트 목록
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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExecuteFailedRouteRes {
        // 실패한 라우트(출발/도착 창고) 식별값
        private String fromWarehouseCode;
        private String toWarehouseCode;
        // 실패 사유 표준 코드/메시지
        private Integer errorCode;
        private String errorMessage;
        // 라우트 내 실패 라인 상세
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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExecuteLineFailureRes {
        private String lineId;
        private String skuCode;
        private Integer qty;
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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TransferListItemRes {
        private String transferNo;
        private String fromWarehouseCode;
        private String fromWarehouseName;
        private String toWarehouseCode;
        private String toWarehouseName;
        private String requestedBy;
        private Date requestedAt;
        private String status;
        private List<TransferLineRes> lines;
        private Integer skuCount;
        private Integer totalQty;
        private Integer reasonCount;
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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TransferDetailRes {
        private String transferNo;
        private String fromWarehouseCode;
        private String fromWarehouseName;
        private String toWarehouseCode;
        private String toWarehouseName;
        private String requestedBy;
        private Date requestedAt;
        private String status;
        private List<TransferLineRes> lines;
        private Integer skuCount;
        private Integer totalQty;
        private Integer reasonCount;
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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TransferLineRes {
        private String skuCode;
        private String itemCode;
        private String itemName;
        private String color;
        private String size;
        private Integer qty;
        private String reason;
        private String memo;
        private Integer fromStockBefore;
        private Integer toStockBefore;
        private Integer fromStockAfter;
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

    @Getter
    @Builder
    @AllArgsConstructor
    public static class WarehouseSkuDistributionRes {
        private String warehouseCode;
        private String warehouseName;
        private String location;
        private Integer onHandStock;
        private Integer reservedStock;
        private Integer availableStock;
        private Integer safetyStock;
        private String status;
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
