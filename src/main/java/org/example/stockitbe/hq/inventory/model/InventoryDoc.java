package org.example.stockitbe.hq.inventory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

// ES `inventory` 인덱스의 평탄화 doc 매핑 DTO (ADR-028).
// Logstash JDBC pipeline 의 SELECT alias 와 1:1 대응 — 변경 시 07-logstash.yaml 도 함께.
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDoc {

    private Long id;

    @JsonProperty("sku_id")
    private Long skuId;

    @JsonProperty("location_id")
    private Long locationId;

    @JsonProperty("inventory_status")
    private String inventoryStatus;

    private Integer quantity;

    @JsonProperty("available_quantity")
    private Integer availableQuantity;

    @JsonProperty("reserved_quantity")
    private Integer reservedQuantity;

    @JsonProperty("in_transit_quantity")
    private Integer inTransitQuantity;

    @JsonProperty("status_changed_at")
    private Date statusChangedAt;

    @JsonProperty("last_movement_at")
    private Date lastMovementAt;

    @JsonProperty("update_date")
    private Date updateDate;

    @JsonProperty("sku_code")
    private String skuCode;

    private String color;

    private String size;

    @JsonProperty("unit_price")
    private Long unitPrice;

    @JsonProperty("product_code")
    private String productCode;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("warehouse_safety_stock")
    private Integer warehouseSafetyStock;

    @JsonProperty("store_safety_stock")
    private Integer storeSafetyStock;

    @JsonProperty("location_code")
    private String locationCode;

    @JsonProperty("location_name")
    private String locationName;

    @JsonProperty("location_type")
    private String locationType;

    @JsonProperty("location_region")
    private String locationRegion;

    @JsonProperty("child_category_code")
    private String childCategoryCode;

    @JsonProperty("child_category")
    private String childCategory;

    @JsonProperty("parent_category_code")
    private String parentCategoryCode;

    @JsonProperty("parent_category")
    private String parentCategory;
}
