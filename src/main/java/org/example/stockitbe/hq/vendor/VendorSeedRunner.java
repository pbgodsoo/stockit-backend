package org.example.stockitbe.hq.vendor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.stockitbe.hq.vendor.model.Vendor;
import org.example.stockitbe.hq.vendor.model.VendorStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * dev 프로파일 부팅 시 vendor 테이블이 비어 있으면 거래처 12개를 자동 INSERT.
 * 운영(prod) 에선 동작하지 않음 — 운영 거래처 데이터는 운영자가 별도 관리.
 *
 * 빈 테이블 조건(count == 0) — 이미 데이터 있으면 건너뜀.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class VendorSeedRunner implements CommandLineRunner {

    private final VendorRepository vendorRepository;

    @Override
    public void run(String... args) {
        if (vendorRepository.count() > 0) {
            log.info("[VendorSeedRunner] vendor 테이블에 데이터 존재 — 시드 건너뜀");
            return;
        }

        List<Vendor> seed = List.of(
                vendor("VND-001", "(주)베이직어패럴",       "김민준", "02-1234-5678",  "minjun.kim@basicapparel.co.kr",   VendorStatus.ACTIVE),
                vendor("VND-002", "모던셔츠",               "이수연", "031-9876-5432", "suyeon.lee@modernshirt.co.kr",    VendorStatus.ACTIVE),
                vendor("VND-003", "데일리진",               "박재현", "02-5555-3333",  "jaehyun.park@dailyjean.co.kr",    VendorStatus.ACTIVE),
                vendor("VND-004", "컴포트팬츠",             "최영희", "032-7777-8888", "younghee.choi@comfortpants.co.kr",VendorStatus.ACTIVE),
                vendor("VND-005", "니트하우스",             "정도현", "051-4444-2222", "dohyun.jung@knithouse.co.kr",     VendorStatus.ACTIVE),
                vendor("VND-006", "어반아우터",             "한지윤", "02-2222-1111",  "jiyoon.han@urbanouter.co.kr",     VendorStatus.ACTIVE),
                vendor("VND-007", "(주)패딩프로",           "임도현", "053-3333-4444", "dohyun.lim@paddingpro.co.kr",     VendorStatus.ACTIVE),
                vendor("VND-008", "트렌디드레스",           "윤서연", "02-8888-9999",  "seoyeon.yoon@trendydress.co.kr",  VendorStatus.ACTIVE),
                vendor("VND-009", "슈즈크래프트",           "강민호", "031-2222-3333", "minho.kang@shoecraft.co.kr",      VendorStatus.ACTIVE),
                vendor("VND-010", "캐주얼백",               "송지원", "02-7777-1111",  "jiwon.song@casualbag.co.kr",      VendorStatus.ACTIVE),
                vendor("VND-011", "베이직삭스",             "노승현", "042-5555-6666", "seunghyun.noh@basicsocks.co.kr",  VendorStatus.ACTIVE),
                vendor("VND-012", "헤리티지스카프",         "신유정", "02-3333-7777",  "yujung.shin@heritagescarf.co.kr", VendorStatus.INACTIVE)
        );

        vendorRepository.saveAll(seed);
        log.info("[VendorSeedRunner] vendor 시드 {}건 INSERT 완료", seed.size());
    }

    private Vendor vendor(String code, String name, String contactName, String contactPhone,
                           String contactEmail, VendorStatus status) {
        return Vendor.builder()
                .code(code)
                .name(name)
                .contactName(contactName)
                .contactPhone(contactPhone)
                .contactEmail(contactEmail)
                .status(status)
                .build();
    }
}
