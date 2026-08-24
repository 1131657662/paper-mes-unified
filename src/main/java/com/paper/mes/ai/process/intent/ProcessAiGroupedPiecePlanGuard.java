package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProcessAiGroupedPiecePlanGuard {

    public ProcessAiExtractionResult resolve(ProcessAiExtractionResult result,
                                             ProcessAiOrderContext context) {
        Map<String, ProcessAiRollContext> rolls = context.rolls().stream()
                .collect(Collectors.toMap(ProcessAiRollContext::shortRef, Function.identity()));
        LinkedHashSet<String> questions = ProcessAiClarificationQuestions
                .withoutGroupedPieceSplit(result.clarificationQuestions());
        List<ProcessAiAssignment> assignments = result.assignments().stream()
                .map(value -> normalize(value, rolls.get(value.ownerRollRef()), questions))
                .toList();
        return new ProcessAiExtractionResult(
                result.parseId(), result.schemaVersion(), assignments,
                result.unmappedText(), result.conflicts(),
                ProcessAiClarificationQuestions.needsClarification(result, questions),
                List.copyOf(questions));
    }

    private ProcessAiAssignment normalize(ProcessAiAssignment assignment,
                                          ProcessAiRollContext roll,
                                          LinkedHashSet<String> questions) {
        if (!looksLikePerPieceRecipes(assignment.sawIntent(), roll)) return assignment;
        Map<Integer, Long> counts = widthCounts(assignment.sawIntent().widths());
        if (counts.size() == 1) return withWidths(assignment, List.of(counts.keySet().iterator().next()));
        questions.add(splitQuestion(assignment.ownerRollRef(), roll.pieceNum(), counts));
        return assignment;
    }

    private boolean looksLikePerPieceRecipes(ProcessAiSawIntent intent, ProcessAiRollContext roll) {
        if (roll == null || roll.pieceNum() == null || roll.pieceNum() <= 1
                || roll.originalWidth() == null || intent == null
                || !"EXPLICIT_WIDTHS".equals(intent.type()) || intent.widths() == null) return false;
        if (intent.widths().size() != roll.pieceNum()) return false;
        return intent.widths().stream().allMatch(width -> width <= roll.originalWidth())
                && intent.widths().stream().mapToInt(Integer::intValue).sum() > roll.originalWidth();
    }

    private Map<Integer, Long> widthCounts(List<Integer> widths) {
        return widths.stream().collect(Collectors.groupingBy(
                Function.identity(), LinkedHashMap::new, Collectors.counting()));
    }

    private ProcessAiAssignment withWidths(ProcessAiAssignment assignment, List<Integer> widths) {
        ProcessAiSawIntent source = assignment.sawIntent();
        ProcessAiSawIntent normalized = new ProcessAiSawIntent(
                source.type(), source.knifeCount(), widths, source.unit());
        return new ProcessAiAssignment(
                assignment.sourceRollRefs(), assignment.ownerRollRef(), assignment.coveredRollRefs(),
                assignment.processType(), assignment.processMode(), assignment.rewindIntent(), normalized,
                assignment.ancillaryRequirements(), assignment.evidence(), assignment.customerSpecs());
    }

    private String splitQuestion(String ref, int pieceCount, Map<Integer, Long> counts) {
        String recipes = counts.entrySet().stream()
                .map(entry -> entry.getValue() + "件切" + entry.getKey() + "mm")
                .collect(Collectors.joining("、"));
        return ref + " 是合并录入的 " + pieceCount + " 件母卷，但要求 " + recipes
                + "，同一母卷记录只能保存一套工艺。请返回第2步拆成对应的母卷行后重新解析。";
    }
}
