package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.calc.IntegerWeightAllocator;
import com.paper.mes.processorder.service.FinishRollStatusPolicy;
import com.paper.mes.processorder.service.SourceConsumptionRatioAllocator;
import com.paper.mes.processorder.model.WidthDifferencePolicy;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ProcessOrderListStats {

    private static final int PROCESS_MODE_DIRECT_SHIP = 3;
    private static final int IS_SPARE_NO = 0;
    private static final int IS_SPARE_YES = 1;
    private static final int IS_REMAIN_YES = 1;
    private static final int ROLL_NO_VOID = 3;

    private ProcessOrderListStats() {
    }

    static void apply(ProcessOrder order, List<OriginalRoll> originals, List<FinishRoll> finishes) {
        apply(order, originals, finishes, List.of());
    }

    static void apply(ProcessOrder order, List<OriginalRoll> originals, List<FinishRoll> finishes,
                      List<FinishOriginalRel> relations) {
        apply(order, originals, finishes, relations, List.of());
    }

    static void apply(ProcessOrder order, List<OriginalRoll> originals, List<FinishRoll> finishes,
                      List<FinishOriginalRel> relations, List<ProcessStep> steps) {
        List<OriginalRoll> originalRows = originals == null ? List.of() : originals.stream()
                .filter(ProcessOrderListStats::isProductionOriginal)
                .toList();
        List<FinishRoll> finishRows = finishes == null ? List.of() : finishes;
        order.setOriginalRollCount(originalRows.size());
        order.setOriginalPieceCount(sumOriginalPieces(originalRows));
        order.setOriginalRollWeight(sumOriginalWeight(originalRows));
        order.setFinishRollCount((int) finishRows.stream().filter(ProcessOrderListStats::isFormalFinishRoll).count());
        Map<String, BigDecimal> estimateWeights = canonicalEstimateWeights(originalRows, finishRows, relations, steps);
        order.setFinishRollWeight(sumFinishWeight(finishRows, estimateWeights));
        order.setEstimateFinishWeight(sumFinishEstimateWeight(finishRows, estimateWeights));
        order.setActualFinishWeight(sumFinishActualWeight(finishRows));
        order.setSpareRollCount((int) finishRows.stream().filter(ProcessOrderListStats::isActiveSpareRoll).count());
    }

    static List<String> processNames(List<ProcessStep> steps, List<OriginalRoll> rolls) {
        List<ProcessStep> rows = steps == null ? List.of() : steps;
        if (rows.isEmpty()) {
            return allDirectShip(rolls) ? List.of("直发") : List.of("待配置");
        }
        Map<Integer, String> namesByType = new LinkedHashMap<>();
        for (ProcessStep step : rows) {
            if (step.getStepType() == null) continue;
            String name = StringUtils.hasText(step.getStepName())
                    ? step.getStepName().trim()
                    : processStepName(step.getStepType());
            namesByType.putIfAbsent(step.getStepType(), name);
        }
        return List.copyOf(namesByType.values());
    }

    static boolean isFormalFinishRoll(FinishRoll roll) {
        boolean formal = roll.getIsSpare() == null || roll.getIsSpare() == IS_SPARE_NO;
        boolean finalProduct = roll.getIsRemain() == null || roll.getIsRemain() != IS_REMAIN_YES;
        boolean active = roll.getRollNoStatus() == null || roll.getRollNoStatus() != ROLL_NO_VOID;
        return formal && finalProduct && active && !FinishRollStatusPolicy.isScrapped(roll);
    }

    private static boolean allDirectShip(List<OriginalRoll> rolls) {
        return rolls != null && !rolls.isEmpty()
                && rolls.stream().allMatch(roll -> Integer.valueOf(PROCESS_MODE_DIRECT_SHIP)
                .equals(roll.getProcessMode()));
    }

    private static boolean isProductionOriginal(OriginalRoll roll) {
        return roll.getDispositionAction() == null
                && !Integer.valueOf(PROCESS_MODE_DIRECT_SHIP).equals(roll.getProcessMode());
    }

    private static int sumOriginalPieces(List<OriginalRoll> rolls) {
        return rolls.stream().mapToInt(roll -> roll.getPieceNum() == null ? 1 : roll.getPieceNum()).sum();
    }

    private static BigDecimal sumOriginalWeight(List<OriginalRoll> rolls) {
        return rolls.stream()
                .map(ProcessOrderListStats::effectiveOriginalWeight)
                .map(ProcessOrderListStats::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal effectiveOriginalWeight(OriginalRoll roll) {
        if (roll.getActualWeight() != null && roll.getActualWeight().signum() > 0) {
            return roll.getActualWeight();
        }
        if ("UNKNOWN".equalsIgnoreCase(roll.getWeightStatus())) return null;
        if (roll.getTotalWeight() != null && roll.getTotalWeight().signum() > 0) {
            return roll.getTotalWeight();
        }
        if (roll.getRollWeight() == null || roll.getRollWeight().signum() <= 0) return null;
        return roll.getRollWeight().multiply(BigDecimal.valueOf(
                roll.getPieceNum() == null ? 1 : roll.getPieceNum()));
    }

    private static BigDecimal sumFinishWeight(List<FinishRoll> rolls,
                                              Map<String, BigDecimal> estimateWeights) {
        return rolls.stream()
                .filter(ProcessOrderListStats::isFormalFinishRoll)
                .map(roll -> hasActualWeight(roll)
                        ? roll.getActualWeight()
                        : estimatedWeight(roll, estimateWeights))
                .map(ProcessOrderListStats::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumFinishEstimateWeight(List<FinishRoll> rolls,
                                                      Map<String, BigDecimal> estimateWeights) {
        return rolls.stream()
                .filter(ProcessOrderListStats::isFormalFinishRoll)
                .map(roll -> estimatedWeight(roll, estimateWeights))
                .map(ProcessOrderListStats::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal estimatedWeight(FinishRoll roll, Map<String, BigDecimal> estimateWeights) {
        BigDecimal stored = storedEstimate(roll);
        if (stored != null) return stored;
        if (estimateWeights.containsKey(roll.getUuid())) return estimateWeights.get(roll.getUuid());
        return roll.getEstimateWeight();
    }

    private static BigDecimal storedEstimate(FinishRoll roll) {
        BigDecimal value = roll.getEstimateWeightSnap() != null
                ? roll.getEstimateWeightSnap() : roll.getEstimateWeight();
        if (value == null) return null;
        return roll.getEstimateWeightSnap() != null || roll.getFinishRollNo() != null ? value : null;
    }

    private static boolean hasStoredEstimate(FinishRoll roll) {
        return storedEstimate(roll) != null;
    }

    private static boolean hasCompleteStoredEstimatePlan(List<FinishRoll> rolls) {
        return !rolls.isEmpty() && rolls.stream().allMatch(ProcessOrderListStats::hasStoredEstimate);
    }

    private static Map<String, BigDecimal> canonicalEstimateWeights(
            List<OriginalRoll> originals, List<FinishRoll> finishes, List<FinishOriginalRel> relations,
            List<ProcessStep> steps) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        Map<String, Set<String>> relatedSources = relatedSourcesByFinish(relations);
        Map<String, List<FinishOriginalRel>> relationsByFinish = relationsByFinish(relations);
        Map<FinishOriginalRel, BigDecimal> effectiveRatios = effectiveConsumptionRatios(relations);
        Map<String, List<FinishRoll>> groups = new LinkedHashMap<>();
        Map<String, Set<String>> sourceIdsByGroup = new LinkedHashMap<>();
        for (FinishRoll finish : finishes) {
            if (!isActiveOutput(finish)) continue;
            Set<String> sourceIds = sourceIds(finish, originals, relatedSources);
            if (sourceIds.isEmpty()) continue;
            String groupKey = String.join("|", sourceIds.stream().sorted().toList());
            groups.computeIfAbsent(groupKey, ignored -> new java.util.ArrayList<>()).add(finish);
            sourceIdsByGroup.putIfAbsent(groupKey, sourceIds);
        }
        Map<String, BigDecimal> totalConsumptionRatios = totalConsumptionRatios(
                groups, sourceIdsByGroup, relationsByFinish, effectiveRatios);
        Map<String, OriginalRoll> originalsByUuid = new LinkedHashMap<>();
        for (OriginalRoll original : originals) {
            if (original.getUuid() != null) originalsByUuid.put(original.getUuid(), original);
        }
        for (Map.Entry<String, List<FinishRoll>> entry : groups.entrySet()) {
            if (hasCompleteStoredEstimatePlan(entry.getValue())) {
                entry.getValue().forEach(finish -> result.put(finish.getUuid(), storedEstimate(finish)));
                continue;
            }
            BigDecimal sourceWeight = sourceWeightForGroup(entry.getValue(), sourceIdsByGroup.get(entry.getKey()),
                    originalsByUuid, relationsByFinish, effectiveRatios);
            if (sourceWeight == null) {
                entry.getValue().forEach(finish -> result.put(finish.getUuid(), null));
                continue;
            }
            if (sourceWeight.signum() <= 0) continue;
            BigDecimal lossWeight = plannedLoss(entry.getValue(), sourceIdsByGroup.get(entry.getKey()),
                    steps, relationsByFinish, effectiveRatios, totalConsumptionRatios);
            boolean saw = sourceIdsByGroup.get(entry.getKey()).stream()
                    .map(originalsByUuid::get)
                    .filter(java.util.Objects::nonNull)
                    .anyMatch(original -> Integer.valueOf(1).equals(original.getMainStepType()));
            Integer sourceWidth = sourceWidth(sourceIdsByGroup.get(entry.getKey()), originalsByUuid);
            WidthDifferencePolicy trimPolicy = trimPolicy(sourceIdsByGroup.get(entry.getKey()), steps);
            allocateGroup(entry.getValue(), sourceWeight, lossWeight, saw, sourceWidth,
                    trimPolicy, result);
        }
        return result;
    }

    private static BigDecimal plannedLoss(List<FinishRoll> group, Set<String> sourceIds,
                                          List<ProcessStep> steps,
                                          Map<String, List<FinishOriginalRel>> relationsByFinish,
                                          Map<FinishOriginalRel, BigDecimal> effectiveRatios,
                                          Map<String, BigDecimal> totalConsumptionRatios) {
        if (sourceIds == null || sourceIds.isEmpty() || steps == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (ProcessStep step : steps) {
            boolean stepBelongsToGroup = step.getOriginalUuid() == null
                    ? sourceIds.size() == 1 : sourceIds.contains(step.getOriginalUuid());
            if (!stepBelongsToGroup
                    || !"LOSS".equalsIgnoreCase(step.getWidthDifferencePolicy())
                    || step.getPlannedLossWeight() == null || step.getPlannedLossWeight().signum() <= 0) {
                continue;
            }
            String sourceId = step.getOriginalUuid() == null
                    ? sourceIds.iterator().next() : step.getOriginalUuid();
            BigDecimal groupRatio = consumeRatio(group, sourceId, relationsByFinish, effectiveRatios);
            BigDecimal totalRatio = totalConsumptionRatios.getOrDefault(sourceId, groupRatio);
            if (totalRatio.signum() <= 0) continue;
            total = total.add(step.getPlannedLossWeight().multiply(groupRatio)
                    .divide(totalRatio, 12, java.math.RoundingMode.HALF_UP));
        }
        return total.setScale(0, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal consumeRatio(List<FinishRoll> group, String sourceId,
                                           Map<String, List<FinishOriginalRel>> relationsByFinish,
                                           Map<FinishOriginalRel, BigDecimal> effectiveRatios) {
        BigDecimal ratio = BigDecimal.ZERO;
        boolean matched = false;
        for (FinishRoll finish : group) {
            for (FinishOriginalRel relation : relationsByFinish.getOrDefault(finish.getUuid(), List.of())) {
                if (!sourceId.equals(relation.getOriginalUuid())) continue;
                matched = true;
                ratio = ratio.add(effectiveRatios.getOrDefault(relation, BigDecimal.ZERO));
            }
        }
        return matched ? ratio.min(new BigDecimal("100")) : new BigDecimal("100");
    }

    private static BigDecimal sourceWeightForGroup(List<FinishRoll> group, Set<String> sourceIds,
                                                   Map<String, OriginalRoll> originalsByUuid,
                                                   Map<String, List<FinishOriginalRel>> relationsByFinish,
                                                   Map<FinishOriginalRel, BigDecimal> effectiveRatios) {
        BigDecimal total = BigDecimal.ZERO;
        for (String sourceId : sourceIds) {
            OriginalRoll source = originalsByUuid.get(sourceId);
            if (source == null) return null;
            BigDecimal weight = effectiveOriginalWeight(source);
            if (weight == null) return null;
            BigDecimal ratio = consumeRatio(group, sourceId, relationsByFinish, effectiveRatios);
            total = total.add(ratio == null ? weight
                    : weight.multiply(ratio).movePointLeft(2));
        }
        return total;
    }

    private static Map<String, List<FinishOriginalRel>> relationsByFinish(List<FinishOriginalRel> relations) {
        Map<String, List<FinishOriginalRel>> result = new LinkedHashMap<>();
        for (FinishOriginalRel relation : relations == null ? List.<FinishOriginalRel>of() : relations) {
            result.computeIfAbsent(relation.getFinishUuid(), ignored -> new java.util.ArrayList<>()).add(relation);
        }
        return result;
    }

    private static Map<String, BigDecimal> totalConsumptionRatios(
            Map<String, List<FinishRoll>> groups,
            Map<String, Set<String>> sourceIdsByGroup,
            Map<String, List<FinishOriginalRel>> relationsByFinish,
            Map<FinishOriginalRel, BigDecimal> effectiveRatios) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<FinishRoll>> entry : groups.entrySet()) {
            List<FinishRoll> group = entry.getValue();
            Set<String> sourceIds = sourceIdsByGroup.getOrDefault(entry.getKey(), Set.of());
            for (String sourceId : sourceIds) {
                result.merge(sourceId, consumeRatio(group, sourceId, relationsByFinish, effectiveRatios),
                        BigDecimal::add);
            }
        }
        return result;
    }

    private static Map<FinishOriginalRel, BigDecimal> effectiveConsumptionRatios(
            List<FinishOriginalRel> relations) {
        List<FinishOriginalRel> sources = relations == null ? List.of() : relations;
        List<BigDecimal> ratios = SourceConsumptionRatioAllocator.allocate(sources.stream()
                .map(relation -> new SourceConsumptionRatioAllocator.SourceRatio(
                        relation.getOriginalUuid(), relation.getConsumeRatio())).toList());
        Map<FinishOriginalRel, BigDecimal> result = new java.util.IdentityHashMap<>();
        for (int index = 0; index < sources.size(); index++) result.put(sources.get(index), ratios.get(index));
        return result;
    }

    private static void allocateGroup(List<FinishRoll> group, BigDecimal sourceWeight, BigDecimal lossWeight,
                                      boolean saw,
                                      Integer sourceWidth,
                                      WidthDifferencePolicy trimPolicy,
                                      Map<String, BigDecimal> result) {
        List<FinishRoll> products = group.stream()
                .filter(ProcessOrderListStats::isFormalFinishRoll).toList();
        List<FinishRoll> trims = group.stream()
                .filter(ProcessOrderListStats::isActiveTrim).toList();
        if (products.isEmpty()) {
            allocateTrimOnlyGroup(group, trims, sourceWeight, lossWeight, sourceWidth, saw, result);
            return;
        }
        BigDecimal measuredProduct = products.stream()
                .filter(ProcessOrderListStats::hasActualWeight)
                .map(FinishRoll::getActualWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal measuredTrim = trims.stream()
                .filter(ProcessOrderListStats::hasActualWeight)
                .map(FinishRoll::getActualWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal explicitTrimBudget = trims.stream()
                .map(finish -> finish.getActualWeight() != null && finish.getActualWeight().signum() > 0
                        ? finish.getActualWeight()
                        : finish.getTrimWeightShare() != null
                            ? finish.getTrimWeightShare() : finish.getEstimateWeight())
                .map(ProcessOrderListStats::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal widthTrimBudget = BigDecimal.ZERO;
        if (sourceWidth != null && sourceWidth > 0) {
            int trimWidth = trims.stream().mapToInt(finish -> Math.max(0,
                    finish.getFinishWidth() == null ? 0 : finish.getFinishWidth())).sum();
            int productWidth = products.stream().mapToInt(finish -> Math.max(0,
                    finish.getFinishWidth() == null ? 0 : finish.getFinishWidth())).sum();
            boolean canInferRemainder = saw && trimPolicy == WidthDifferencePolicy.REMAINDER
                    && (lossWeight == null || lossWeight.signum() == 0)
                    && !trims.isEmpty()
                    && trims.stream().allMatch(finish -> finish.getFinishWidth() != null
                    && finish.getFinishWidth() > 0);
            if (canInferRemainder) {
                trimWidth += Math.max(0, sourceWidth - productWidth - trimWidth);
            }
            widthTrimBudget = sourceWeight.multiply(BigDecimal.valueOf(trimWidth))
                    .divide(BigDecimal.valueOf(sourceWidth), 12, java.math.RoundingMode.HALF_UP);
        }
        if (trims.stream().anyMatch(finish -> !hasActualWeight(finish)
                && !hasTrimBasis(finish, sourceWidth))) {
            group.forEach(finish -> result.put(finish.getUuid(), null));
            return;
        }
        BigDecimal trimBudget = trims.stream().noneMatch(finish -> !hasActualWeight(finish)
                        && (finish.getTrimWeightShare() == null || finish.getTrimWeightShare().signum() <= 0)
                        && (finish.getEstimateWeight() == null || finish.getEstimateWeight().signum() <= 0))
                && explicitTrimBudget.signum() > 0
                ? explicitTrimBudget
                : sourceWidth != null && sourceWidth > 0
                    ? widthTrimBudget.max(measuredTrim)
                    : explicitTrimBudget.max(measuredTrim);
        if (!trims.isEmpty() && trimBudget.signum() == 0) {
            group.forEach(finish -> result.put(finish.getUuid(), null));
            return;
        }
        trimBudget = trimBudget.min(sourceWeight).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal unknownTrimBudget = trimBudget.subtract(measuredTrim).max(BigDecimal.ZERO);
        BigDecimal productBudget = sourceWeight.subtract(unknownTrimBudget).subtract(measuredTrim)
                .subtract(measuredProduct).subtract(lossWeight == null ? BigDecimal.ZERO : lossWeight);
        if (productBudget.signum() < 0) {
            group.forEach(finish -> result.put(finish.getUuid(), null));
            return;
        }
        for (FinishRoll product : products) {
            if (hasActualWeight(product)) result.put(product.getUuid(), product.getActualWeight());
        }
        List<FinishRoll> unknownProducts = products.stream()
                .filter(finish -> !hasActualWeight(finish)).toList();
        if (!unknownProducts.isEmpty() && !isWholeNonNegative(productBudget)) {
            unknownProducts.forEach(finish -> result.put(finish.getUuid(), null));
        } else {
            List<BigDecimal> productWeights = IntegerWeightAllocator.allocate(productBudget,
                    unknownProducts.stream().map(finish -> basis(finish, saw)).toList());
            for (int index = 0; index < unknownProducts.size(); index++) {
                result.put(unknownProducts.get(index).getUuid(), productWeights.get(index));
            }
        }
        if (trims.isEmpty() || trimBudget.signum() <= 0) return;
        for (FinishRoll trim : trims) {
            if (hasActualWeight(trim)) result.put(trim.getUuid(), trim.getActualWeight());
        }
        List<FinishRoll> unknownTrims = trims.stream()
                .filter(finish -> !hasActualWeight(finish)).toList();
        List<FinishRoll> allocatableTrims = unknownTrims.stream()
                .filter(finish -> hasTrimBasis(finish, sourceWidth))
                .toList();
        if (!allocatableTrims.isEmpty() && !isWholeNonNegative(unknownTrimBudget)) {
            allocatableTrims.forEach(finish -> result.put(finish.getUuid(), null));
        } else {
            List<BigDecimal> trimWeights = IntegerWeightAllocator.allocate(unknownTrimBudget,
                    allocatableTrims.stream().map(finish -> basis(finish, saw)).toList());
            for (int index = 0; index < allocatableTrims.size(); index++) {
                result.put(allocatableTrims.get(index).getUuid(), trimWeights.get(index));
            }
        }
        unknownTrims.stream().filter(finish -> !hasTrimBasis(finish, sourceWidth))
                .forEach(finish -> result.put(finish.getUuid(), null));
    }

    private static void allocateTrimOnlyGroup(List<FinishRoll> group,
                                               List<FinishRoll> trims,
                                               BigDecimal sourceWeight,
                                               BigDecimal lossWeight,
                                               Integer sourceWidth,
                                               boolean saw,
                                               Map<String, BigDecimal> result) {
        if (trims.isEmpty() || trims.stream().anyMatch(finish -> !hasTrimBasis(finish, sourceWidth))) {
            group.forEach(finish -> result.put(finish.getUuid(), null));
            return;
        }
        BigDecimal target = sourceWeight.subtract(zeroIfNull(lossWeight)).max(BigDecimal.ZERO);
        BigDecimal measured = trims.stream().filter(ProcessOrderListStats::hasActualWeight)
                .map(FinishRoll::getActualWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<FinishRoll> unknown = trims.stream()
                .filter(finish -> !hasActualWeight(finish)).toList();
        BigDecimal remaining = target.subtract(measured);
        if (!unknown.isEmpty() && !isWholeNonNegative(remaining)) {
            unknown.forEach(finish -> result.put(finish.getUuid(), null));
            return;
        }
        trims.stream().filter(ProcessOrderListStats::hasActualWeight)
                .forEach(finish -> result.put(finish.getUuid(), finish.getActualWeight()));
        List<BigDecimal> weights = IntegerWeightAllocator.allocate(remaining.max(BigDecimal.ZERO),
                unknown.stream().map(finish -> basis(finish, saw)).toList());
        for (int index = 0; index < unknown.size(); index++) {
            result.put(unknown.get(index).getUuid(), weights.get(index));
        }
    }

    private static WidthDifferencePolicy trimPolicy(Set<String> sourceIds, List<ProcessStep> steps) {
        if (steps == null || steps.isEmpty()) return WidthDifferencePolicy.REMAINDER;
        ProcessStep latestSaw = null;
        for (ProcessStep step : steps) {
            if (step.getStepType() != null && step.getStepType() != 1) continue;
            if (step.getOriginalUuid() != null && !sourceIds.contains(step.getOriginalUuid())) continue;
            if (step.getOriginalUuid() == null && sourceIds.size() != 1) continue;
            if (latestSaw == null || compareStepOrder(step, latestSaw) > 0) latestSaw = step;
        }
        if (latestSaw == null) return null;
        return WidthDifferencePolicy.resolve(latestSaw.getWidthDifferencePolicy());
    }

    private static int compareStepOrder(ProcessStep left, ProcessStep right) {
        Integer leftSort = left.getStepSort();
        Integer rightSort = right.getStepSort();
        if (leftSort == null && rightSort == null) return 0;
        if (leftSort == null) return -1;
        if (rightSort == null) return 1;
        return Integer.compare(leftSort, rightSort);
    }

    private static boolean hasTrimBasis(FinishRoll finish, Integer sourceWidth) {
        if (finish.getTrimWeightShare() != null && finish.getTrimWeightShare().signum() > 0) return true;
        if (finish.getEstimateWeight() != null && finish.getEstimateWeight().signum() > 0) return true;
        return sourceWidth != null && sourceWidth > 0
                && finish.getFinishWidth() != null && finish.getFinishWidth() > 0;
    }

    private static boolean hasActualWeight(FinishRoll finish) {
        return finish.getActualWeight() != null && finish.getActualWeight().signum() > 0;
    }

    private static boolean isWholeNonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0
                && value.stripTrailingZeros().scale() <= 0;
    }

    private static Integer sourceWidth(Set<String> sourceIds,
                                       Map<String, OriginalRoll> originalsByUuid) {
        Integer width = null;
        for (String sourceId : sourceIds) {
            OriginalRoll source = originalsByUuid.get(sourceId);
            Integer candidate = source == null
                    ? null : source.getActualWidth() != null && source.getActualWidth() > 0
                        ? source.getActualWidth() : source.getOriginalWidth();
            if (candidate == null || candidate <= 0) return null;
            if (width == null) width = candidate;
            else if (!width.equals(candidate)) return null;
        }
        return width;
    }

    private static BigDecimal basis(FinishRoll finish, boolean saw) {
        if (saw) {
            return BigDecimal.valueOf(Math.max(1, finish.getFinishWidth() == null ? 1 : finish.getFinishWidth()));
        }
        if (finish.getEstimateWeight() != null && finish.getEstimateWeight().signum() > 0) {
            return finish.getEstimateWeight();
        }
        return BigDecimal.valueOf(Math.max(1, finish.getFinishWidth() == null ? 1 : finish.getFinishWidth()));
    }

    private static Map<String, Set<String>> relatedSourcesByFinish(List<FinishOriginalRel> relations) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        if (relations == null) return result;
        for (FinishOriginalRel relation : relations) {
            if (!StringUtils.hasText(relation.getFinishUuid()) || !StringUtils.hasText(relation.getOriginalUuid())) {
                continue;
            }
            result.computeIfAbsent(relation.getFinishUuid(), ignored -> new LinkedHashSet<>())
                    .add(relation.getOriginalUuid());
        }
        return result;
    }

    private static Set<String> sourceIds(FinishRoll finish, List<OriginalRoll> originals,
                                         Map<String, Set<String>> relatedSources) {
        Set<String> related = relatedSources.get(finish.getUuid());
        if (related != null) return related;
        String sourceText = finish.getOriginalRollNos();
        Set<String> result = new LinkedHashSet<>();
        for (OriginalRoll original : originals) {
            if (original.getUuid() != null && sourceTokenMatches(sourceText, original.getUuid(), original.getRollNo())) {
                result.add(original.getUuid());
            }
        }
        if (result.isEmpty() && !StringUtils.hasText(sourceText) && originals.size() == 1
                && originals.get(0).getUuid() != null) {
            result.add(originals.get(0).getUuid());
        }
        return result;
    }

    private static boolean sourceTokenMatches(String sourceText, String uuid, String rollNo) {
        if (!StringUtils.hasText(sourceText)) return false;
        for (String token : sourceText.split("[,，;；/\\s]+")) {
            if (token.equals(uuid) || (StringUtils.hasText(rollNo) && token.equals(rollNo))) return true;
        }
        return false;
    }

    private static boolean isActiveOutput(FinishRoll finish) {
        return !Integer.valueOf(ROLL_NO_VOID).equals(finish.getRollNoStatus())
                && !FinishRollStatusPolicy.isScrapped(finish);
    }

    private static boolean isActiveTrim(FinishRoll finish) {
        return isActiveOutput(finish)
                && Integer.valueOf(IS_REMAIN_YES).equals(finish.getIsRemain())
                && !Integer.valueOf(IS_SPARE_YES).equals(finish.getIsSpare());
    }

    private static BigDecimal sumFinishActualWeight(List<FinishRoll> rolls) {
        return rolls.stream()
                .filter(ProcessOrderListStats::isFormalFinishRoll)
                .map(FinishRoll::getActualWeight)
                .map(ProcessOrderListStats::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean isActiveSpareRoll(FinishRoll roll) {
        return Integer.valueOf(IS_SPARE_YES).equals(roll.getIsSpare())
                && !Integer.valueOf(ROLL_NO_VOID).equals(roll.getRollNoStatus())
                && !FinishRollStatusPolicy.isScrapped(roll);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String processStepName(Integer stepType) {
        if (stepType == null) return "其他工艺";
        return switch (stepType) {
            case 1 -> "锯纸";
            case 2 -> "复卷";
            case 3 -> "剥损整理";
            case 4 -> "重新包装";
            default -> "其他工艺";
        };
    }
}
