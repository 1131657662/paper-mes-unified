package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 将历史直发成品稳定匹配到来源母卷，供首次补关系和回退重录使用。 */
final class DirectShipFinishMatcher {

    private static final int ROLL_NO_VOID = 3;

    private DirectShipFinishMatcher() {
    }

    static Map<String, List<FinishRoll>> assign(List<OriginalRoll> sources, List<FinishRoll> finishes,
                                                List<FinishOriginalRel> relations) {
        requireOneSourcePerFinish(finishes, relations);
        Map<String, FinishRoll> finishByUuid = new HashMap<>();
        finishes.forEach(finish -> finishByUuid.put(finish.getUuid(), finish));
        Set<String> sourceIds = new HashSet<>();
        sources.forEach(source -> sourceIds.add(source.getUuid()));
        Map<String, List<FinishRoll>> result = linkedAssignments(sourceIds, finishByUuid, relations);
        List<FinishRoll> available = unlinkedFinishes(finishes, relations);
        assignLegacyMatches(sources, result, available);
        requireNoAmbiguousLegacyFinishes(available);
        requirePieceCapacity(sources, result);
        return result;
    }

    private static void requireOneSourcePerFinish(List<FinishRoll> finishes,
                                                  List<FinishOriginalRel> relations) {
        Set<String> directFinishIds = new HashSet<>();
        finishes.forEach(finish -> directFinishIds.add(finish.getUuid()));
        Map<String, String> sourcesByFinish = new HashMap<>();
        for (FinishOriginalRel relation : relations) {
            if (!directFinishIds.contains(relation.getFinishUuid())) {
                continue;
            }
            String previous = sourcesByFinish.putIfAbsent(
                    relation.getFinishUuid(), relation.getOriginalUuid());
            if (previous != null && !previous.equals(relation.getOriginalUuid())) {
                throw new BusinessException("直发成品不能关联多个来源母卷，请先修复来源数据");
            }
        }
    }

    private static Map<String, List<FinishRoll>> linkedAssignments(Set<String> sourceIds,
                                                                    Map<String, FinishRoll> finishes,
                                                                    List<FinishOriginalRel> relations) {
        Map<String, List<FinishRoll>> result = new LinkedHashMap<>();
        for (FinishOriginalRel relation : relations) {
            FinishRoll finish = finishes.get(relation.getFinishUuid());
            if (sourceIds.contains(relation.getOriginalUuid()) && isActive(finish)) {
                List<FinishRoll> assigned = result.computeIfAbsent(
                        relation.getOriginalUuid(), ignored -> new ArrayList<>());
                if (assigned.stream().noneMatch(row -> row.getUuid().equals(finish.getUuid()))) {
                    assigned.add(finish);
                }
            }
        }
        result.values().forEach(rows -> rows.sort(Comparator.comparing(
                FinishRoll::getRowSort, Comparator.nullsLast(Comparator.naturalOrder()))));
        return result;
    }

    private static List<FinishRoll> unlinkedFinishes(List<FinishRoll> finishes,
                                                     List<FinishOriginalRel> relations) {
        Set<String> relatedIds = new HashSet<>();
        relations.forEach(relation -> relatedIds.add(relation.getFinishUuid()));
        return new ArrayList<>(finishes.stream()
                .filter(DirectShipFinishMatcher::isActive)
                .filter(finish -> !relatedIds.contains(finish.getUuid())).toList());
    }

    private static void assignLegacyMatches(List<OriginalRoll> sources,
                                            Map<String, List<FinishRoll>> assigned,
                                            List<FinishRoll> available) {
        for (OriginalRoll source : sources) {
            List<FinishRoll> current = assigned.computeIfAbsent(source.getUuid(), ignored -> new ArrayList<>());
            int remaining = DirectShipPiecePlan.from(source).count() - current.size();
            if (remaining <= 0) continue;
            boolean uniqueRollNo = StringUtils.hasText(source.getRollNo())
                    && sources.stream().filter(row -> source.getRollNo().equals(row.getRollNo())).count() == 1;
            List<FinishRoll> matches = available.stream()
                    .filter(finish -> legacyMatches(source, finish, uniqueRollNo))
                    .toList();
            if (matches.size() > remaining) {
                throw new BusinessException("历史直发成品存在多个可能来源，不能自动匹配，请先修复来源数据");
            }
            current.addAll(matches);
            available.removeAll(matches);
        }
    }

    private static void requirePieceCapacity(List<OriginalRoll> sources,
                                             Map<String, List<FinishRoll>> assigned) {
        for (OriginalRoll source : sources) {
            if (assigned.getOrDefault(source.getUuid(), List.of()).size()
                    > DirectShipPiecePlan.from(source).count()) {
                throw new BusinessException("直发成品数量超过母卷件数，请先修复来源数据");
            }
        }
    }

    private static void requireNoAmbiguousLegacyFinishes(List<FinishRoll> available) {
        if (!available.isEmpty()) {
            throw new BusinessException("历史直发成品缺少可靠来源关系且无法唯一匹配，不能按顺序匹配，请先修复来源数据");
        }
    }

    private static boolean legacyMatches(OriginalRoll source, FinishRoll finish, boolean uniqueRollNo) {
        if (source.getUuid().equals(finish.getOriginalRollNos())) {
            return true;
        }
        return uniqueRollNo
                && (source.getRollNo().equals(finish.getFinishRollNo())
                || source.getRollNo().equals(finish.getOriginalRollNos()));
    }

    private static boolean isActive(FinishRoll finish) {
        return finish != null && !Integer.valueOf(ROLL_NO_VOID).equals(finish.getRollNoStatus());
    }
}
