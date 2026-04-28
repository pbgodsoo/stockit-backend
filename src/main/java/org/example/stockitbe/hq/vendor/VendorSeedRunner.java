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
                vendor("VND-001", "(주)테크서플라이",     "김민준", "02-1234-5678",  "minjun.kim@techsupply.co.kr",     VendorStatus.ACTIVE),
                vendor("VND-002", "한국생활물산",          "이수연", "031-9876-5432", "suyeon.lee@kliving.co.kr",        VendorStatus.ACTIVE),
                vendor("VND-003", "글로벌오피스",          "박재현", "02-5555-3333",  "jaehyun.park@globaloffice.com",   VendorStatus.ACTIVE),
                vendor("VND-004", "위생물자(주)",          "최영희", "032-7777-8888", "younghee.choi@hygiene.co.kr",     VendorStatus.INACTIVE),
                vendor("VND-005", "스마트주방솔루션",      "정도현", "051-4444-2222", "dohyun.jung@smartkitchen.kr",     VendorStatus.ACTIVE),
                vendor("VND-006", "패션라인(주)",          "한지윤", "02-2222-1111",  "jiyoon.han@fashionline.co.kr",    VendorStatus.ACTIVE),
                vendor("VND-007", "슈즈모아",              "임도현", "053-3333-4444", "dohyun.lim@shoesmoa.com",         VendorStatus.ACTIVE),
                vendor("VND-008", "코스메틱플러스",        "윤서연", "02-8888-9999",  "seoyeon.yoon@cosmeticplus.kr",    VendorStatus.ACTIVE),
                vendor("VND-009", "오피스인테리어(주)",    "강민호", "031-2222-3333", "minho.kang@officeint.co.kr",      VendorStatus.ACTIVE),
                vendor("VND-010", "푸드유통센터",          "송지원", "02-7777-1111",  "jiwon.song@fooddist.co.kr",       VendorStatus.ACTIVE),
                vendor("VND-011", "그린리빙코리아",        "노승현", "042-5555-6666", "seunghyun.noh@greenliving.kr",    VendorStatus.ACTIVE),
                vendor("VND-012", "베스트가전",            "신유정", "02-3333-7777",  "yujung.shin@bestappliance.co.kr", VendorStatus.INACTIVE)
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
