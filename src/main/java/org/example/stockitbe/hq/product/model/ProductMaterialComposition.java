package org.example.stockitbe.hq.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductMaterialComposition {
    private String materialCode;
    private Integer ratio;
}
