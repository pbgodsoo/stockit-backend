package org.example.stockitbe.warehouse.outbound.model;

public enum OutboundDestinationType {
    STORE,              // 매장
    WAREHOUSE,          // 물류창고
    CIRCULAR_BUYER,     // 순환판매 구매처
    DONATION            // 기부처 (destination_id=null, destination_name에 기부처명 저장)
}

