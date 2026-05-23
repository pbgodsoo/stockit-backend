package org.example.stockitbe.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 전용 스레드 풀 설정.
 *
 * 현재 등록 풀:
 *   - notificationExecutor : SSE push fan-out 전용 (1500+ HQ admin 대응)
 *
 * 정책:
 *   - CallerRunsPolicy — 큐 가득 차면 호출자(도메인 스레드)가 실행.
 *     알림 누락 없이 약간의 지연만 발생 (Discard/Abort 보다 안전).
 *   - 사용자별 emitter.send 가 별도 스레드에서 병렬 실행 → fan-out 시간 ↓.
 */
@Configuration
public class AsyncConfig {

    // SSE push 전용 풀.
    // 1500명 fan-out 시 사용자별 send 를 N개 스레드로 병렬화 → 도메인 응답 즉시 반환.
    @Bean(name = "notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);            // 평시 유지 스레드 수
        executor.setMaxPoolSize(32);            // 폭주 시 최대 스레드 수
        executor.setQueueCapacity(2000);        // 1500명 + 여유 (버퍼링)
        executor.setKeepAliveSeconds(60);       // core 초과 스레드는 60초 후 회수
        executor.setThreadNamePrefix("notif-"); // 로그/스레드 덤프에서 식별 용이
        // 큐 가득 시 호출자(도메인 스레드)가 직접 실행 — 알림 손실 방지 + 자연스러운 백프레셔
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 부팅 시 풀 미리 초기화
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

}
