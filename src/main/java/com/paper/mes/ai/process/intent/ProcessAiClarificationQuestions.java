package com.paper.mes.ai.process.intent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;

final class ProcessAiClarificationQuestions {

    private ProcessAiClarificationQuestions() {
    }

    static LinkedHashSet<String> withoutSourceBinding(List<String> questions) {
        return without(questions, value -> value.contains("被多组工艺重复引用")
                && value.contains("唯一匹配母卷行"));
    }

    static LinkedHashSet<String> withoutGroupedPieceSplit(List<String> questions) {
        return without(questions, value -> value.contains("是合并录入的")
                && value.contains("同一母卷记录只能保存一套工艺"));
    }

    static LinkedHashSet<String> withoutSawRemainder(List<String> questions) {
        return without(questions, value -> value.contains("剩余")
                && value.contains("作为切边余料")
                && value.contains("保留为")
                && value.contains("成品"));
    }

    static boolean needsClarification(ProcessAiExtractionResult result,
                                      LinkedHashSet<String> questions) {
        return !result.conflicts().isEmpty() || !questions.isEmpty();
    }

    private static LinkedHashSet<String> without(List<String> questions,
                                                  Predicate<String> obsolete) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        questions.stream().filter(value -> !obsolete.test(value)).forEach(result::add);
        return result;
    }
}
