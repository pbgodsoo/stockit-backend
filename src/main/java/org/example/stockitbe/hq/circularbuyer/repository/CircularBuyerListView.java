package org.example.stockitbe.hq.circularbuyer.repository;

import java.util.List;

/**
 * 목록 조회 전용 JPQL 프로젝션 — embedding 컬럼(~23KB/행 JSON)을 SELECT에서 제외.
 * 4만 건 환경에서 페이지당 ~460KB 불필요한 역직렬화를 방지.
 */
public interface CircularBuyerListView {
    String getCode();
    String getCompanyName();
    String getIndustryGroup();
    List<String> getFactoryProduct();
    String getDescription();
    String getPrimaryMaterialFit();
    String getManagerName();
    String getPhone();
    String getAddress();
    String getPartnerType();
}
