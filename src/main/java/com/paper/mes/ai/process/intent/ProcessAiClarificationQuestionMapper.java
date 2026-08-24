package com.paper.mes.ai.process.intent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.IntStream;

/** Converts legacy extraction question strings into the bound clarification protocol. */
public final class ProcessAiClarificationQuestionMapper {

    private ProcessAiClarificationQuestionMapper() {
    }

    public static List<ProcessAiClarificationQuestion> fromExtraction(
            List<String> questions, int parseRevision) {
        if (questions == null || questions.isEmpty()) return List.of();
        return IntStream.range(0, Math.min(8, questions.size()))
                .mapToObj(index -> map(questions.get(index), parseRevision, index))
                .toList();
    }

    private static ProcessAiClarificationQuestion map(String question, int parseRevision, int index) {
        String text = question == null ? "需要补充工艺信息" : question.strip();
        if (text.isEmpty()) text = "需要补充工艺信息";
        if (text.length() > 500) text = text.substring(0, 500);
        if (text.contains("剩余") && text.contains("切边余料") && text.contains("保留为")) {
            return question(text, parseRevision, "remainderPolicy",
                    List.of(new ProcessAiClarificationOption("TRIM", "作为切边余料"),
                            new ProcessAiClarificationOption("FINISH", "保留为成品")), false, index);
        }
        if (text.contains("合并录入")) {
            return question(text, parseRevision, "sourceBinding",
                    List.of(new ProcessAiClarificationOption("SPLIT_SOURCE_ROLLS", "拆分母卷行后重新解析")),
                    true, index);
        }
        if ((text.contains("数量") || text.contains("每条") || text.contains("全单")
                || text.contains("合计") || text.contains("范围"))
                && (text.contains("每条") || text.contains("全单") || text.contains("合计")
                || text.contains("范围"))) {
            return question(text, parseRevision, "quantityScope",
                    List.of(new ProcessAiClarificationOption("PER_SOURCE", "每条母卷分别计算"),
                            new ProcessAiClarificationOption("TOTAL", "全单合计")), true, index);
        }
        return question(text, parseRevision, "clarification",
                List.of(new ProcessAiClarificationOption("ANSWER_TEXT", "补充说明")), true, index);
    }

    private static ProcessAiClarificationQuestion question(
            String text, int revision, String field,
            List<ProcessAiClarificationOption> options, boolean allowUnknown, int index) {
        return new ProcessAiClarificationQuestion(
                "clarification-" + digest(index + ":" + text), field, revision, text, options,
                allowUnknown);
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (Exception ex) {
            throw new IllegalStateException("AI clarification question id is unavailable", ex);
        }
    }
}
