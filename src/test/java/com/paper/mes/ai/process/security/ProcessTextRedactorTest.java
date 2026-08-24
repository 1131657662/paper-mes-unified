package com.paper.mes.ai.process.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessTextRedactorTest {

    private final ProcessTextRedactor redactor = new ProcessTextRedactor();

    @Test
    void redactKeepsProcessDimensionsAndExtractsPackagingPriceLocally() {
        ProcessTextRedactionResult result = redactor.redact(
                "2000mm切2刀，直径1200mm，包膜加20元每件");

        assertThat(result.sanitizedText())
                .isEqualTo("2000mm切2刀，直径1200mm，包膜加[金额]每件");
        assertThat(result.charges()).containsExactly(
                new ProcessTextRedactor.ExtractedCharge(new java.math.BigDecimal("20"), "PIECE"));
    }

    @Test
    void redactExtractsPieceUnitWhenUnitPrecedesPrice() {
        ProcessTextRedactionResult result = redactor.redact("剥破损包装，每件20元");

        assertThat(result.sanitizedText()).isEqualTo("剥破损包装，每件[金额]");
        assertThat(result.charges()).containsExactly(
                new ProcessTextRedactor.ExtractedCharge(new java.math.BigDecimal("20"), "PIECE"));
    }

    @Test
    void redactRemovesIdentifiersAndCredentialsWithoutLoggingOrRejectingTheRequest() {
        ProcessTextRedactionResult result = redactor.redact(
                "电话13800138000，编号202608120003，api key=sk-1234567890123456，邮箱a@example.com");

        assertThat(result.sanitizedText())
                .doesNotContain("13800138000", "202608120003", "sk-1234567890123456", "a@example.com")
                .contains("[手机号已隐藏]", "[编号已隐藏]", "[密钥已隐藏]", "[邮箱已隐藏]");
    }

    @Test
    void redactRemovesNamedIdentityAndAddressButKeepsProcessDimensions() {
        String input = "\u8054\u7cfb\u4eba:\u5f20\u4e09,"
                + "\u516c\u53f8\u540d\u79f0:\u6d77\u6d0b\u7eb8\u4e1a,"
                + "\u6536\u8d27\u5730\u5740:\u5b81\u6ce2\u5e02\u6d77\u66d9\u533a\u67d0\u67d0\u8def88\u53f7,"
                + "2000mm \u52072\u5200";

        ProcessTextRedactionResult result = redactor.redact(input);

        assertThat(result.sanitizedText())
                .doesNotContain("\u5f20\u4e09", "\u6d77\u6d0b\u7eb8\u4e1a", "\u67d0\u67d0\u8def88\u53f7")
                .contains("[identity redacted]", "[address redacted]", "2000mm", "\u52072\u5200");
    }

    @Test
    void redactRemovesUnlabelledOrganizationWithoutRemovingProcessParameters() {
        ProcessTextRedactionResult result = redactor.redact(
                "给宁波某某纸业加工，门幅2000mm，直径1200mm");

        assertThat(result.sanitizedText())
                .doesNotContain("宁波某某纸业")
                .contains("[identity redacted]", "2000mm", "1200mm");
    }
}
