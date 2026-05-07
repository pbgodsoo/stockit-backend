package org.example.stockitbe.warehouse.inbound;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.warehouse.inbound.model.entity.WhInboundHeader;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 입고번호 자동 생성기 — `IB-{YYYYMMDD}-{NNNNN}` 5자리 zero-pad.
 * PO 의 PO-{YYYYMMDD}-{NNNNN} 패턴 일관 (PR #188).
 *
 * 호출자 (Service) 에서 unique 충돌 시 retry 책임. nextCode() 자체는 단순히 다음 시퀀스 반환.
 */
@Component
@RequiredArgsConstructor
public class InboundCodeGenerator {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String PREFIX = "IB-";

    private final WhInboundHeaderRepository repository;

    public String nextCode(Date now) {
        LocalDate day = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String prefix = PREFIX + day.format(DAY_FORMAT) + "-";
        int nextSeq = repository.findTopByInboundCodeStartingWithOrderByInboundCodeDesc(prefix)
                .map(WhInboundHeader::getInboundCode)
                .map(this::parseSeq)
                .orElse(0) + 1;
        return prefix + String.format("%05d", nextSeq);
    }

    private int parseSeq(String inboundCode) {
        try {
            int dash = inboundCode.lastIndexOf('-');
            if (dash < 0 || dash + 1 >= inboundCode.length()) return 0;
            return Integer.parseInt(inboundCode.substring(dash + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
