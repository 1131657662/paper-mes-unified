package com.paper.mes.ai.service;

import com.paper.mes.ai.config.AiDataMode;
import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.dto.AiAssistRequest;
import com.paper.mes.ai.dto.AiAssistResponse;
import com.paper.mes.ai.dto.AiCitation;
import com.paper.mes.ai.dto.AiStatusResponse;
import com.paper.mes.ai.rule.AiRule;
import com.paper.mes.ai.rule.AiRuleCatalog;
import com.paper.mes.ai.rule.AiRuleMatcher;
import com.paper.mes.ai.security.AiQuestionSanitizer;
import com.paper.mes.common.RequestIdContext;
import com.paper.mes.common.ResultCode;
import com.paper.mes.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
public class AiAssistService {

    private final AiProperties properties;
    private final AiRuleCatalog catalog;
    private final AiRuleMatcher matcher;
    private final AiQuestionSanitizer sanitizer;
    private final AiAnswerComposer answerComposer;
    private final Semaphore permits;

    public AiAssistService(AiProperties properties, AiRuleCatalog catalog, AiRuleMatcher matcher) {
        this(properties, catalog, matcher, new AiQuestionSanitizer(),
                new AiAnswerComposer(properties, prompt -> java.util.Optional.empty(),
                        new com.paper.mes.ai.model.AiOutputGuard()));
    }

    @Autowired
    public AiAssistService(AiProperties properties, AiRuleCatalog catalog, AiRuleMatcher matcher,
                           AiQuestionSanitizer sanitizer, AiAnswerComposer answerComposer) {
        this.properties = properties;
        this.catalog = catalog;
        this.matcher = matcher;
        this.sanitizer = sanitizer;
        this.answerComposer = answerComposer;
        this.permits = new Semaphore(properties.getGlobalConcurrentRequests());
    }

    public AiAssistResponse assist(AiAssistRequest request) {
        acquirePermit();
        try {
            return answer(request);
        } finally {
            permits.release();
        }
    }

    public AiStatusResponse status() {
        return new AiStatusResponse(properties.enabled(), properties.mode(), catalog.version(), catalog.ready(),
                properties.effectiveProvider());
    }

    private AiAssistResponse answer(AiAssistRequest request) {
        String requestId = RequestIdContext.current();
        AiDataMode mode = properties.mode();
        if (mode == AiDataMode.DISABLED) {
            return response(requestId, "REFUSE", "NONE", "智能助手当前未启用。", List.of(), "NONE");
        }
        if (mode != AiDataMode.FAQ_ONLY) {
            return response(requestId, "REFUSE", "NONE", "当前上下文模式尚未开放，暂不读取业务数据。", List.of(), "NONE");
        }
        if (request.question().length() > properties.getMaxQuestionChars()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "AI_QUESTION_TOO_LONG", "问题内容超过允许长度");
        }
        if (!catalog.ready()) {
            log.error("AI rules unavailable: requestId={}", requestId);
            return response(requestId, "REFUSE", "NONE", "本地规则暂不可用，请联系管理员。", List.of());
        }
        AiQuestionSanitizer.Result inspection = sanitizer.inspect(request.question());
        if (!inspection.allowed()) {
            return response(requestId, "REFUSE", "NONE", inspection.reason(), List.of(), "NONE");
        }
        List<AiRule> matched = matcher.match(inspection.sanitizedQuestion(), request.pageTemplate(), catalog.rules());
        if (matched.isEmpty()) {
            return response(requestId, "CLARIFY", "LOW", "当前规则库不足以安全判断。请补充错误码、页面或业务状态。", List.of());
        }
        AiRule selected = matched.getFirst();
        AiAnswerComposer.Result answer = answerComposer.compose(request.pageTemplate(), selected,
                inspection.sanitizedQuestion());
        return response(requestId, selected.decision(), "HIGH", answer, selected);
    }

    private AiAssistResponse response(String requestId, String decision, String confidence,
                                      String answer, List<String> steps) {
        return response(requestId, decision, confidence, answer, steps, "LOCAL_RULES");
    }

    private AiAssistResponse response(String requestId, String decision, String confidence,
                                      String answer, List<String> steps, String provider) {
        return new AiAssistResponse(requestId, decision, confidence, answer, steps, List.of(),
                properties.mode(), provider);
    }

    private AiAssistResponse response(String requestId, String decision, String confidence,
                                      String answer, AiRule rule) {
        return new AiAssistResponse(requestId, decision, confidence, answer, rule.safeNextSteps(),
                List.of(new AiCitation(rule.ruleId(), rule.title(), rule.version())), properties.mode(), "LOCAL_RULES");
    }

    private AiAssistResponse response(String requestId, String decision, String confidence,
                                      AiAnswerComposer.Result answer, AiRule rule) {
        return new AiAssistResponse(requestId, decision, confidence, answer.answer(), rule.safeNextSteps(),
                List.of(new AiCitation(rule.ruleId(), rule.title(), rule.version())), properties.mode(), answer.provider());
    }

    private void acquirePermit() {
        if (!permits.tryAcquire()) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "AI_BUSY", "智能助手当前繁忙，请稍后重试");
        }
    }

}
