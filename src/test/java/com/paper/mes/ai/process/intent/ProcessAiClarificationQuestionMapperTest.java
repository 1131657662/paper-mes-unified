package com.paper.mes.ai.process.intent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessAiClarificationQuestionMapperTest {

    @Test
    void mapsRemainderQuestionToStableBoundedOptions() {
        String text = "R1：2400mm母卷切成800mm后，剩余800mm是作为切边余料，还是保留为800mm成品？";

        ProcessAiClarificationQuestion first = ProcessAiClarificationQuestionMapper
                .fromExtraction(List.of(text), 4).getFirst();
        ProcessAiClarificationQuestion second = ProcessAiClarificationQuestionMapper
                .fromExtraction(List.of(text), 4).getFirst();

        assertThat(first.questionId()).isEqualTo(second.questionId());
        assertThat(first.field()).isEqualTo("remainderPolicy");
        assertThat(first.parseRevision()).isEqualTo(4);
        assertThat(first.options()).extracting(ProcessAiClarificationOption::code)
                .containsExactly("TRIM", "FINISH");
        assertThat(first.allowUnknown()).isFalse();
    }

    @Test
    void mapsUnclassifiedQuestionToTextOptionWithoutTrustingArbitraryFieldNames() {
        ProcessAiClarificationQuestion question = ProcessAiClarificationQuestionMapper
                .fromExtraction(List.of("请说明这组工艺对应哪条母卷"), 2).getFirst();

        assertThat(question.field()).isEqualTo("clarification");
        assertThat(question.options()).extracting(ProcessAiClarificationOption::code)
                .containsExactly("ANSWER_TEXT");
        assertThat(question.allowUnknown()).isTrue();
    }

    @Test
    void mapsQuantityScopeQuestionToStructuredOptions() {
        ProcessAiClarificationQuestion question = ProcessAiClarificationQuestionMapper
                .fromExtraction(List.of("800×3 是每条母卷分别计算，还是全单合计？"), 3).getFirst();

        assertThat(question.field()).isEqualTo("quantityScope");
        assertThat(question.options()).extracting(ProcessAiClarificationOption::code)
                .containsExactly("PER_SOURCE", "TOTAL");
    }

    @Test
    void givesDuplicateQuestionTextsDistinctIdsWithinOneRevision() {
        List<ProcessAiClarificationQuestion> questions = ProcessAiClarificationQuestionMapper
                .fromExtraction(List.of("请补充门幅", "请补充门幅"), 5);

        assertThat(questions).extracting(ProcessAiClarificationQuestion::questionId)
                .doesNotHaveDuplicates();
        assertThat(questions).allMatch(question -> question.parseRevision() == 5);
    }
}
