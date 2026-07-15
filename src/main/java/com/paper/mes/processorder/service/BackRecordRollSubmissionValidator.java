package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.entity.OriginalRoll;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

public final class BackRecordRollSubmissionValidator {

    private BackRecordRollSubmissionValidator() {
    }

    public static void validate(List<OriginalRoll> expectedRolls, List<BackRecordRollDTO> submittedRolls) {
        Map<String, OriginalRoll> expected = expectedByUuid(expectedRolls);
        Set<String> submitted = submittedUuids(submittedRolls);

        Set<String> unknown = new LinkedHashSet<>(submitted);
        unknown.removeAll(expected.keySet());
        if (!unknown.isEmpty()) {
            throw new BusinessException("回录包含不属于本加工单的母卷：" + String.join("、", unknown));
        }

        Set<String> missing = new LinkedHashSet<>(expected.keySet());
        missing.removeAll(submitted);
        if (!missing.isEmpty()) {
            throw new BusinessException("以下母卷尚未回录，不能完成加工单：" + missingLabels(missing, expected));
        }
    }

    private static Map<String, OriginalRoll> expectedByUuid(List<OriginalRoll> rolls) {
        Map<String, OriginalRoll> expected = new LinkedHashMap<>();
        for (OriginalRoll roll : rolls == null ? List.<OriginalRoll>of() : rolls) {
            expected.put(roll.getUuid(), roll);
        }
        if (expected.isEmpty()) {
            throw new BusinessException("加工单没有可回录的母卷");
        }
        return expected;
    }

    private static Set<String> submittedUuids(List<BackRecordRollDTO> rolls) {
        Set<String> submitted = new LinkedHashSet<>();
        for (BackRecordRollDTO roll : rolls == null ? List.<BackRecordRollDTO>of() : rolls) {
            if (roll == null || !StringUtils.hasText(roll.getUuid())) {
                throw new BusinessException("母卷回录明细缺少有效标识");
            }
            if (!submitted.add(roll.getUuid())) {
                throw new BusinessException("母卷回录明细重复：" + roll.getUuid());
            }
        }
        return submitted;
    }

    private static String missingLabels(Set<String> missing, Map<String, OriginalRoll> expected) {
        return missing.stream().map(uuid -> {
            String rollNo = expected.get(uuid).getRollNo();
            return rollNo == null || rollNo.isBlank() ? uuid : rollNo;
        }).reduce((left, right) -> left + "、" + right).orElse("");
    }
}
