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

    /** 친환경 나무 키우기 점수 — 연간 sale 거래 이벤트 조회 */
    @GetMapping("/score-events")
    public ResponseEntity<BaseResponse<ScoreEventsDto.Response>> getEvents(
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(BaseResponse.success(service.getEvents(year)));
    }
}
