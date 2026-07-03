package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class RollLossSummaryCalculator {

    private static final int PROCESS_MODE_DIRECT_SHIP = 3;
    private static final int SOURCE_DIRECT_SHIP = 2;
    private static final int ROLL_NO_VOID = 3;
    private static final int IS_SPARE_YES = 1;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private RollLossSummaryCalculator() {
    }

    public static Map<String, BigDecimal> calculate(List<OriginalRoll> rolls, List<ProcessStep> steps,
                                                    List<FinishRoll> finishes, List<FinishOriginalRel> rels) {
        Set<String> rollUuids = processRollUuids(rolls);
        Map<String, BigDecimal> lossByRoll = new LinkedHashMap<>();
        addStepLosses(lossByRoll, rollUuids, steps);
        addFinishLosses(lossByRoll, rollUuids, finishes, rels);
        return lossByRoll;
    }

    private static Set<String> processRollUuids(List<OriginalRoll> rolls) {
        return rolls.stream()
                .filter(roll -> PROCESS_MODE_DIRECT_SHIP != (roll.getProcessMode() == null ? 0 : roll.getProcessMode()))
                .map(OriginalRoll::getUuid)
                .collect(Collectors.toSet());
    }

    private static void addStepLosses(Map<String, BigDecimal> lossByRoll, Set<String> rollUuids,
                                      List<ProcessStep> steps) {
        for (ProcessStep step : steps) {
            if (!rollUuids.contains(step.getOriginalUuid())) {
                continue;
            }
            add(lossByRoll, step.getOriginalUuid(), step.getLossWeight());
        }
    }

    private static void addFinishLosses(Map<String, BigDecimal> lossByRoll, Set<String> rollUuids,
                                        List<FinishRoll> finishes, List<FinishOriginalRel> rels) {
        Map<String, FinishRoll> finishByUuid = finishes.stream()
                .collect(Collectors.toMap(FinishRoll::getUuid, finish -> finish, (left, right) -> left));
        for (FinishOriginalRel rel : rels) {
            FinishRoll finish = finishByUuid.get(rel.getFinishUuid());
            if (!rollUuids.contains(rel.getOriginalUuid()) || shouldSkipFinish(finish)) {
                continue;
            }
            BigDecimal ratio = rel.getShareRatio() == null ? HUNDRED : rel.getShareRatio();
            add(lossByRoll, rel.getOriginalUuid(), share(finish.getScrapWeight(), ratio));
            add(lossByRoll, rel.getOriginalUuid(), share(finish.getTrimWeightShare(), ratio));
        }
    }

    private static boolean shouldSkipFinish(FinishRoll finish) {
        if (finish == null) {
            return true;
        }
        return SOURCE_DIRECT_SHIP == (finish.getSourceType() == null ? 0 : finish.getSourceType())
                || ROLL_NO_VOID == (finish.getRollNoStatus() == null ? 0 : finish.getRollNoStatus())
                || IS_SPARE_YES == (finish.getIsSpare() == null ? 0 : finish.getIsSpare());
    }

    private static BigDecimal share(BigDecimal weight, BigDecimal ratio) {
        if (weight == null) {
            return BigDecimal.ZERO;
        }
        return weight.multiply(ratio).divide(HUNDRED, 3, RoundingMode.HALF_UP);
    }

    private static void add(Map<String, BigDecimal> lossByRoll, String rollUuid, BigDecimal weight) {
        BigDecimal current = lossByRoll.getOrDefault(rollUuid, BigDecimal.ZERO);
        lossByRoll.put(rollUuid, current.add(weight == null ? BigDecimal.ZERO : weight));
    }
}
