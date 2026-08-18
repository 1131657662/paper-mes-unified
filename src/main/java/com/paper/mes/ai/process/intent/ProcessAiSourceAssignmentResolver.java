package com.paper.mes.ai.process.intent;

import com.paper.mes.ai.process.context.ProcessAiOrderContext;
import com.paper.mes.ai.process.context.ProcessAiRollContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ProcessAiSourceAssignmentResolver {

    private static final Pattern PIECE_COUNT = Pattern.compile("(?<!\\d)(\\d{1,3})\\s*(?:件|卷)");

    public ProcessAiExtractionResult resolve(ProcessAiExtractionResult result,
                                             ProcessAiOrderContext context) {
        Map<String, ProcessAiRollContext> rolls = context.rolls().stream()
                .collect(Collectors.toMap(ProcessAiRollContext::shortRef, Function.identity()));
        LinkedHashSet<String> questions = ProcessAiClarificationQuestions
                .withoutSourceBinding(result.clarificationQuestions());
        List<ProcessAiAssignment> expanded = expandCombinedRecipes(
                result.assignments(), rolls, questions);
        Set<String> duplicatedSources = duplicatedSources(expanded);
        if (duplicatedSources.isEmpty() && expanded.equals(result.assignments())) return result;

        Set<String> reserved = initiallyReserved(expanded, duplicatedSources);
        List<ProcessAiAssignment> resolved = new ArrayList<>(expanded.size());
        for (ProcessAiAssignment assignment : expanded) {
            resolved.add(resolveAssignment(assignment, rolls, duplicatedSources, reserved, questions));
        }
        return new ProcessAiExtractionResult(
                result.parseId(), result.schemaVersion(), resolved,
                result.unmappedText(), result.conflicts(),
                ProcessAiClarificationQuestions.needsClarification(result, questions),
                List.copyOf(questions));
    }

    private List<ProcessAiAssignment> expandCombinedRecipes(
            List<ProcessAiAssignment> assignments,
            Map<String, ProcessAiRollContext> rolls,
            LinkedHashSet<String> questions) {
        List<ProcessAiAssignment> result = new ArrayList<>();
        for (ProcessAiAssignment assignment : assignments) {
            List<ProcessAiAssignment> split = splitCombinedRecipe(assignment, rolls);
            if (split == null) {
                result.add(assignment);
            } else {
                result.addAll(split);
            }
        }
        return result;
    }

    private List<ProcessAiAssignment> splitCombinedRecipe(
            ProcessAiAssignment assignment,
            Map<String, ProcessAiRollContext> rolls) {
        if (assignment.sourceRollRefs().size() != 1 || assignment.sawIntent() == null
                || !"EXPLICIT_WIDTHS".equals(assignment.sawIntent().type())
                || assignment.sawIntent().widths() == null) return null;
        ProcessAiRollContext referenced = rolls.get(assignment.ownerRollRef());
        if (referenced == null || referenced.originalWidth() == null
                || assignment.sawIntent().widths().stream().mapToInt(Integer::intValue).sum()
                <= referenced.originalWidth()) return null;
        Map<Integer, Long> recipes = assignment.sawIntent().widths().stream()
                .collect(Collectors.groupingBy(
                        Function.identity(), java.util.LinkedHashMap::new, Collectors.counting()));
        if (recipes.size() < 2) return null;
        List<ProcessAiAssignment> result = new ArrayList<>();
        Set<String> selected = new HashSet<>();
        for (Map.Entry<Integer, Long> recipe : recipes.entrySet()) {
            List<ProcessAiRollContext> candidates = rolls.values().stream()
                    .filter(roll -> !selected.contains(roll.shortRef()))
                    .filter(roll -> sameSourceSignature(referenced, roll))
                    .filter(roll -> roll.pieceNum() != null
                            && roll.pieceNum() == recipe.getValue().intValue())
                    .toList();
            if (candidates.size() != 1) return null;
            ProcessAiRollContext owner = candidates.getFirst();
            selected.add(owner.shortRef());
            ProcessAiSawIntent saw = new ProcessAiSawIntent(
                    assignment.sawIntent().type(), assignment.sawIntent().knifeCount(),
                    List.of(recipe.getKey()), assignment.sawIntent().unit());
            result.add(new ProcessAiAssignment(
                    List.of(owner.shortRef()), owner.shortRef(), List.of(), "SAW",
                    null, saw, assignment.ancillaryRequirements(), assignment.evidence()));
        }
        return result;
    }

    private ProcessAiAssignment resolveAssignment(ProcessAiAssignment assignment,
                                                  Map<String, ProcessAiRollContext> rolls,
                                                  Set<String> duplicatedSources,
                                                  Set<String> reserved,
                                                  LinkedHashSet<String> questions) {
        if (assignment.sourceRollRefs().size() != 1
                || !duplicatedSources.contains(assignment.ownerRollRef())) {
            return assignment;
        }
        ProcessAiRollContext referenced = rolls.get(assignment.ownerRollRef());
        Integer pieceCount = evidencePieceCount(assignment);
        if (referenced == null || pieceCount == null) {
            questions.add(bindingQuestion(assignment.ownerRollRef(), pieceCount));
            return assignment;
        }
        List<ProcessAiRollContext> candidates = rolls.values().stream()
                .filter(roll -> !reserved.contains(roll.shortRef()))
                .filter(roll -> sameSourceSignature(referenced, roll))
                .filter(roll -> pieceCount.equals(roll.pieceNum()))
                .toList();
        if (candidates.size() != 1) {
            questions.add(bindingQuestion(assignment.ownerRollRef(), pieceCount));
            return assignment;
        }
        ProcessAiRollContext selected = candidates.getFirst();
        reserved.add(selected.shortRef());
        return new ProcessAiAssignment(
                List.of(selected.shortRef()), selected.shortRef(), List.of(),
                assignment.processType(), assignment.rewindIntent(), assignment.sawIntent(),
                assignment.ancillaryRequirements(), assignment.evidence());
    }

    private Set<String> duplicatedSources(List<ProcessAiAssignment> assignments) {
        Map<String, Integer> counts = new HashMap<>();
        for (ProcessAiAssignment assignment : assignments) {
            for (String ref : assignment.sourceRollRefs()) counts.merge(ref, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private Set<String> initiallyReserved(List<ProcessAiAssignment> assignments,
                                          Set<String> duplicatedSources) {
        Set<String> result = new HashSet<>();
        for (ProcessAiAssignment assignment : assignments) {
            assignment.sourceRollRefs().stream()
                    .filter(ref -> !duplicatedSources.contains(ref))
                    .forEach(result::add);
        }
        return result;
    }

    private Integer evidencePieceCount(ProcessAiAssignment assignment) {
        LinkedHashSet<Integer> counts = new LinkedHashSet<>();
        for (ProcessAiEvidence item : assignment.evidence()) {
            Matcher matcher = PIECE_COUNT.matcher(item.text());
            while (matcher.find()) counts.add(Integer.parseInt(matcher.group(1)));
        }
        if (counts.size() == 1) return counts.iterator().next();
        if (assignment.sawIntent() == null || assignment.sawIntent().widths() == null
                || assignment.sawIntent().widths().size() <= 1) return null;
        long distinctWidths = assignment.sawIntent().widths().stream().distinct().count();
        return distinctWidths == 1 ? assignment.sawIntent().widths().size() : null;
    }

    private boolean sameSourceSignature(ProcessAiRollContext left, ProcessAiRollContext right) {
        if (!java.util.Objects.equals(left.originalWidth(), right.originalWidth())) return false;
        if (left.paperName() != null && right.paperName() != null
                && !left.paperName().equals(right.paperName())) return false;
        return left.gramWeight() == null || right.gramWeight() == null
                || left.gramWeight().equals(right.gramWeight());
    }

    private String bindingQuestion(String ref, Integer pieceCount) {
        String count = pieceCount == null ? "未识别件数" : pieceCount + "件";
        return ref + " 被多组工艺重复引用，且无法根据门幅和" + count
                + "唯一匹配母卷行。请说明该组对应哪一条母卷。";
    }
}
