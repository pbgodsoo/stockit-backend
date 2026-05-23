package org.example.stockitbe.hq.esg.scoreevents;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.esg.scoreevents.model.ScoreEventsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hq/esg")
@RequiredArgsConstructor
public class ScoreEventsController {

    private final ScoreEventsService service;

    /**
     * 친환경 나무 키우기 점수 — 풀세트 응답 (이벤트 페이지 + 통계 + 차트 데이터).
     *  파라미터:
     *   - year     : 연도. 기본 현재 연도.
     *   - page     : 0-based 페이지 번호. 기본 0.
     *   - size     : 페이지 크기. 기본 20. 서버 측에서 최대 200 으로 클램프.
     *   - dateFrom : "yyyy-MM-dd" 시작일. dateTo 와 함께 지정 시 연도 범위보다 우선.
     *   - dateTo   : "yyyy-MM-dd" 종료일 (포함).
     *   - category : ALL | saleExecution | carbon | newBuyer | localPartner. 기본 ALL.
     *
     *  응답: 이벤트 페이지 슬라이스 + summary/monthlyBreakdown/categoryBreakdown (필터 적용 후 전체 기준).
     *  FE 측에서 KPI/도넛/막대 차트를 직접 계산하지 않고 BE 응답을 그대로 사용.
     */
    @GetMapping("/score-events")
    public ResponseEntity<BaseResponse<ScoreEventsDto.Response>> getEvents(
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false, defaultValue = "ALL") String category
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                service.getEvents(year, page, size, dateFrom, dateTo, category)
        ));
    }
}
