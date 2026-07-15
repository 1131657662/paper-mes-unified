package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BackRecordOnSiteFinishIndex {

    private static final int PROCESS_MODE_ON_SITE = 2;

    private final Map<String, OriginalRoll> sources = new LinkedHashMap<>();
    private final Map<String, FinishRoll> finishes = new LinkedHashMap<>();
    private final Map<String, String> linkedSources = new LinkedHashMap<>();
    private final Map<String, List<FinishOriginalRel>> relations = new LinkedHashMap<>();

    BackRecordOnSiteFinishIndex(List<OriginalRoll> rolls, List<FinishRoll> finishRolls,
                                List<FinishOriginalRel> relations) {
        rolls.forEach(roll -> sources.put(roll.getUuid(), roll));
        finishRolls.forEach(finish -> finishes.put(finish.getUuid(), finish));
        relations.forEach(relation -> {
            linkedSources.putIfAbsent(relation.getFinishUuid(), relation.getOriginalUuid());
            this.relations.computeIfAbsent(
                    relation.getFinishUuid(), ignored -> new java.util.ArrayList<>()).add(relation);
        });
    }

    boolean accepts(BackRecordFinishDTO dto) {
        if (StringUtils.hasText(dto.getUuid())) {
            return isOnSite(sources.get(linkedSources.get(dto.getUuid())));
        }
        return isOnSite(sources.get(dto.getOriginalUuid()));
    }

    OriginalRoll requireSource(BackRecordFinishDTO dto) {
        String linkedSourceUuid = linkedSources.get(dto.getUuid());
        if (StringUtils.hasText(linkedSourceUuid)
                && StringUtils.hasText(dto.getOriginalUuid())
                && !linkedSourceUuid.equals(dto.getOriginalUuid())) {
            throw new BusinessException("现场定尺成品来源与原记录不一致");
        }
        String sourceUuid = StringUtils.hasText(linkedSourceUuid)
                ? linkedSourceUuid : dto.getOriginalUuid();
        OriginalRoll source = sources.get(sourceUuid);
        if (source == null) {
            throw new BusinessException("现场定尺成品必须选择来源母卷");
        }
        if (!isOnSite(source)) {
            throw new BusinessException("只有现场定尺母卷可以在回录时新增成品");
        }
        return source;
    }

    FinishRoll finish(String uuid) {
        return finishes.get(uuid);
    }

    boolean hasFinish(String uuid) {
        return finishes.containsKey(uuid);
    }

    boolean hasLinkedSource(String finishUuid) {
        return linkedSources.containsKey(finishUuid);
    }

    List<FinishOriginalRel> relations(String finishUuid) {
        return relations.getOrDefault(finishUuid, List.of());
    }

    Set<String> managedUuids() {
        Set<String> result = new LinkedHashSet<>();
        linkedSources.forEach((finishUuid, sourceUuid) -> {
            if (isOnSite(sources.get(sourceUuid)) && finishes.containsKey(finishUuid)) {
                result.add(finishUuid);
            }
        });
        return result;
    }

    private boolean isOnSite(OriginalRoll source) {
        return source != null && Integer.valueOf(PROCESS_MODE_ON_SITE).equals(source.getProcessMode());
    }
}
