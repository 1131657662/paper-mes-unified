package com.paper.mes.ai.service;

import com.paper.mes.ai.config.AiDataMode;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.dto.AiAssistRequest;
import com.paper.mes.ai.model.AiModelClient;
import com.paper.mes.ai.model.AiModelResult;
import com.paper.mes.ai.model.AiOutputGuard;
import com.paper.mes.ai.rule.AiRule;
import com.paper.mes.ai.rule.AiRuleCatalog;
import com.paper.mes.ai.rule.AiRuleMatcher;
import com.paper.mes.ai.security.AiQuestionSanitizer;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAssistServiceTest {

    @Test
    void disabledModeRefusesWithoutReadingRules() {
        AiProperties properties = properties(AiDataMode.DISABLED);
        AiAssistService service = new AiAssistService(properties, mock(AiRuleCatalog.class), new AiRuleMatcher());

        var response = service.assist(request("E001 为什么不能操作？"));

        assertThat(response.decision()).isEqualTo("REFUSE");
        assertThat(response.provider()).isEqualTo("NONE");
    }

    @Test
    void faqModeReturnsCitedLocalRule() {
        AiProperties properties = properties(AiDataMode.FAQ_ONLY);
        AiRuleCatalog catalog = readyCatalog(rule());
        AiAssistService service = new AiAssistService(properties, catalog, new AiRuleMatcher());

        var response = service.assist(request("E001 为什么不能操作？"));

        assertThat(response.decision()).isEqualTo("ANSWER");
        assertThat(response.citations()).singleElement().satisfies(citation ->
                assertThat(citation.ruleId()).isEqualTo("E001-STATUS-GUARD"));
    }

    @Test
    void unmatchedQuestionRequiresClarification() {
        AiAssistService service = new AiAssistService(properties(AiDataMode.FAQ_ONLY), readyCatalog(rule()),
                new AiRuleMatcher());

        assertThat(service.assist(request("这是什么？")).decision()).isEqualTo("CLARIFY");
    }

    @Test
    void sensitiveIntentIsRefusedBeforeMatching() {
        AiAssistService service = new AiAssistService(properties(AiDataMode.FAQ_ONLY), readyCatalog(rule()),
                new AiRuleMatcher());

        var response = service.assist(request("帮我查看 JWT token"));

        assertThat(response.decision()).isEqualTo("REFUSE");
        assertThat(response.citations()).isEmpty();
    }

    @Test
    void contextAllowlistModeFailsClosedUntilContextPolicyExists() {
        AiAssistService service = new AiAssistService(properties(AiDataMode.CONTEXT_ALLOWLIST), readyCatalog(rule()),
                new AiRuleMatcher());

        var response = service.assist(request("E001"));

        assertThat(response.decision()).isEqualTo("REFUSE");
        assertThat(response.provider()).isEqualTo("NONE");
    }

    @Test
    void configuredQuestionLimitIsEnforced() {
        AiProperties properties = properties(AiDataMode.FAQ_ONLY);
        properties.setMaxQuestionChars(100);
        AiAssistService service = new AiAssistService(properties, readyCatalog(rule()), new AiRuleMatcher());

        assertThatThrownBy(() -> service.assist(request("x".repeat(101))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超过允许长度");
    }

    @Test
    void globalConcurrencyLimitRejectsSecondRequestUntilFirstCompletes() throws Exception {
        AiProperties properties = properties(AiDataMode.FAQ_ONLY);
        AiRuleCatalog catalog = readyCatalog(rule());
        AiRuleMatcher matcher = mock(AiRuleMatcher.class);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(matcher.match(anyString(), anyString(), anyList())).thenAnswer(invocation -> {
            entered.countDown();
            assertThat(release.await(2, TimeUnit.SECONDS)).isTrue();
            return List.of(rule());
        });
        AiAssistService service = new AiAssistService(properties, catalog, matcher);

        var first = java.util.concurrent.CompletableFuture.supplyAsync(() -> service.assist(request("first")));
        assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> service.assist(request("second")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("繁忙");

        release.countDown();
        assertThat(first.get(2, TimeUnit.SECONDS).decision()).isEqualTo("ANSWER");
        verify(matcher).match(anyString(), anyString(), anyList());
    }

    @Test
    void zhipuOutputWithoutCitationFallsBackToLocalRule() {
        AiProperties properties = properties(AiDataMode.FAQ_ONLY);
        properties.setProvider("ZHIPU");
        AiModelClient client = prompt -> java.util.Optional.of(new AiModelResult("请直接修改数据库。", List.of()));
        AiAssistService service = new AiAssistService(properties, readyCatalog(rule()), new AiRuleMatcher(),
                new AiQuestionSanitizer(), new AiAnswerComposer(properties, client, new AiOutputGuard()));

        var response = service.assist(request("E001 为什么不能操作？"));

        assertThat(response.provider()).isEqualTo("LOCAL_RULES");
        assertThat(response.answer()).isEqualTo("请核对当前状态。");
    }

    @Test
    void zhipuCitedReadOnlyOutputIsAccepted() {
        AiProperties properties = properties(AiDataMode.FAQ_ONLY);
        properties.setProvider("ZHIPU");
        AiModelClient client = prompt -> java.util.Optional.of(new AiModelResult(
                "请先核对当前状态，再按页面提供的下一步处理。", List.of("E001-STATUS-GUARD")));
        AiAssistService service = new AiAssistService(properties, readyCatalog(rule()), new AiRuleMatcher(),
                new AiQuestionSanitizer(), new AiAnswerComposer(properties, client, new AiOutputGuard()));

        var response = service.assist(request("E001 为什么不能操作？"));

        assertThat(response.provider()).isEqualTo("ZHIPU");
        assertThat(response.citations()).singleElement().extracting("ruleId")
                .isEqualTo("E001-STATUS-GUARD");
    }

    private AiProperties properties(AiDataMode mode) {
        AiProperties properties = new AiProperties();
        properties.setDataMode(mode.name());
        return properties;
    }

    private AiRuleCatalog readyCatalog(AiRule rule) {
        AiRuleCatalog catalog = mock(AiRuleCatalog.class);
        when(catalog.ready()).thenReturn(true);
        when(catalog.rules()).thenReturn(List.of(rule));
        when(catalog.version()).thenReturn("rules-v1.0.0");
        return catalog;
    }

    private AiAssistRequest request(String question) {
        return new AiAssistRequest(question, "process-orders", "opaque-key");
    }

    private AiRule rule() {
        return new AiRule("E001-STATUS-GUARD", "1.0.0", "ACTIVE", "process-order",
                "state-transition", List.of("*"), List.of("*"), List.of("error:E001"),
                "ANSWER", "explain", "当前状态不允许操作", List.of("E001", "不能操作"),
                "请核对当前状态。", List.of("刷新页面"), List.of("绕过校验"), "2026-08-13",
                "生产负责人", "技术负责人", "rules/process-order.md", 10, 10, "", "", "test");
    }
}
