package com.paper.mes.processorder.service;

import com.paper.mes.processorder.calc.IntegerWeightAllocator;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.model.WidthDifferencePolicy;
import com.paper.mes.processorder.service.SourceConsumptionRatioAllocator;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.TreeSet;

final class ProcessOrderExportWeightResolver {

    private ProcessOrderExportWeightResolver() {
    }

    static BigDecimal estimateWeight(FinishRoll finish, Map<String, BigDecimal> fallbackWeights) {
        BigDecimal explicit = ProcessOrderExportText.estimateWeight(finish);
        if (hasStoredEstimate(finish, explicit)) {
            return IntegerWeightAllocator.roundTotal(explicit);
        }
        if (fallbackWeights.containsKey(finish.getUuid())) {
            BigDecimal value = fallbackWeights.get(finish.getUuid());
            return value == null ? null : IntegerWeightAllocator.roundTotal(value);
        }
        if (fallbackWeights.containsKey(finish.getFinishRollNo())) {
            BigDecimal value = fallbackWeights.get(finish.getFinishRollNo());
            return value == null ? null : IntegerWeightAllocator.roundTotal(value);
        }
        return isPositive(explicit) ? IntegerWeightAllocator.roundTotal(explicit) : null;
    }

    static Map<String, BigDecimal> fallbackEstimateWeights(List<ProcessOrderDetailVO.RollProductionVO> productions) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (productions == null) {
            return result;
        }
        Map<ProcessOrderDetailVO.FinishSourceVO, BigDecimal> effectiveRatios =
                effectiveConsumptionRatios(productions);
        for (ProcessOrderDetailVO.RollProductionVO production : productions) {
            appendProductionEstimateWeights(result, production, productions, effectiveRatios);
            stageOutputEstimateWeights(production).forEach((key, value) -> putEstimate(result, key, value));
        }
        return result;
    }

    private static void appendProductionEstimateWeights(Map<String, BigDecimal> result,
                                                        ProcessOrderDetailVO.RollProductionVO production,
                                                        List<ProcessOrderDetailVO.RollProductionVO> allProductions,
                                                        Map<ProcessOrderDetailVO.FinishSourceVO, BigDecimal> effectiveRatios) {
        List<ProcessOrderDetailVO.FinishProductionVO> finishes = production.getFinishes() == null
                ? List.of()
                : production.getFinishes();
        Map<String, BigDecimal> canonicalWeights = canonicalWeights(
                production, finishes, allProductions, effectiveRatios);
        for (ProcessOrderDetailVO.FinishProductionVO finish : finishes) {
            BigDecimal estimate = canonicalWeights.get(finish.getUuid());
            BigDecimal stored = storedEstimate(finish);
            if (hasStoredEstimate(finish, stored)) {
                estimate = IntegerWeightAllocator.roundTotal(stored);
            }
            if (!canonicalWeights.containsKey(finish.getUuid()) && isPositive(finish.getEstimateWeight())) {
                estimate = IntegerWeightAllocator.roundTotal(finish.getEstimateWeight());
            }
            putEstimate(result, finish.getUuid(), estimate);
            putEstimate(result, finish.getFinishRollNo(), estimate);
        }
    }

    private static boolean hasStoredEstimate(ProcessOrderDetailVO.FinishProductionVO finish,
                                             BigDecimal estimate) {
        return estimate != null && (finish.getFinishRollNo() != null || finish.getEstimateWeightSnap() != null);
    }

    private static boolean hasStoredEstimate(FinishRoll finish, BigDecimal estimate) {
        return estimate != null && (finish.getFinishRollNo() != null || finish.getEstimateWeightSnap() != null);
    }

    private static BigDecimal storedEstimate(ProcessOrderDetailVO.FinishProductionVO finish) {
        return finish.getEstimateWeight() != null ? finish.getEstimateWeight() : finish.getEstimateWeightSnap();
    }

    private static boolean hasCompleteStoredEstimatePlan(
            List<ProcessOrderDetailVO.FinishProductionVO> finishes) {
        return !finishes.isEmpty()
                && finishes.stream().allMatch(finish -> hasStoredEstimate(finish, storedEstimate(finish)));
    }

    private static Map<String, BigDecimal> canonicalWeights(
            ProcessOrderDetailVO.RollProductionVO production,
            List<ProcessOrderDetailVO.FinishProductionVO> finishes,
            List<ProcessOrderDetailVO.RollProductionVO> allProductions,
            Map<ProcessOrderDetailVO.FinishSourceVO, BigDecimal> effectiveRatios) {
        List<ProcessOrderDetailVO.FinishProductionVO> candidates = finishes.stream()
                .filter(finish -> !Integer.valueOf(1).equals(finish.getIsSpare())
                        && !Integer.valueOf(3).equals(finish.getRollNoStatus()))
                .toList();
        Map<String, BigDecimal> result = new HashMap<>();
        Map<String, List<ProcessOrderDetailVO.FinishProductionVO>> groups = new HashMap<>();
        for (ProcessOrderDetailVO.FinishProductionVO finish : candidates) {
            groups.computeIfAbsent(sourceKey(production, finish), ignored -> new ArrayList<>()).add(finish);
        }
        for (List<ProcessOrderDetailVO.FinishProductionVO> group : groups.values()) {
            if (hasCompleteStoredEstimatePlan(group)) {
                group.forEach(finish -> result.put(finish.getUuid(), hasStoredEstimate(finish, storedEstimate(finish))
                        ? IntegerWeightAllocator.roundTotal(storedEstimate(finish)) : null));
                continue;
            }
            allocateGroup(result, production, group, allProductions, effectiveRatios);
        }
        return result;
    }

    private static void allocateGroup(Map<String, BigDecimal> result,
                                      ProcessOrderDetailVO.RollProductionVO production,
                                      List<ProcessOrderDetailVO.FinishProductionVO> group,
                                      List<ProcessOrderDetailVO.RollProductionVO> allProductions,
                                      Map<ProcessOrderDetailVO.FinishSourceVO, BigDecimal> effectiveRatios) {
        BigDecimal sourceWeight = sourceWeightForGroup(production, group, allProductions, effectiveRatios);
        if (sourceWeight == null) {
            group.forEach(finish -> result.put(finish.getUuid(), null));
            group.forEach(finish -> result.put(finish.getFinishRollNo(), null));
            return;
        }
        if (sourceWeight.signum() <= 0) return;
        BigDecimal lossWeight = plannedLossForGroup(production, group, allProductions, effectiveRatios);
        List<ProcessOrderDetailVO.FinishProductionVO> products = group.stream()
                .filter(finish -> !Integer.valueOf(1).equals(finish.getIsRemain())).toList();
        List<ProcessOrderDetailVO.FinishProductionVO> trims = group.stream()
                .filter(finish -> Integer.valueOf(1).equals(finish.getIsRemain())).toList();
        if (products.isEmpty()) {
            allocateTrimOnlyGroup(result, group, trims, sourceWeight, lossWeight,
                    sourceWidthForGroup(production, group, allProductions));
            return;
        }
        WidthDifferencePolicy policy = trimPolicy(production, group, allProductions);
        BigDecimal explicitTrimBudget = trims.stream()
                .map(ProcessOrderExportWeightResolver::trimWeightBasis)
                .map(ProcessOrderExportWeightResolver::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Integer sourceWidth = sourceWidthForGroup(production, group, allProductions);
        BigDecimal widthTrimBudget = BigDecimal.ZERO;
        if (sourceWidth != null && sourceWidth > 0) {
            int trimWidth = trims.stream().mapToInt(finish -> Math.max(0,
                    finish.getFinishWidth() == null ? 0 : finish.getFinishWidth())).sum();
            int productWidth = products.stream().mapToInt(finish -> Math.max(0,
                    finish.getFinishWidth() == null ? 0 : finish.getFinishWidth())).sum();
            if (policy == WidthDifferencePolicy.REMAINDER && !trims.isEmpty()) {
                trimWidth += Math.max(0, sourceWidth - productWidth - trimWidth);
            }
            widthTrimBudget = sourceWeight.multiply(BigDecimal.valueOf(trimWidth))
                    .divide(BigDecimal.valueOf(sourceWidth), 12, java.math.RoundingMode.HALF_UP);
        }
        BigDecimal measuredTrim = trims.stream().filter(finish -> isPositive(finish.getActualWeight()))
                .map(ProcessOrderDetailVO.FinishProductionVO::getActualWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean allTrimsMeasured = !trims.isEmpty()
                && trims.stream().allMatch(finish -> isPositive(finish.getActualWeight()));
        BigDecimal trimBudget = allTrimsMeasured && explicitTrimBudget.signum() > 0
                ? explicitTrimBudget
                : sourceWidth != null && sourceWidth > 0
                    ? widthTrimBudget.max(measuredTrim)
                    : explicitTrimBudget.max(measuredTrim);
        trimBudget = trimBudget.min(sourceWeight);
        // An unmeasured trim without any physical or persisted basis cannot be
        // assigned a weight. Do not hide that remainder inside saleable products.
        if (!trims.isEmpty() && trimBudget.signum() <= 0) {
            group.forEach(finish -> result.put(finish.getUuid(), null));
            group.forEach(finish -> result.put(finish.getFinishRollNo(), null));
            return;
        }
        if (trims.stream().anyMatch(finish -> !isPositive(finish.getActualWeight())
                && !hasTrimBasis(finish, sourceWidth))) {
            group.forEach(finish -> result.put(finish.getUuid(), null));
            group.forEach(finish -> result.put(finish.getFinishRollNo(), null));
            return;
        }
        for (ProcessOrderDetailVO.FinishProductionVO product : products) {
            if (isPositive(product.getActualWeight())) {
                result.put(product.getUuid(), IntegerWeightAllocator.roundTotal(product.getActualWeight()));
            }
        }
        List<ProcessOrderDetailVO.FinishProductionVO> unknownProducts = products.stream()
                .filter(product -> !isPositive(product.getActualWeight())).toList();
        BigDecimal measuredProduct = products.stream()
                .filter(product -> isPositive(product.getActualWeight()))
                .map(ProcessOrderDetailVO.FinishProductionVO::getActualWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal productTarget = sourceWeight.subtract(trimBudget)
                .subtract(lossWeight).subtract(measuredProduct);
        if (!unknownProducts.isEmpty() && !isWholeNonNegative(productTarget)) {
            unknownProducts.forEach(product -> result.put(product.getUuid(), null));
        } else {
            List<BigDecimal> weights = IntegerWeightAllocator.allocate(productTarget.max(BigDecimal.ZERO),
                    unknownProducts.stream().map(finish -> basis(production, finish)).toList());
            for (int index = 0; index < unknownProducts.size(); index++) {
                result.put(unknownProducts.get(index).getUuid(), weights.get(index));
            }
        }
        if (trimBudget.signum() <= 0 || trims.isEmpty()) return;
        List<ProcessOrderDetailVO.FinishProductionVO> unknownTrims = trims.stream()
                .filter(finish -> !isPositive(finish.getActualWeight())).toList();
        for (ProcessOrderDetailVO.FinishProductionVO trim : trims) {
            if (isPositive(trim.getActualWeight())) {
                result.put(trim.getUuid(), IntegerWeightAllocator.roundTotal(trim.getActualWeight()));
            }
        }
        BigDecimal unknownTrimBudget = trimBudget.subtract(measuredTrim).max(BigDecimal.ZERO);
        List<ProcessOrderDetailVO.FinishProductionVO> allocatableTrims = unknownTrims.stream()
                .filter(finish -> hasTrimBasis(finish, sourceWidth))
                .toList();
        if (!allocatableTrims.isEmpty() && !isWholeNonNegative(unknownTrimBudget)) {
            allocatableTrims.forEach(trim -> result.put(trim.getUuid(), null));
        } else {
            List<BigDecimal> trimWeights = IntegerWeightAllocator.allocate(unknownTrimBudget,
                    allocatableTrims.stream().map(finish -> basis(production, finish)).toList());
            for (int index = 0; index < allocatableTrims.size(); index++) {
                result.put(allocatableTrims.get(index).getUuid(), trimWeights.get(index));
            }
        }
        unknownTrims.stream().filter(finish -> !hasTrimBasis(finish, sourceWidth))
                .forEach(finish -> result.put(finish.getUuid(), null));
    }

    private static boolean hasTrimBasis(ProcessOrderDetailVO.FinishProductionVO finish,
                                        Integer sourceWidth) {
        if (finish.getTrimWeightShare() != null && finish.getTrimWeightShare().signum() > 0) return true;
        if (finish.getEstimateWeight() != null && finish.getEstimateWeight().signum() > 0) return true;
        return sourceWidth != null && sourceWidth > 0
                && finish.getFinishWidth() != null && finish.getFinishWidth() > 0;
    }

    private static BigDecimal trimWeightBasis(ProcessOrderDetailVO.FinishProductionVO finish) {
        if (isPositive(finish.getActualWeight())) return finish.getActualWeight();
        return finish.getTrimWeightShare() != null ? finish.getTrimWeightShare() : finish.getEstimateWeight();
    }

    private static WidthDifferencePolicy trimPolicy(ProcessOrderDetailVO.RollProductionVO production,
                                                    List<ProcessOrderDetailVO.FinishProductionVO> group,
                                                    List<ProcessOrderDetailVO.RollProductionVO> allProductions) {
        Set<String> sourceIds = new LinkedHashSet<>();
        group.stream().flatMap(finish -> finish.getSources() == null ? java.util.stream.Stream.empty()
                        : finish.getSources().stream())
                .map(ProcessOrderDetailVO.FinishSourceVO::getOriginalUuid)
                .filter(value -> value != null && !value.isBlank()).forEach(sourceIds::add);
        if (sourceIds.isEmpty() && production.getOriginalUuid() != null) sourceIds.add(production.getOriginalUuid());
        ProcessStep latestSaw = null;
        for (ProcessOrderDetailVO.RollProductionVO source : allProductions) {
            if (!sourceIds.contains(source.getOriginalUuid()) || source.getSteps() == null) continue;
            for (ProcessStep step : source.getSteps()) {
                if (step.getStepType() != null && step.getStepType() != 1) continue;
                if (latestSaw == null || compareStepOrder(step, latestSaw) > 0) latestSaw = step;
            }
        }
        if (latestSaw == null && Integer.valueOf(1).equals(production.getMainStepType())) {
            return WidthDifferencePolicy.REMAINDER;
        }
        return latestSaw == null ? null : WidthDifferencePolicy.resolve(latestSaw.getWidthDifferencePolicy());
    }

    private static int compareStepOrder(ProcessStep left, ProcessStep right) {
        Integer leftSort = left.getStepSort();
        Integer rightSort = right.getStepSort();
        if (leftSort == null && rightSort == null) return 0;
        if (leftSort == null) return -1;
        if (rightSort == null) return 1;
        return Integer.compare(leftSort, rightSort);
    }

    private static String sourceKey(ProcessOrderDetailVO.RollProductionVO production,
                                    ProcessOrderDetailVO.FinishProductionVO finish) {
        Set<String> sourceIds = new TreeSet<>();
        if (finish.getSources() != null) {
            finish.getSources().stream().map(ProcessOrderDetailVO.FinishSourceVO::getOriginalUuid)
                    .filter(value -> value != null && !value.isBlank()).forEach(sourceIds::add);
        }
        if (sourceIds.isEmpty() && production.getOriginalUuid() != null) sourceIds.add(production.getOriginalUuid());
        return String.join("|", sourceIds);
    }

    private static BigDecimal sourceWeightForGroup(ProcessOrderDetailVO.RollProductionVO production,
                                                  List<ProcessOrderDetailVO.FinishProductionVO> group,
                                                  List<ProcessOrderDetailVO.RollProductionVO> allProductions,
                                                  Map<ProcessOrderDetailVO.FinishSourceVO, BigDecimal> effectiveRatios) {
        Map<String, ProcessOrderDetailVO.RollProductionVO> productionByUuid = new HashMap<>();
        for (ProcessOrderDetailVO.RollProductionVO item : allProductions) {
            if (item.getOriginalUuid() != null) productionByUuid.put(item.getOriginalUuid(), item);
        }
        Map<String, ProcessOrderDetailVO.FinishSourceVO> snapshotByUuid = new HashMap<>();
        Map<String, BigDecimal> consumeRatios = new HashMap<>();
        Set<String> relatedSources = new LinkedHashSet<>();
        for (ProcessOrderDetailVO.FinishProductionVO finish : group) {
            List<ProcessOrderDetailVO.FinishSourceVO> sources = finish.getSources() == null
                    ? List.of() : finish.getSources();
            for (ProcessOrderDetailVO.FinishSourceVO source : sources) {
                if (source.getOriginalUuid() != null) {
                    snapshotByUuid.putIfAbsent(source.getOriginalUuid(), source);
                    relatedSources.add(source.getOriginalUuid());
                    consumeRatios.merge(source.getOriginalUuid(),
                            effectiveRatios.getOrDefault(source, BigDecimal.ZERO), BigDecimal::add);
                }
            }
        }
        Set<String> sourceIds = new TreeSet<>();
        for (ProcessOrderDetailVO.FinishProductionVO finish : group) {
            if (finish.getSources() != null) finish.getSources().stream()
                    .map(ProcessOrderDetailVO.FinishSourceVO::getOriginalUuid)
                    .filter(value -> value != null && !value.isBlank()).forEach(sourceIds::add);
        }
        if (sourceIds.isEmpty() && production.getOriginalUuid() != null) sourceIds.add(production.getOriginalUuid());
        BigDecimal total = BigDecimal.ZERO;
        for (String sourceId : sourceIds) {
            ProcessOrderDetailVO.RollProductionVO source = productionByUuid.get(sourceId);
            BigDecimal weight = sourceWeight(source, snapshotByUuid.get(sourceId));
            if (weight == null) return null;
            BigDecimal ratio = relatedSources.contains(sourceId)
                    ? consumeRatios.getOrDefault(sourceId, BigDecimal.ZERO) : new BigDecimal("100");
            if (ratio != null) ratio = ratio.min(new BigDecimal("100.00"));
            total = total.add(ratio == null ? weight : weight.multiply(ratio).movePointLeft(2));
        }
        if (!sourceIds.isEmpty()) {
            return total.signum() > 0 ? IntegerWeightAllocator.roundTotal(total) : null;
        }
        return productionSourceWeight(production);
    }

    private static BigDecimal plannedLossForGroup(ProcessOrderDetailVO.RollProductionVO production,
                                                  List<ProcessOrderDetailVO.FinishProductionVO> group,
                                                  List<ProcessOrderDetailVO.RollProductionVO> allProductions,
                                                  Map<ProcessOrderDetailVO.FinishSourceVO, BigDecimal> effectiveRatios) {
        Map<String, ProcessOrderDetailVO.RollProductionVO> productionByUuid = new HashMap<>();
        for (ProcessOrderDetailVO.RollProductionVO item : allProductions) {
            if (item.getOriginalUuid() != null) productionByUuid.put(item.getOriginalUuid(), item);
        }
        Map<String, BigDecimal> ratios = new HashMap<>();
        Set<String> relatedSources = new LinkedHashSet<>();
        Set<String> sourceIds = new TreeSet<>();
        for (ProcessOrderDetailVO.FinishProductionVO finish : group) {
            if (finish.getSources() == null) continue;
            for (ProcessOrderDetailVO.FinishSourceVO source : finish.getSources()) {
                if (source.getOriginalUuid() == null) continue;
                sourceIds.add(source.getOriginalUuid());
                relatedSources.add(source.getOriginalUuid());
                ratios.merge(source.getOriginalUuid(),
                        effectiveRatios.getOrDefault(source, BigDecimal.ZERO), BigDecimal::add);
            }
        }
        if (sourceIds.isEmpty() && production.getOriginalUuid() != null) sourceIds.add(production.getOriginalUuid());
        BigDecimal total = BigDecimal.ZERO;
        for (String sourceId : sourceIds) {
            ProcessOrderDetailVO.RollProductionVO source = productionByUuid.get(sourceId);
            if (source == null || source.getSteps() == null) continue;
            BigDecimal ratio = (relatedSources.contains(sourceId)
                    ? ratios.getOrDefault(sourceId, BigDecimal.ZERO) : new BigDecimal("100"))
                    .min(new BigDecimal("100"));
            for (ProcessStep step : source.getSteps()) {
                if ("LOSS".equalsIgnoreCase(step.getWidthDifferencePolicy())
                        && step.getPlannedLossWeight() != null && step.getPlannedLossWeight().signum() > 0) {
                    total = total.add(step.getPlannedLossWeight().multiply(ratio).movePointLeft(2));
                }
            }
        }
        return IntegerWeightAllocator.roundTotal(total).max(BigDecimal.ZERO);
    }

    private static Map<ProcessOrderDetailVO.FinishSourceVO, BigDecimal> effectiveConsumptionRatios(
            List<ProcessOrderDetailVO.RollProductionVO> productions) {
        List<ProcessOrderDetailVO.FinishSourceVO> sources = productions.stream()
                .flatMap(production -> production.getFinishes() == null ? java.util.stream.Stream.empty()
                        : production.getFinishes().stream())
                .flatMap(finish -> finish.getSources() == null ? java.util.stream.Stream.empty()
                        : finish.getSources().stream()).toList();
        List<BigDecimal> ratios = SourceConsumptionRatioAllocator.allocate(sources.stream()
                .map(source -> new SourceConsumptionRatioAllocator.SourceRatio(
                        source.getOriginalUuid(), source.getConsumeRatio())).toList());
        Map<ProcessOrderDetailVO.FinishSourceVO, BigDecimal> result = new java.util.IdentityHashMap<>();
        for (int index = 0; index < sources.size(); index++) result.put(sources.get(index), ratios.get(index));
        return result;
    }

    private static BigDecimal sourceWeightSnapshot(ProcessOrderDetailVO.FinishSourceVO source) {
        if (source == null) return null;
        if (isPositive(source.getActualWeight())) return IntegerWeightAllocator.roundTotal(source.getActualWeight());
        if ("UNKNOWN".equalsIgnoreCase(source.getWeightStatus())) return null;
        if (isPositive(source.getTotalWeight())) return IntegerWeightAllocator.roundTotal(source.getTotalWeight());
        if (!isPositive(source.getRollWeight())) return null;
        int pieces = source.getPieceNum() == null ? 1 : Math.max(1, source.getPieceNum());
        return IntegerWeightAllocator.roundTotal(source.getRollWeight().multiply(BigDecimal.valueOf(pieces)));
    }

    private static BigDecimal productionSourceWeight(ProcessOrderDetailVO.RollProductionVO production) {
        if (isPositive(production.getActualWeight())) return IntegerWeightAllocator.roundTotal(production.getActualWeight());
        if ("UNKNOWN".equalsIgnoreCase(production.getWeightStatus())) return null;
        if (isPositive(production.getTotalWeight())) return IntegerWeightAllocator.roundTotal(production.getTotalWeight());
        if (!isPositive(production.getRollWeight())) return null;
        int pieces = production.getPieceNum() == null ? 1 : Math.max(1, production.getPieceNum());
        return IntegerWeightAllocator.roundTotal(production.getRollWeight().multiply(BigDecimal.valueOf(pieces)));
    }

    private static BigDecimal sourceWeight(ProcessOrderDetailVO.RollProductionVO production,
                                            ProcessOrderDetailVO.FinishSourceVO snapshot) {
        if (production == null) return sourceWeightSnapshot(snapshot);
        BigDecimal value = productionSourceWeight(production);
        return value == null ? sourceWeightSnapshot(snapshot) : value;
    }

    private static Integer sourceWidthForGroup(ProcessOrderDetailVO.RollProductionVO production,
                                               List<ProcessOrderDetailVO.FinishProductionVO> group,
                                               List<ProcessOrderDetailVO.RollProductionVO> allProductions) {
        Set<String> ids = new LinkedHashSet<>();
        group.stream().flatMap(finish -> finish.getSources() == null ? java.util.stream.Stream.empty()
                        : finish.getSources().stream())
                .map(ProcessOrderDetailVO.FinishSourceVO::getOriginalUuid)
                .filter(value -> value != null && !value.isBlank()).forEach(ids::add);
        if (ids.isEmpty() && production.getOriginalUuid() != null) ids.add(production.getOriginalUuid());
        Integer width = null;
        for (String id : ids) {
            ProcessOrderDetailVO.RollProductionVO source = allProductions.stream()
                    .filter(item -> id.equals(item.getOriginalUuid())).findFirst().orElse(null);
            Integer candidate = source == null ? null
                    : source.getActualWidth() != null && source.getActualWidth() > 0
                        ? source.getActualWidth() : source.getOriginalWidth();
            if (candidate == null || candidate <= 0) return null;
            if (width == null) width = candidate;
            else if (!width.equals(candidate)) return null;
        }
        return width;
    }

    private static BigDecimal basis(ProcessOrderDetailVO.RollProductionVO production,
                                    ProcessOrderDetailVO.FinishProductionVO finish) {
        if (Integer.valueOf(1).equals(production.getMainStepType())) {
            return BigDecimal.valueOf(Math.max(1, finish.getFinishWidth() == null ? 1 : finish.getFinishWidth()));
        }
        if (isPositive(finish.getEstimateWeight())) return finish.getEstimateWeight();
        return BigDecimal.valueOf(Math.max(1, finish.getFinishWidth() == null ? 1 : finish.getFinishWidth()));
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    static Map<String, BigDecimal> stageOutputEstimateWeights(
            ProcessOrderDetailVO.RollProductionVO production) {
        List<ProcessOrderDetailVO.StageOutputVO> outputs = production.getStageOutputs() == null
                ? List.of() : production.getStageOutputs();
        Map<String, BigDecimal> result = new HashMap<>();
        List<Integer> levels = outputs.stream()
                .map(output -> output.getStageLevel() == null ? 1 : output.getStageLevel())
                .distinct().sorted().toList();
        for (Integer level : levels) {
            List<ProcessOrderDetailVO.StageOutputVO> stage = outputs.stream()
                    .filter(output -> (output.getStageLevel() == null ? 1 : output.getStageLevel()) == level)
                    .sorted(Comparator.comparing(output -> output.getOutputSort() == null ? 0 : output.getOutputSort()))
                    .toList();
            if (stage.stream().anyMatch(ProcessOrderExportWeightResolver::hasStoredStageEstimate)) {
                stage.forEach(output -> result.put(output.getUuid(), hasStoredStageEstimate(output)
                        ? IntegerWeightAllocator.roundTotal(output.getEstimateWeight()) : null));
                continue;
            }
            BigDecimal source = stageSourceWeight(production, stage, outputs, result);
            if (source == null) {
                stage.forEach(output -> result.put(output.getUuid(), null));
                continue;
            }
            if (source.signum() <= 0) continue;
            StageBudget budget = stageBudget(production, stage, source, outputs);
            if (budget == null) {
                stage.forEach(output -> result.put(output.getUuid(), null));
                continue;
            }
            allocateStageWeights(production, stage, budget, result);
        }
        return result;
    }

    private static boolean hasStoredStageEstimate(ProcessOrderDetailVO.StageOutputVO output) {
        return "ESTIMATED".equalsIgnoreCase(output.getWeightStatus())
                && output.getEstimateWeight() != null && output.getEstimateWeight().signum() >= 0;
    }

    private static BigDecimal stageSourceWeight(
            ProcessOrderDetailVO.RollProductionVO production,
            List<ProcessOrderDetailVO.StageOutputVO> stage,
            List<ProcessOrderDetailVO.StageOutputVO> allOutputs,
            Map<String, BigDecimal> estimates) {
        Set<String> parentKeys = new LinkedHashSet<>();
        for (ProcessOrderDetailVO.StageOutputVO output : stage) {
            if (output.getInputOutputUuids() != null && !output.getInputOutputUuids().isEmpty()) {
                parentKeys.addAll(output.getInputOutputUuids());
            } else if (output.getParentOutputUuid() != null) {
                parentKeys.add(output.getParentOutputUuid());
            }
        }
        BigDecimal total = BigDecimal.ZERO;
        Map<String, ProcessOrderDetailVO.StageOutputVO> outputsByUuid = new HashMap<>();
        for (ProcessOrderDetailVO.StageOutputVO output : allOutputs) {
            if (output.getUuid() != null) outputsByUuid.put(output.getUuid(), output);
        }
        for (String parentKey : parentKeys) {
            ProcessOrderDetailVO.StageOutputVO output = outputsByUuid.get(parentKey);
            if (output == null) return null;
            BigDecimal weight = output.getActualWeight() != null && output.getActualWeight().signum() > 0
                    ? output.getActualWeight()
                    : "UNKNOWN".equalsIgnoreCase(output.getWeightStatus())
                        ? null
                        : estimates.containsKey(output.getUuid()) ? estimates.get(output.getUuid())
                            : output.getEstimateWeight();
            if (weight == null || weight.signum() <= 0) return null;
            total = total.add(weight);
        }
        if (total.signum() > 0) return IntegerWeightAllocator.roundTotal(total);
        List<ProcessOrderDetailVO.StageOutputVO> inherited = previousStageOutputs(stage, allOutputs);
        if (!parentKeys.isEmpty() || inherited.isEmpty()) {
            return productionSourceWeight(production) == null
                    ? null : IntegerWeightAllocator.roundTotal(productionSourceWeight(production));
        }
        for (ProcessOrderDetailVO.StageOutputVO output : inherited) {
            BigDecimal weight = output.getActualWeight() != null && output.getActualWeight().signum() > 0
                    ? output.getActualWeight()
                    : "UNKNOWN".equalsIgnoreCase(output.getWeightStatus()) ? null
                        : estimates.containsKey(output.getUuid()) ? estimates.get(output.getUuid())
                            : output.getEstimateWeight();
            if (weight == null || weight.signum() <= 0) return null;
            total = total.add(weight);
        }
        if (total.signum() > 0) return IntegerWeightAllocator.roundTotal(total);
        return productionSourceWeight(production) == null
                ? null : IntegerWeightAllocator.roundTotal(productionSourceWeight(production));
    }

    private static List<ProcessOrderDetailVO.StageOutputVO> previousStageOutputs(
            List<ProcessOrderDetailVO.StageOutputVO> stage,
            List<ProcessOrderDetailVO.StageOutputVO> allOutputs) {
        int currentLevel = stage.stream()
                .mapToInt(output -> output.getStageLevel() == null ? 1 : output.getStageLevel())
                .min().orElse(1);
        int previousLevel = allOutputs.stream()
                .mapToInt(output -> output.getStageLevel() == null ? 1 : output.getStageLevel())
                .filter(level -> level < currentLevel)
                .max().orElse(Integer.MIN_VALUE);
        if (previousLevel == Integer.MIN_VALUE) return List.of();
        return allOutputs.stream()
                .filter(output -> (output.getStageLevel() == null ? 1 : output.getStageLevel()) == previousLevel)
                .filter(output -> !isTrim(output))
                .toList();
    }

    private static StageBudget stageBudget(ProcessOrderDetailVO.RollProductionVO production,
                                           List<ProcessOrderDetailVO.StageOutputVO> stage,
                                           BigDecimal source,
                                           List<ProcessOrderDetailVO.StageOutputVO> allOutputs) {
        WidthDifferencePolicy policy = stagePolicy(production, stage);
        BigDecimal target = source;
        ProcessStep step = stageStep(production, stage);
        if (policy == WidthDifferencePolicy.LOSS && step != null && step.getPlannedLossWeight() != null) {
            target = source.subtract(IntegerWeightAllocator.roundTotal(step.getPlannedLossWeight())).max(BigDecimal.ZERO);
        }
        Integer sourceWidth = stageSourceWidth(stage, allOutputs, production);
        BigDecimal explicitTrim = stage.stream().filter(ProcessOrderExportWeightResolver::isTrim)
                .map(output -> hasActual(output) ? output.getActualWeight() : output.getEstimateWeight())
                .map(ProcessOrderExportWeightResolver::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal trimWidth = stage.stream().filter(ProcessOrderExportWeightResolver::isTrim)
                .map(output -> BigDecimal.valueOf(Math.max(0, output.getFinishWidth() == null ? 0 : output.getFinishWidth())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal productWidth = stage.stream().filter(output -> !isTrim(output))
                .map(output -> BigDecimal.valueOf(Math.max(0, output.getFinishWidth() == null ? 0 : output.getFinishWidth())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal widthBudget = BigDecimal.ZERO;
        if (sourceWidth != null && sourceWidth > 0) {
            BigDecimal budgetWidth = trimWidth;
            if (policy == WidthDifferencePolicy.REMAINDER) {
                budgetWidth = budgetWidth.add(BigDecimal.valueOf(sourceWidth)
                        .subtract(productWidth).subtract(trimWidth).max(BigDecimal.ZERO));
            }
            widthBudget = source.multiply(budgetWidth)
                    .divide(BigDecimal.valueOf(sourceWidth), 12, java.math.RoundingMode.HALF_UP);
        }
        boolean hasTrim = stage.stream().anyMatch(ProcessOrderExportWeightResolver::isTrim);
        BigDecimal measuredTrim = knownActual(stage.stream()
                .filter(ProcessOrderExportWeightResolver::isTrim).toList());
        boolean allTrimsMeasured = hasTrim && stage.stream()
                .filter(ProcessOrderExportWeightResolver::isTrim)
                .allMatch(ProcessOrderExportWeightResolver::hasActual);
        BigDecimal trimBudget = allTrimsMeasured && explicitTrim.signum() > 0
                ? explicitTrim
                : sourceWidth != null && sourceWidth > 0
                    ? widthBudget.max(measuredTrim)
                    : explicitTrim.max(measuredTrim);
        if (hasTrim && trimBudget.signum() == 0) return null;
        trimBudget = IntegerWeightAllocator.roundTotal(trimBudget).min(target).max(BigDecimal.ZERO);
        return new StageBudget(target, trimBudget);
    }

    private static void allocateStageWeights(ProcessOrderDetailVO.RollProductionVO production,
                                              List<ProcessOrderDetailVO.StageOutputVO> stage,
                                              StageBudget budget,
                                              Map<String, BigDecimal> result) {
        List<ProcessOrderDetailVO.StageOutputVO> products = stage.stream().filter(output -> !isTrim(output)).toList();
        List<ProcessOrderDetailVO.StageOutputVO> trims = stage.stream().filter(ProcessOrderExportWeightResolver::isTrim).toList();
        BigDecimal knownProduct = knownActual(products);
        BigDecimal knownTrim = knownActual(trims);
        BigDecimal productTarget = budget.target().subtract(budget.trim()).subtract(knownProduct);
        List<ProcessOrderDetailVO.StageOutputVO> unknownProducts = products.stream().filter(output -> !hasActual(output)).toList();
        if (!unknownProducts.isEmpty() && !isWholeNonNegative(productTarget)) {
            unknownProducts.forEach(output -> result.put(output.getUuid(), null));
        } else {
            putUnknown(result, unknownProducts, IntegerWeightAllocator.allocate(productTarget.max(BigDecimal.ZERO),
                    unknownProducts.stream().map(output -> stageBasis(production, output)).toList()));
        }
        BigDecimal trimTarget = budget.trim().subtract(knownTrim);
        List<ProcessOrderDetailVO.StageOutputVO> unknownTrims = trims.stream().filter(output -> !hasActual(output)).toList();
        List<ProcessOrderDetailVO.StageOutputVO> allocatableTrims = unknownTrims.stream()
                .filter(ProcessOrderExportWeightResolver::hasTrimBasis)
                .toList();
        if (!allocatableTrims.isEmpty() && !isWholeNonNegative(trimTarget)) {
            allocatableTrims.forEach(output -> result.put(output.getUuid(), null));
        } else {
            putUnknown(result, allocatableTrims, IntegerWeightAllocator.allocate(trimTarget,
                    allocatableTrims.stream().map(output -> stageBasis(production, output)).toList()));
        }
        unknownTrims.stream().filter(output -> !hasTrimBasis(output))
                .forEach(output -> result.put(output.getUuid(), null));
    }

    private static void putUnknown(Map<String, BigDecimal> result,
                                   List<ProcessOrderDetailVO.StageOutputVO> outputs,
                                   List<BigDecimal> weights) {
        for (int index = 0; index < outputs.size(); index++) result.put(outputs.get(index).getUuid(), weights.get(index));
    }

    private static BigDecimal knownActual(List<ProcessOrderDetailVO.StageOutputVO> outputs) {
        return outputs.stream().filter(ProcessOrderExportWeightResolver::hasActual)
                .map(ProcessOrderDetailVO.StageOutputVO::getActualWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean hasActual(ProcessOrderDetailVO.StageOutputVO output) {
        return output.getActualWeight() != null && output.getActualWeight().signum() > 0;
    }

    private static boolean hasTrimBasis(ProcessOrderDetailVO.StageOutputVO output) {
        return isPositive(output.getEstimateWeight())
                || (output.getFinishWidth() != null && output.getFinishWidth() > 0);
    }

    private static WidthDifferencePolicy stagePolicy(ProcessOrderDetailVO.RollProductionVO production,
                                                     List<ProcessOrderDetailVO.StageOutputVO> stage) {
        ProcessStep step = stageStep(production, stage);
        return step == null ? WidthDifferencePolicy.REMAINDER
                : WidthDifferencePolicy.resolve(step.getWidthDifferencePolicy());
    }

    private static ProcessStep stageStep(ProcessOrderDetailVO.RollProductionVO production,
                                         List<ProcessOrderDetailVO.StageOutputVO> stage) {
        if (production.getSteps() == null || stage.isEmpty()) return null;
        Integer level = stage.get(0).getStageLevel() == null ? 1 : stage.get(0).getStageLevel();
        ProcessStep byLevel = findStep(production, step -> level.equals(step.getStageLevel()));
        if (byLevel != null) return byLevel;
        ProcessStep bySort = findStep(production, step -> level.equals(step.getStepSort()));
        if (bySort != null) return bySort;
        if (level == 1) {
            ProcessStep main = findStep(production, step -> Integer.valueOf(1).equals(step.getIsMain()));
            if (main != null) return main;
        }
        Integer type = stage.get(0).getSourceStepType();
        return type == null ? null : findStep(production, step -> type.equals(step.getStepType()));
    }

    private static ProcessStep findStep(ProcessOrderDetailVO.RollProductionVO production,
                                        java.util.function.Predicate<ProcessStep> predicate) {
        return production.getSteps().stream().filter(predicate).findFirst().orElse(null);
    }

    private static Integer stageSourceWidth(List<ProcessOrderDetailVO.StageOutputVO> stage,
                                            List<ProcessOrderDetailVO.StageOutputVO> allOutputs,
                                            ProcessOrderDetailVO.RollProductionVO production) {
        Set<String> parents = new LinkedHashSet<>();
        stage.forEach(output -> {
            if (output.getInputOutputUuids() != null) parents.addAll(output.getInputOutputUuids());
            else if (output.getParentOutputUuid() != null) parents.add(output.getParentOutputUuid());
        });
        Integer width = null;
        Map<String, ProcessOrderDetailVO.StageOutputVO> outputsByUuid = new HashMap<>();
        for (ProcessOrderDetailVO.StageOutputVO output : allOutputs) {
            if (output.getUuid() != null) outputsByUuid.put(output.getUuid(), output);
        }
        for (String parent : parents) {
            ProcessOrderDetailVO.StageOutputVO output = outputsByUuid.get(parent);
            if (output == null || output.getFinishWidth() == null || output.getFinishWidth() <= 0) return null;
            if (width == null) width = output.getFinishWidth();
            else if (!width.equals(output.getFinishWidth())) return null;
        }
        if (width != null) return width;
        List<ProcessOrderDetailVO.StageOutputVO> inherited = previousStageOutputs(stage, allOutputs);
        Set<Integer> inheritedWidths = inherited.stream().map(ProcessOrderDetailVO.StageOutputVO::getFinishWidth)
                .filter(value -> value != null && value > 0).collect(java.util.stream.Collectors.toSet());
        if (parents.isEmpty() && !inherited.isEmpty() && inheritedWidths.size() == 1) {
            return inheritedWidths.iterator().next();
        }
        return production.getActualWidth() != null && production.getActualWidth() > 0
                ? production.getActualWidth() : production.getOriginalWidth();
    }

    private static boolean isTrim(ProcessOrderDetailVO.StageOutputVO output) {
        return Integer.valueOf(1).equals(output.getIsRemain());
    }

    private static BigDecimal stageBasis(ProcessOrderDetailVO.RollProductionVO production,
                                         ProcessOrderDetailVO.StageOutputVO output) {
        if (Integer.valueOf(1).equals(output.getSourceStepType())) {
            return BigDecimal.valueOf(Math.max(1, output.getFinishWidth() == null ? 1 : output.getFinishWidth()));
        }
        if (isPositive(output.getEstimateWeight())) return output.getEstimateWeight();
        return BigDecimal.valueOf(Math.max(1, output.getFinishWidth() == null ? 1 : output.getFinishWidth()));
    }

    private record StageBudget(BigDecimal target, BigDecimal trim) {
    }

    private static void putEstimate(Map<String, BigDecimal> result, String key, BigDecimal estimate) {
        if (key != null && !key.isBlank()) {
            result.put(key, estimate);
        }
    }

    private static void allocateTrimOnlyGroup(
            Map<String, BigDecimal> result,
            List<ProcessOrderDetailVO.FinishProductionVO> group,
            List<ProcessOrderDetailVO.FinishProductionVO> trims,
            BigDecimal sourceWeight,
            BigDecimal lossWeight,
            Integer sourceWidth) {
        if (trims.isEmpty() || trims.stream().anyMatch(finish -> !hasTrimBasis(finish, sourceWidth))) {
            group.forEach(finish -> putTrimEstimate(result, finish, null));
            return;
        }
        BigDecimal target = sourceWeight.subtract(zeroIfNull(lossWeight)).max(BigDecimal.ZERO);
        BigDecimal measured = trims.stream().filter(ProcessOrderExportWeightResolver::isPositiveActual)
                .map(ProcessOrderDetailVO.FinishProductionVO::getActualWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ProcessOrderDetailVO.FinishProductionVO> unknown = trims.stream()
                .filter(finish -> !isPositiveActual(finish)).toList();
        BigDecimal remaining = target.subtract(measured);
        if (!unknown.isEmpty() && !isWholeNonNegative(remaining)) {
            unknown.forEach(finish -> putTrimEstimate(result, finish, null));
            return;
        }
        trims.stream().filter(ProcessOrderExportWeightResolver::isPositiveActual)
                .forEach(finish -> putTrimEstimate(result, finish,
                        IntegerWeightAllocator.roundTotal(finish.getActualWeight())));
        List<BigDecimal> weights = IntegerWeightAllocator.allocate(remaining.max(BigDecimal.ZERO),
                unknown.stream().map(finish -> basisForTrim(finish, sourceWidth)).toList());
        for (int index = 0; index < unknown.size(); index++) {
            putTrimEstimate(result, unknown.get(index), weights.get(index));
        }
    }

    private static boolean isPositiveActual(ProcessOrderDetailVO.FinishProductionVO finish) {
        return isPositive(finish.getActualWeight());
    }

    private static BigDecimal basisForTrim(
            ProcessOrderDetailVO.FinishProductionVO finish, Integer sourceWidth) {
        if (isPositive(finish.getTrimWeightShare())) return finish.getTrimWeightShare();
        if (isPositive(finish.getEstimateWeight())) return finish.getEstimateWeight();
        return BigDecimal.valueOf(Math.max(1, finish.getFinishWidth() == null
                ? sourceWidth == null ? 1 : sourceWidth : finish.getFinishWidth()));
    }

    private static void putTrimEstimate(
            Map<String, BigDecimal> result,
            ProcessOrderDetailVO.FinishProductionVO finish,
            BigDecimal estimate) {
        putEstimate(result, finish.getUuid(), estimate);
        putEstimate(result, finish.getFinishRollNo(), estimate);
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static boolean isWholeNonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0
                && value.stripTrailingZeros().scale() <= 0;
    }
}
