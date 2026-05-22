package org.example.stockitbe.hq.inventory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

// inventory-sku ES 인덱스 매핑 DTO (ADR-028 — sku 단위 doc 재설계).
// Logstash JDBC pipeline 이 GROUP BY sku_id 로 미리 집계된 doc 색인.
@Data
@NoArgsConstructor
public class SkuInventoryDoc {

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("sku_code")
    private String skuCode;

    private String color;
    private String size;

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
    private List<ProductInventoryDoc.LocationStock> byLocation;
}
