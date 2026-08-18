package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ProcessAiSawRemainderResolver {

    private static final Pattern FINISH_REMAINDER = Pattern.compile(
            "(?:剩余|剩下|余下|余料|边料).{0,12}(?:保留|成品|也要|小卷)"
                    + "|(?:作为|算作|保留为).{0,8}成品|keep.{0,12}(?:finish|product)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TRIM_REMAINDER = Pattern.compile(
            "切边|修边|余料|边料|废料|损耗|(?:剩余|剩下|余下).{0,8}(?:不要|丢弃|舍弃)"
                    + "|trim|scrap|waste",
            Pattern.CASE_INSENSITIVE);

    public ProcessAiExtractionResult resolve(ProcessAiExtractionResult result,
                                             ProcessAiOrderContext context,
                                             String currentRequirement) {
        Map<String, ProcessAiRollContext> rolls = context.rolls().stream()
                .collect(Collectors.toMap(ProcessAiRollContext::shortRef, Function.identity()));
        List<Underfill> underfills = result.assignments().stream()
                .map(assignment -> underfill(assignment, rolls.get(assignment.ownerRollRef())))
                .filter(value -> value != null)
                .toList();
        if (underfills.isEmpty()) return result;
        boolean useGlobalInstruction = underfills.stream()
                .map(Underfill::signature).distinct().count() == 1;
        Map<String, Resolution> resolutions = underfills.stream().collect(Collectors.toMap(
                value -> value.assignment().ownerRollRef(),
                value -> resolve(value, currentRequirement, useGlobalInstruction),
                (left, right) -> left, LinkedHashMap::new));
        return resolvedResult(result, resolutions);
    }

    private Underfill underfill(ProcessAiAssignment assignment, ProcessAiRollContext roll) {
        ProcessAiSawIntent intent = assignment.sawIntent();
        if (roll == null || intent == null || !"EXPLICIT_WIDTHS".equals(intent.type())
                || intent.widths() == null || intent.widths().isEmpty()
                || roll.originalWidth() == null) return null;
        int used = intent.widths().stream().mapToInt(Integer::intValue).sum();
        if (used >= roll.originalWidth()) return null;
        return new Underfill(assignment, new Signature(
                roll.originalWidth(), used, roll.originalWidth() - used, intent.widths()));
    }

    private Resolution resolve(Underfill value, String currentRequirement,
                               boolean useGlobalInstruction) {
        RemainderDecision decision = decision(evidenceText(value.assignment()));
        if (decision == RemainderDecision.UNSPECIFIED && useGlobalInstruction) {
            decision = decision(currentRequirement);
        }
        if (decision == RemainderDecision.FINISH) {
            return new Resolution(withFinishRemainder(value), value.signature(), false);
        }
        return new Resolution(value.assignment(), value.signature(),
                decision == RemainderDecision.UNSPECIFIED);
    }

    private ProcessAiExtractionResult resolvedResult(
            ProcessAiExtractionResult result, Map<String, Resolution> resolutions) {
        List<ProcessAiAssignment> assignments = result.assignments().stream()
                .map(value -> resolutions.getOrDefault(value.ownerRollRef(),
                        new Resolution(value, null, false)).assignment())
                .toList();
        LinkedHashSet<String> questions = ProcessAiClarificationQuestions
                .withoutSawRemainder(result.clarificationQuestions());
        unresolvedGroups(resolutions).forEach((signature, refs) ->
                questions.add(question(refs, signature)));
        return new ProcessAiExtractionResult(
                result.parseId(), result.schemaVersion(), assignments,
                result.unmappedText(), result.conflicts(),
                ProcessAiClarificationQuestions.needsClarification(result, questions),
                List.copyOf(questions));
    }

    private Map<Signature, List<String>> unresolvedGroups(Map<String, Resolution> resolutions) {
        Map<Signature, List<String>> result = new LinkedHashMap<>();
        resolutions.forEach((ref, resolution) -> {
            if (resolution.needsClarification()) {
                result.computeIfAbsent(resolution.signature(), ignored -> new ArrayList<>()).add(ref);
            }
        });
        return result;
    }

    private ProcessAiAssignment withFinishRemainder(Underfill value) {
        ProcessAiSawIntent intent = value.assignment().sawIntent();
        List<Integer> widths = new ArrayList<>(intent.widths());
        widths.add(value.signature().remainderWidth());
        ProcessAiSawIntent normalized = new ProcessAiSawIntent(
                intent.type(), intent.knifeCount(), widths, intent.unit());
        ProcessAiAssignment assignment = value.assignment();
        return new ProcessAiAssignment(
                assignment.sourceRollRefs(), assignment.ownerRollRef(), assignment.coveredRollRefs(),
                assignment.processType(), assignment.rewindIntent(), normalized,
                assignment.ancillaryRequirements(), assignment.evidence());
    }

    private RemainderDecision decision(String text) {
        if (text == null || text.isBlank()) return RemainderDecision.UNSPECIFIED;
        if (FINISH_REMAINDER.matcher(text).find()) return RemainderDecision.FINISH;
        if (TRIM_REMAINDER.matcher(text).find()) return RemainderDecision.TRIM;
        return RemainderDecision.UNSPECIFIED;
    }

    private String evidenceText(ProcessAiAssignment assignment) {
        return assignment.evidence().stream().map(ProcessAiEvidence::text)
                .collect(Collectors.joining("\n"));
    }

    private String question(List<String> refs, Signature value) {
        String widths = value.widths().stream().map(width -> width + "mm")
                .collect(Collectors.joining(" + "));
        return String.join("、", refs) + "：" + value.sourceWidth() + "mm母卷切成" + widths
                + "后，剩余" + value.remainderWidth()
                + "mm是作为切边余料，还是保留为" + value.remainderWidth() + "mm成品？";
    }

    private enum RemainderDecision { TRIM, FINISH, UNSPECIFIED }

    private record Signature(int sourceWidth, int usedWidth, int remainderWidth,
                             List<Integer> widths) {
        private Signature {
            widths = List.copyOf(widths);
        }
    }

    private record Underfill(ProcessAiAssignment assignment, Signature signature) {
    }

    private record Resolution(ProcessAiAssignment assignment, Signature signature,
                              boolean needsClarification) {
    }
}
