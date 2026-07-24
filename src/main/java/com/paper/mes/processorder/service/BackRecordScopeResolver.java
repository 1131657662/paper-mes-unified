package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStep;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 将回录请求收敛到完整母卷闭合组，阻止跨批次修改关联成品。 */
@Component
public class BackRecordScopeResolver {

    public BackRecordScope resolve(List<OriginalRoll> allRolls, List<FinishRoll> allFinishes,
                                   List<ProcessStep> allSteps, List<FinishOriginalRel> allRelations,
                                   BackRecordDTO dto) {
        Map<String, OriginalRoll> rollByUuid = indexRolls(allRolls);
        Set<String> selectedIds = selectedRollIds(dto.getRolls(), rollByUuid);
        requireValidCompletionIntent(allRolls, selectedIds, dto);
        Map<String, Set<String>> sourcesByFinish = sourcesByFinish(allRelations);
        Set<String> finishIds = relatedFinishIds(selectedIds, sourcesByFinish);
        includeLegacyFinishes(dto.getFinishes(), allFinishes, sourcesByFinish, selectedIds, finishIds);
        if (selectedIds.size() == allRolls.size()) {
            includeUnlinkedFinishes(allFinishes, sourcesByFinish, finishIds);
        }
        List<OriginalRoll> rolls = allRolls.stream().filter(roll -> selectedIds.contains(roll.getUuid())).toList();
        List<FinishRoll> finishes = new java.util.ArrayList<>(
                allFinishes.stream().filter(finish -> finishIds.contains(finish.getUuid())).toList());
        List<ProcessStep> steps = allSteps.stream().filter(step -> selectedIds.contains(step.getOriginalUuid())).toList();
        List<FinishOriginalRel> relations = new java.util.ArrayList<>(allRelations.stream()
                .filter(relation -> finishIds.contains(relation.getFinishUuid()))
                .toList());
        return new BackRecordScope(rolls, finishes, steps, relations);
    }

    private void requireValidCompletionIntent(List<OriginalRoll> rolls, Set<String> selectedIds,
                                              BackRecordDTO dto) {
        boolean hasUnselected = rolls.stream()
                .filter(roll -> !Integer.valueOf(1).equals(roll.getIsChecked()))
                .anyMatch(roll -> !selectedIds.contains(roll.getUuid()));
        if (Boolean.TRUE.equals(dto.getCompleteOrder()) && hasUnselected) {
            throw new BusinessException(ErrorCode.E003, "完成整单必须包含全部未回录母卷");
        }
        if (!Boolean.TRUE.equals(dto.getCompleteOrder()) && !hasUnselected) {
            throw new BusinessException(ErrorCode.E003,
                    "本批已包含全部未回录母卷，请使用“完成整单”生成完成快照并计费");
        }
    }

    private Map<String, OriginalRoll> indexRolls(List<OriginalRoll> rolls) {
        Map<String, OriginalRoll> result = new LinkedHashMap<>();
        for (OriginalRoll roll : rolls) {
            result.put(roll.getUuid(), roll);
        }
        if (result.isEmpty()) {
            throw new BusinessException("加工单没有可回录的母卷");
        }
        return result;
    }

    private Set<String> selectedRollIds(List<BackRecordRollDTO> rows,
                                        Map<String, OriginalRoll> rollByUuid) {
        Set<String> result = new LinkedHashSet<>();
        for (BackRecordRollDTO row : rows == null ? List.<BackRecordRollDTO>of() : rows) {
            String uuid = row == null ? null : row.getUuid();
            if (!StringUtils.hasText(uuid) || !result.add(uuid)) {
                throw new BusinessException("母卷回录明细缺少有效标识或存在重复");
            }
            OriginalRoll roll = rollByUuid.get(uuid);
            if (roll == null) {
                throw new BusinessException(ErrorCode.E002, "回录母卷不属于当前加工单");
            }
            if (Integer.valueOf(1).equals(roll.getIsChecked())) {
                throw new BusinessException(ErrorCode.E004, "母卷已完成回录，不允许重复覆盖：" + rollLabel(roll));
            }
        }
        if (result.isEmpty()) {
            throw new BusinessException("请至少选择一卷母卷回录");
        }
        return result;
    }

    private Map<String, Set<String>> sourcesByFinish(List<FinishOriginalRel> relations) {
        Map<String, Set<String>> result = new HashMap<>();
        for (FinishOriginalRel relation : relations) {
            result.computeIfAbsent(relation.getFinishUuid(), ignored -> new LinkedHashSet<>())
                    .add(relation.getOriginalUuid());
        }
        return result;
    }

    private Set<String> relatedFinishIds(Set<String> selectedIds,
                                         Map<String, Set<String>> sourcesByFinish) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : sourcesByFinish.entrySet()) {
            boolean selected = entry.getValue().stream().anyMatch(selectedIds::contains);
            if (!selected) {
                continue;
            }
            if (!selectedIds.containsAll(entry.getValue())) {
                throw new BusinessException(ErrorCode.E003, "合并复卷的全部来源母卷必须在同一批回录");
            }
            result.add(entry.getKey());
        }
        return result;
    }

    private void includeLegacyFinishes(List<BackRecordFinishDTO> rows, List<FinishRoll> finishes,
                                       Map<String, Set<String>> sourcesByFinish,
                                       Set<String> selectedIds, Set<String> finishIds) {
        Map<String, FinishRoll> finishByUuid = new HashMap<>();
        finishes.forEach(finish -> finishByUuid.put(finish.getUuid(), finish));
        for (BackRecordFinishDTO row : rows == null ? List.<BackRecordFinishDTO>of() : rows) {
            if (row == null) {
                throw new BusinessException("成品回录明细不能包含空记录");
            }
            if (!StringUtils.hasText(row.getUuid())) {
                requireSelectedSource(row, selectedIds);
                continue;
            }
            FinishRoll finish = finishByUuid.get(row.getUuid());
            if (finish == null) {
                throw new BusinessException(ErrorCode.E002, "成品回录记录不存在");
            }
            if (finishIds.contains(finish.getUuid())) {
                continue;
            }
            if (!sourcesByFinish.containsKey(finish.getUuid())) {
                requireSelectedSource(row, selectedIds);
                finishIds.add(finish.getUuid());
                continue;
            }
            throw new BusinessException(ErrorCode.E003, "成品不属于本批回录母卷：" + finish.getFinishRollNo());
        }
    }

    private void requireSelectedSource(BackRecordFinishDTO row, Set<String> selectedIds) {
        if (!StringUtils.hasText(row.getOriginalUuid()) || !selectedIds.contains(row.getOriginalUuid())) {
            throw new BusinessException(ErrorCode.E003, "未关联成品必须选择本批来源母卷");
        }
    }

    private void includeUnlinkedFinishes(List<FinishRoll> finishes,
                                         Map<String, Set<String>> sourcesByFinish,
                                         Set<String> finishIds) {
        finishes.stream()
                .filter(finish -> !sourcesByFinish.containsKey(finish.getUuid()))
                .map(FinishRoll::getUuid)
                .forEach(finishIds::add);
    }

    private String rollLabel(OriginalRoll roll) {
        if (StringUtils.hasText(roll.getRollNo())) {
            return roll.getRollNo();
        }
        return StringUtils.hasText(roll.getExtraNo()) ? roll.getExtraNo() : roll.getUuid();
    }
}
