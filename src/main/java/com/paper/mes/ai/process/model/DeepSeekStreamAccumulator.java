package com.paper.mes.ai.process.model;

/** Mutable state for decoding one DeepSeek streaming response. */
final class DeepSeekStreamAccumulator {

    final StringBuilder content = new StringBuilder();
    String model;
    int inputTokens;
    int outputTokens;
    int chunkCount;
    int reasoningCharacters;
    String finishReason;
    boolean sawSseData;

    DeepSeekStreamAccumulator(String model) {
        this.model = model;
    }
}
