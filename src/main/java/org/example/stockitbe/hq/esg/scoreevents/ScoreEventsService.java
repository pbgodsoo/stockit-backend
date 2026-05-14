package org.example.stockitbe.hq.esg.scoreevents;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.hq.circularbuyer.repository.CircularBuyerTransactionRepository;
import org.example.stockitbe.hq.esg.scoreevents.model.ScoreEventsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreEventsService {

    private final CircularBuyerTransactionRepository txRepo;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * 지정 연도의 sale 거래 이벤트 조회.
     *  - 각 거래에 buyer 의 최초 거래일을 동시 조회해서 isNewBuyer derive
     *  - 결과는 거래일 내림차순 (FE 도 sortedEvents 에서 같은 정렬)
     */
    public ScoreEventsDto.Response getEvents(Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();

        // row: { id, transacted_at, company_name, material_code, weight_kg, first_tx_at }
        List<Object[]> rows = txRepo.findEventsForYear(targetYear);

        List<ScoreEventsDto.EventDto> events = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Long id              = ((Number) r[0]).longValue();
            LocalDateTime txAt   = ((Timestamp) r[1]).toLocalDateTime();
            String buyer         = (String) r[2];
            String material      = (String) r[3];
            Integer weightKg     = ((Number) r[4]).intValue();
            LocalDateTime firstAt = ((Timestamp) r[5]).toLocalDateTime();

            boolean isNewBuyer = txAt.equals(firstAt);

            events.add(ScoreEventsDto.EventDto.builder()
                    .id(id)
                    .date(txAt.toLocalDate().format(DATE_FMT))
                    .type("sale")
                    .buyer(buyer)
                    .material(material)
                    .weightKg(weightKg)
                    .isNewBuyer(isNewBuyer)
                    .isLocalPartner(false)  // partner_type 미구현
                    .build());
        }

        return ScoreEventsDto.Response.builder()
                .year(targetYear)
                .events(events)
                .build();
    }
}
