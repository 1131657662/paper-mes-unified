package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 校验现场定尺每卷母卷的成品与余料总门幅。 */
public final class BackRecordOnSiteWidthValidator {

    private static final int PROCESS_MODE_ON_SITE = 2;
    private static final int ROLL_NO_VOID = 3;
    private static final int FINISH_SCRAPPED = 4;

    private BackRecordOnSiteWidthValidator() {
    }

    public static void validate(List<OriginalRoll> rolls, List<FinishRoll> finishes,
                                List<FinishOriginalRel> relations) {
        Map<String, OriginalRoll> sources = onSiteSources(rolls);
        if (sources.isEmpty()) {
            return;
        }
        Map<String, FinishRoll> finishByUuid = new HashMap<>();
        finishes.forEach(finish -> finishByUuid.put(finish.getUuid(), finish));
        Map<String, Integer> usedWidth = usedWidths(sources, finishByUuid, relations);
        usedWidth.forEach((sourceUuid, width) -> requireWithinCapacity(sources.get(sourceUuid), width));
    }

    private static Map<String, OriginalRoll> onSiteSources(List<OriginalRoll> rolls) {
        Map<String, OriginalRoll> result = new HashMap<>();
        rolls.stream()
                .filter(roll -> Integer.valueOf(PROCESS_MODE_ON_SITE).equals(roll.getProcessMode()))
                .forEach(roll -> result.put(roll.getUuid(), roll));
        return result;
    }

    private static Map<String, Integer> usedWidths(Map<String, OriginalRoll> sources,
                                                   Map<String, FinishRoll> finishes,
                                                   List<FinishOriginalRel> relations) {
        Map<String, Integer> result = new HashMap<>();
        Set<String> counted = new HashSet<>();
        for (FinishOriginalRel relation : relations) {
            FinishRoll finish = finishes.get(relation.getFinishUuid());
            String pair = relation.getOriginalUuid() + ":" + relation.getFinishUuid();
            if (!sources.containsKey(relation.getOriginalUuid()) || !counted.add(pair)
                    || !isActiveOutput(finish)) {
                continue;
            }
            result.merge(relation.getOriginalUuid(), finish.getFinishWidth(), Integer::sum);
        }
        return result;
    }

    private static boolean isActiveOutput(FinishRoll finish) {
        return finish != null && finish.getFinishWidth() != null && finish.getFinishWidth() > 0
                && !Integer.valueOf(ROLL_NO_VOID).equals(finish.getRollNoStatus())
                && !Integer.valueOf(FINISH_SCRAPPED).equals(finish.getFinishStatus());
    }

    private static void requireWithinCapacity(OriginalRoll source, int usedWidth) {
        int sourceWidth = effectiveWidth(source);
        int pieces = source.getPieceNum() == null || source.getPieceNum() <= 0 ? 1 : source.getPieceNum();
        long capacity = (long) sourceWidth * pieces;
        if (sourceWidth <= 0 || usedWidth > capacity) {
            throw new BusinessException("现场定尺成品与余料门幅合计不能超过来源母卷可用门幅 "
                    + capacity + "mm：" + sourceLabel(source));
        }
    }

    private static int effectiveWidth(OriginalRoll roll) {
        return roll.getActualWidth() != null && roll.getActualWidth() > 0
                ? roll.getActualWidth() : roll.getOriginalWidth() == null ? 0 : roll.getOriginalWidth();
    }

    private static String sourceLabel(OriginalRoll roll) {
        if (StringUtils.hasText(roll.getRollNo())) {
            return roll.getRollNo();
        }
        return StringUtils.hasText(roll.getExtraNo()) ? roll.getExtraNo() : roll.getUuid();
    }
}
