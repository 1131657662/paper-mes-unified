package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectMemoryCandidateDocumentValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProjectMemoryCandidateDocumentValidator validator =
            new ProjectMemoryCandidateDocumentValidator();

    @Test
    void acceptsTheGeneratedTermShape() throws Exception {
        var candidate = mapper.readTree("""
                {"type":"TERM","scope":"REWIND","status":"ACTIVE",
                 "phrase":"复卷","aliases":[],"intent":"CHANGE_WIDTH",
                 "meaning":"已确认客户用语","source":"confirmed-ai-candidate"}
                """);

        assertThatCode(() -> validator.validate("TERM", candidate)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAliasesThatContainStructuredOrOversizedContent() throws Exception {
        var candidate = mapper.readTree("""
                {"type":"TERM","scope":"REWIND","status":"ACTIVE",
                 "phrase":"复卷","aliases":[{"raw":"customer"}],"intent":"CHANGE_WIDTH",
                 "meaning":"已确认客户用语","source":"confirmed-ai-candidate"}
                """);

        assertThatThrownBy(() -> validator.validate("TERM", candidate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("候选知识字段或内容不符合安全契约");
    }

    @Test
    void rejectsSensitiveTextInAdministratorEditedCandidate() throws Exception {
        var candidate = mapper.readTree("""
                {"type":"TERM","scope":"REWIND","status":"ACTIVE",
                 "phrase":"客户名称:某纸业有限公司","aliases":[],"intent":"CHANGE_WIDTH",
                 "meaning":"已确认客户用语","source":"confirmed-ai-candidate"}
                """);

        assertThatThrownBy(() -> validator.validate("TERM", candidate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("候选知识字段或内容不符合安全契约");
    }

    @Test
    void acceptsStructuredNumericCandidateValuesThatAreNotSensitive() throws Exception {
        var candidate = mapper.readTree("""
                {"type":"EXAMPLE","scope":"REWIND","status":"ACTIVE",
                 "input":"rewindIntent/widthRule/values=[1200,800]",
                 "expected":{"processType":"REWIND","intent":"WIDTH_VALUES","field":"rewindIntent/widthRule/values"},
                 "evidenceRequired":true,"source":"confirmed-ai-candidate"}
                """);

        assertThatCode(() -> validator.validate("EXAMPLE", candidate)).doesNotThrowAnyException();
    }

    @Test
    void rejectsSensitiveHistoricalCandidateTextBeforeItCanBeListed() throws Exception {
        var candidate = mapper.readTree("""
                {"type":"RULE","status":"ACTIVE","content":"订单号:123456789"}
                """);

        assertThatThrownBy(() -> validator.validateSharedText(candidate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("候选知识字段或内容不符合安全契约");
    }
}
