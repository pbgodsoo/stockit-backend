package org.example.stockitbe.hq.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductMaterialSpec {
    private ProductMaterialType materialType;
    private List<ProductMaterialComposition> compositions;
}
