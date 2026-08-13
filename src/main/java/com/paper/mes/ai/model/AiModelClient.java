package com.paper.mes.ai.model;

import java.util.Optional;

public interface AiModelClient {

    Optional<AiModelResult> rewrite(AiModelPrompt prompt);
}
