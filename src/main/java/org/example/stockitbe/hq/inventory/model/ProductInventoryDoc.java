package org.example.stockitbe.hq.inventory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

// inventory-master ES 인덱스 매핑 DTO (ADR-028 — product 단위 doc 재설계).
// Logstash JDBC pipeline 이 GROUP BY product_code 로 미리 집계된 doc 색인.
@Data
@NoArgsConstructor
public class ProductInventoryDoc {

    @JsonProperty("product_code")
    private String productCode;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("parent_category")
    private String parentCategory;

    @JsonProperty("child_category")
    private String childCategory;

    @JsonProperty("total_quantity")
    private Integer totalQuantity;

    @JsonProperty("total_available")
    private Integer totalAvailable;

    @JsonProperty("total_safety")
    private Integer totalSafety;

    @JsonProperty("store_quantity")
    private Integer storeQuantity;

    @JsonProperty("warehouse_quantity")
    private Integer warehouseQuantity;

    @JsonProperty("store_available")
    private Integer storeAvailable;

    @JsonProperty("warehouse_available")
    private Integer warehouseAvailable;

    @JsonProperty("store_safety")
    private Integer storeSafety;

    @JsonProperty("warehouse_safety")
    private Integer warehouseSafety;

    @JsonProperty("last_update")
    private Date lastUpdate;

    @JsonProperty("by_location")
    private List<LocationStock> byLocation;

    @Data
    @NoArgsConstructor
    public static class LocationStock {
        @JsonProperty("location_id")
        private Long locationId;

        @JsonProperty("location_code")
        private String locationCode;

        @JsonProperty("location_type")
        private String locationType;

        @JsonProperty("location_region")
        private String locationRegion;

        private Integer quantity;

        @JsonProperty("available_quantity")
        private Integer availableQuantity;

        @JsonProperty("safety_stock")
        private Integer safetyStock;
    }
}
