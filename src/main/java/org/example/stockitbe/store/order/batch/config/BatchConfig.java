package org.example.stockitbe.store.order.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.store.order.batch.StoreOrderBatchApproveWriter;
import org.example.stockitbe.store.order.batch.model.dto.StoreOrderBatchItem;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    // 기본값 10: application.yml 설정이 없을 때의 안전망.
    // chunk 크기가 클수록 커밋 빈도가 줄어 성능이 높아지지만 재시도 단위도 커짐.
    @Value("${store-order.batch.chunk-size:10}")
    private int chunkSize;

    // 실행될 때마다 BATCH_JOB_INSTANCE, BATCH_JOB_EXECUTION 테이블에 이력 자동 기록.
    @Bean
    public Job storeOrderBatchApproveJob(Step storeOrderBatchApproveStep) {
        return new JobBuilder("storeOrderBatchApproveJob", jobRepository)
                .start(storeOrderBatchApproveStep)
                .build();
    }

    // reader/writer를 메서드 직접 호출이 아닌 파라미터로 주입받는 이유:
    // @StepScope Bean을 같은 @Configuration 안에서 직접 호출하면 스코프 프록시가 아닌
    // 실제 인스턴스가 즉시 생성되어 jobParameters SpEL 주입이 실패한다.
    // 파라미터 주입 시 Spring이 프록시를 올바르게 연결한다.
    //
    // .listener(writer)를 별도로 추가하는 이유:
    // .writer(writer)만으로는 @BeforeStep 어노테이션이 자동 감지되지 않는다.
    // .listener()로 등록해야 StepListenerFactoryBean이 어노테이션을 스캔하여
    // Step 실행 전에 beforeStep()을 호출한다.
    @Bean
    public Step storeOrderBatchApproveStep(
            JdbcCursorItemReader<StoreOrderBatchItem> storeOrderBatchApproveReader,
            StoreOrderBatchApproveWriter storeOrderBatchApproveWriter) {
        return new StepBuilder("storeOrderBatchApproveStep", jobRepository)
                .<StoreOrderBatchItem, StoreOrderBatchItem>chunk(chunkSize, transactionManager)
                .reader(storeOrderBatchApproveReader)
                .writer(storeOrderBatchApproveWriter)
                .listener(storeOrderBatchApproveWriter)
                .build();
    }

    // Reader 선택 근거:
    // - JdbcCursorItemReader: DB 커서를 열어 행을 순차 fetch → status 변경이 발생해도 OFFSET 밀림 없음.
    // - PagingItemReader 제외: 처리 중 status가 REQUESTED → APPROVED로 바뀌면
    //   다음 페이지 OFFSET이 밀려 처리 누락 발생.
    // - JpaCursorItemReader 제외: approveOne()의 REQUIRES_NEW가 JPA EntityManager와 충돌.
    //
    // 날짜 범위와 storeId는 모두 호출자(Controller/Scheduler)가 JobParameter로 전달.
    // storeId=0은 "전체 매장" 의미의 sentinel. STORE 모드일 때만 실제 storeId가 전달됨.
    @Bean
    @StepScope
    public JdbcCursorItemReader<StoreOrderBatchItem> storeOrderBatchApproveReader(
            @Value("#{jobParameters['fromDateTime']}") LocalDateTime fromDateTime,
            @Value("#{jobParameters['toDateTime']}")   LocalDateTime toDateTime,
            @Value("#{jobParameters['storeId'] ?: 0L}") Long storeId) {

        // storeId > 0이면 STORE 모드(매장별 필터), 0이면 ALL 모드(전체 매장).
        // PreparedStatement 파라미터 인덱스가 SQL 절 수에 따라 달라지므로 SQL과 setter를 함께 구성한다.
        String sql =
                "SELECT id, order_no, store_id, warehouse_id, requested_at " +
                "FROM store_order_header " +
                "WHERE status = 'REQUESTED' AND requested_at BETWEEN ? AND ?" +
                (storeId > 0 ? " AND store_id = ?" : "") +
                " ORDER BY requested_at ASC, id ASC";

        return new JdbcCursorItemReaderBuilder<StoreOrderBatchItem>()
                .name("storeOrderBatchApproveReader")
                .dataSource(dataSource)
                .sql(sql)
                .preparedStatementSetter(ps -> {
                    ps.setTimestamp(1, Timestamp.valueOf(fromDateTime));
                    ps.setTimestamp(2, Timestamp.valueOf(toDateTime));
                    // storeId 조건이 SQL에 포함된 경우에만 3번 인덱스를 바인딩한다.
                    if (storeId > 0) {
                        ps.setLong(3, storeId);
                    }
                })
                .rowMapper((rs, rowNum) -> new StoreOrderBatchItem(
                        rs.getLong("id"),
                        rs.getString("order_no"),
                        rs.getLong("store_id"),
                        rs.getLong("warehouse_id"),
                        rs.getTimestamp("requested_at")
                ))
                .build();
    }
}
