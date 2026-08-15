package com.paper.mes.ai.service;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.config.AiProvider;
import com.paper.mes.ai.model.AiModelClient;
import com.paper.mes.ai.model.AiModelPrompt;
import com.paper.mes.ai.model.AiOutputGuard;
import com.paper.mes.ai.memory.ProjectMemoryContextSelector;
import com.paper.mes.ai.memory.ProjectMemoryDocumentProvider;
import com.paper.mes.ai.rule.AiRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiAnswerComposer {

    private final AiProperties properties;
    private final AiModelClient modelClient;
    private final AiOutputGuard outputGuard;
    private final ProjectMemoryDocumentProvider memoryProvider;
    private final ProjectMemoryContextSelector memorySelector;

    public AiAnswerComposer(AiProperties properties, AiModelClient modelClient, AiOutputGuard outputGuard) {
        this(properties, modelClient, outputGuard, null, new ProjectMemoryContextSelector());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AiAnswerComposer(AiProperties properties, AiModelClient modelClient, AiOutputGuard outputGuard,
                            ProjectMemoryDocumentProvider memoryProvider,
                            ProjectMemoryContextSelector memorySelector) {
        this.properties = properties;
        this.modelClient = modelClient;
        this.outputGuard = outputGuard;
        this.memoryProvider = memoryProvider;
        this.memorySelector = memorySelector;
    }

    public Result compose(String pageTemplate, AiRule selected) {
        return compose(pageTemplate, selected, "");
    }

    public Result compose(String pageTemplate, AiRule selected, String question) {
        if (properties.providerMode() != AiProvider.ZHIPU) {
            return new Result(selected.answer(), "LOCAL_RULES");
        }
        List<String> ruleIds = List.of(selected.ruleId());
        String memory = memoryProvider == null ? "" : memoryProvider.current()
                .map(snapshot -> memorySelector.select(snapshot, question, pageTemplate, 12_000)).orElse("");
        var prompt = new AiModelPrompt(pageTemplate, ruleIds, selected.title(),
                selected.answer(), selected.safeNextSteps(), memory);
        return modelClient.rewrite(prompt)
                .filter(result -> outputGuard.accepts(result, ruleIds))
                .map(result -> new Result(result.answer(), "ZHIPU"))
                .orElseGet(() -> new Result(selected.answer(), "LOCAL_RULES"));
    }

    public record Result(String answer, String provider) {
    }
}
