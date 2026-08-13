package com.paper.mes.ai.service;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.paper.mes.ai.model.AiModelClient;
import com.paper.mes.ai.model.AiModelPrompt;
import com.paper.mes.ai.model.AiOutputGuard;
import com.paper.mes.ai.rule.AiRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiAnswerComposer {

    private final AiProperties properties;
    private final AiModelClient modelClient;
    private final AiOutputGuard outputGuard;

    public AiAnswerComposer(AiProperties properties, AiModelClient modelClient, AiOutputGuard outputGuard) {
        this.properties = properties;
        this.modelClient = modelClient;
        this.outputGuard = outputGuard;
    }

    public Result compose(String pageTemplate, AiRule selected) {
        if (properties.providerMode() != AiProvider.ZHIPU) {
            return new Result(selected.answer(), "LOCAL_RULES");
        }
        List<String> ruleIds = List.of(selected.ruleId());
        var prompt = new AiModelPrompt(pageTemplate, ruleIds, selected.title(),
                selected.answer(), selected.safeNextSteps());
        return modelClient.rewrite(prompt)
                .filter(result -> outputGuard.accepts(result, ruleIds))
                .map(result -> new Result(result.answer(), "ZHIPU"))
                .orElseGet(() -> new Result(selected.answer(), "LOCAL_RULES"));
    }

    public record Result(String answer, String provider) {
    }
}
