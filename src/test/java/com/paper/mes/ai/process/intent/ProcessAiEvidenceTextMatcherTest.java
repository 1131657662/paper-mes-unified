package com.paper.mes.ai.process.intent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiEvidenceTextMatcherTest {

    @Test
    void numericFragmentDoesNotMatchPartOfALargerNumber() {
        assertThat(ProcessAiEvidenceTextMatcher.contains("克重800g", "80")).isFalse();
    }

    @Test
    void numericFragmentMatchesAPunctuationBoundedToken() {
        assertThat(ProcessAiEvidenceTextMatcher.contains("克重：80g", "80")).isTrue();
    }

    @Test
    void nonNumericFragmentRemainsAnExactContainedPhrase() {
        assertThat(ProcessAiEvidenceTextMatcher.contains("全部剥破损包装", "剥破损包装")).isTrue();
    }
}
