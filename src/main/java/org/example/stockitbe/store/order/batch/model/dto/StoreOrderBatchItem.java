package org.example.stockitbe.store.order.batch.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

// StoreOrderHeader는 @NoArgsConstructor(access = PROTECTED)라 외부 패키지 RowMapper로 직접 매핑 불가.
// JdbcCursorItemReader 전용 조회 DTO로 분리해 batch 패키지에서 독립적으로 사용한다.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrderBatchItem {
    private Long id;
    private String orderNo;
    private Long storeId;
    private Long warehouseId;
    private Date requestedAt;
}
