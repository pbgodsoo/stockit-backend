package org.example.stockitbe.common.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@Configuration
@EnableScheduling     // Spring 스케줄러 활성화
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M") // 락 미해제 시 최대 30분 후 자동 해제 (Pod 비정상 종료 대비)
public class ShedLockConfig {

    // 락 저장소를 MariaDB(shedlock 테이블)로 지정
    // 다중 Pod 환경에서 하나의 Pod만 배치를 실행하도록 DB를 분산 락 저장소로 활용
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime() // 각 Pod의 시스템 시각 편차를 제거하기 위해 DB 서버 시각 기준으로 만료 계산
                .build()
        );
    }
}
