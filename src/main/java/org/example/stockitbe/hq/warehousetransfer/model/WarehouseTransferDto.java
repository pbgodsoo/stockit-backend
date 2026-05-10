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
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExecuteLineFailureRes {
        private String lineId;
        private String skuCode;
        private Integer qty;
        private String reason;
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
    }
}
