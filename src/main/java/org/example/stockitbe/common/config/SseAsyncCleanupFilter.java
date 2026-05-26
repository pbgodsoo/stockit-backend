package org.example.stockitbe.common.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Tomcat 10.1.x SSE async 연결 끊김 시 RecycleRequiredException 방지 필터.
 *
 * [문제]
 * 클라이언트가 SSE 연결을 끊으면 Tomcat NIO 가 소켓 EOF 를 감지하고 AsyncContext 를 ERROR 상태로 전환한다.
 * 이때 Spring 은 내부적으로 asyncContext.dispatch() 를 시도하는데,
 * ERROR 상태에서는 dispatch() 가 IllegalStateException 을 던진다.
 * NotificationSseService.onError 콜백의 emitter.complete() 가 이 dispatch() 를 호출하고,
 * 발생한 예외는 catch (Exception ignored) 로 삼켜지면서 AsyncContext 가 미종료 상태로 남는다.
 * 결과: NIO 프로세서 재사용 시 checkRecycled() 에서 RecycleRequiredException 발생.
 *
 * [해결]
 * SSE 엔드포인트에 한해 AsyncListener 를 추가 등록한다.
 * - Tomcat 은 AsyncListener 를 등록 순서대로 실행한다.
 * - Spring 의 AsyncListener(먼저 등록)가 dispatch() 를 시도해 실패하면,
 *   이 필터의 AsyncListener(나중 등록)가 asyncContext.complete() 를 직접 호출한다.
 * - asyncContext.complete() 는 ERROR 상태에서도 COMPLETING 으로 전환 가능 →
 *   NIO 프로세서가 안전하게 재사용된다.
 * - Spring 이 이미 dispatch()/complete() 로 정상 처리한 경우 complete() 는 예외를 던지므로 무시한다.
 */
@Slf4j
@Component
public class SseAsyncCleanupFilter implements Filter {

    private static final String SSE_PATH = "/api/notifications/stream";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(request, response);

        HttpServletRequest httpReq = (HttpServletRequest) request;
        if (!httpReq.isAsyncStarted()) return;
        if (!SSE_PATH.equals(httpReq.getRequestURI())) return;

        try {
            httpReq.getAsyncContext().addListener(new AsyncListener() {

                @Override
                public void onError(AsyncEvent event) {
                    // Spring 의 dispatch() 가 ERROR 상태에서 실패한 뒤 이 콜백이 실행된다.
                    // asyncContext.complete() 는 ERROR 상태에서도 허용 → RecycleRequiredException 방지.
                    safeComplete(event.getAsyncContext(), "disconnect");
                }

                @Override
                public void onTimeout(AsyncEvent event) {
                    // Spring 이 dispatch() 로 이미 처리했으면 complete() 가 조용히 실패한다 (정상).
                    safeComplete(event.getAsyncContext(), "timeout");
                }

                @Override
                public void onComplete(AsyncEvent event) {}

                @Override
                public void onStartAsync(AsyncEvent event) {}

                private void safeComplete(AsyncContext ctx, String reason) {
                    try {
                        ctx.complete();
                        log.debug("[SseCleanup] asyncContext.complete() via {}", reason);
                    } catch (Exception ignored) {
                        // Spring 이 이미 complete()/dispatch() 를 처리한 경우 — 정상적으로 무시
                    }
                }
            });
        } catch (Exception e) {
            // chain.doFilter() 반환 직전에 async context 가 완료된 극히 드문 race — 무시 가능
            log.debug("[SseCleanup] AsyncListener 등록 실패 (무시): {}", e.getMessage());
        }
    }
}
