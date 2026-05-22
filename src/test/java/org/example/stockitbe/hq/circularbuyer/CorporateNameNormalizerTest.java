package org.example.stockitbe.hq.circularbuyer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorporateNameNormalizerTest {

    @Test
    void stripLeadingMarker_removesKnownPrefixes() {
        assertThat(CorporateNameNormalizer.stripLeadingMarker("(주) 스토커즈")).isEqualTo("스토커즈");
        assertThat(CorporateNameNormalizer.stripLeadingMarker("주식회사 스토커즈")).isEqualTo("스토커즈");
        assertThat(CorporateNameNormalizer.stripLeadingMarker("(사) 그린텍")).isEqualTo("그린텍");
        assertThat(CorporateNameNormalizer.stripLeadingMarker("(유) 에코랩")).isEqualTo("에코랩");
        assertThat(CorporateNameNormalizer.stripLeadingMarker("㈜ 한빛")).isEqualTo("한빛");
    }

    @Test
    void stripLeadingMarker_keepsRegularName() {
        assertThat(CorporateNameNormalizer.stripLeadingMarker("홍익플러스")).isEqualTo("홍익플러스");
    }
}
