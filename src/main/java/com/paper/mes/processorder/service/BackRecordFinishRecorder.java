package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BackRecordFinishRecorder {
    private static final int ROLL_NO_VOID = 3;
    private static final int FINISH_STATUS_SCRAPPED = 4;
    private static final int RESULT_PRODUCED = 2;
    private static final int RESULT_NOT_PRODUCED = 3;
    private static final int RESULT_ADDED = 4;

    private final FinishRollMapper finishRollMapper;
    private final FinishRollSourceBinder sourceBinder;

    public void record(List<BackRecordFinishDTO> dtos, Context context) {
        Map<String, BackRecordFinishDTO> dtoByUuid = indexDtos(dtos);
        Map<String, OriginalRoll> rollByUuid = indexRolls(context.rolls());
        Map<String, List<OriginalRoll>> sourcesByFinish = sourceRolls(context.relations(), rollByUuid);
        for (FinishRoll finish : context.finishes()) {
            if (!BackRecordFinishRules.requiresRecord(finish)) {
                continue;
            }
            BackRecordFinishDTO dto = dtoByUuid.remove(finish.getUuid());
            if (dto == null) {
                throw new BusinessException("成品回录明细缺失：" + finish.getFinishRollNo());
            }
            List<OriginalRoll> sources = sourcesByFinish.getOrDefault(finish.getUuid(), List.of());
            if (sources.isEmpty() && dto.getOriginalUuid() != null && !dto.getOriginalUuid().isBlank()) {
                sources = bindSelectedSource(finish, dto, context);
            }
            recordOne(finish, dto, sources);
        }
        if (!dtoByUuid.isEmpty()) {
            throw new BusinessException("成品回录包含无效记录");
        }
    }

    private void recordOne(FinishRoll finish, BackRecordFinishDTO dto, List<OriginalRoll> sources) {
        BackRecordFinishAction action = BackRecordFinishAction.from(dto.getProductionAction());
        BackRecordFinishRules.requireValidActualMetadata(dto);
        if (action == BackRecordFinishAction.ADDED
                && !Integer.valueOf(RESULT_ADDED).equals(finish.getProductionResult())) {
            throw new BusinessException("新增成品不能复用已有成品编号");
        }
        if (BackRecordFinishRules.unusedSpare(finish, dto)) {
            BackRecordFinishRules.requireUnusedSpareBlank(finish, dto);
            return;
        }
        BackRecordFinishRules.requireSources(finish, sources);
        if (action == BackRecordFinishAction.NOT_PRODUCED) {
            markNotProduced(finish, dto);
            return;
        }
        BackRecordFinishRules.requireActualWeight(finish, dto);
        boolean onSite = BackRecordFinishRules.onSiteSources(finish, sources);
        BackRecordFinishRules.validateWidth(finish, dto, sources, onSite);
        applyActuals(finish, dto, onSite);
        ConcurrencyGuard.requireRowUpdated(finishRollMapper.updateById(finish));
    }

    private void markNotProduced(FinishRoll finish, BackRecordFinishDTO dto) {
        BackRecordFinishRules.requireAdjustmentReason(dto);
        String reason = dto.getProductionAdjustmentReason().trim();
        boolean actualAddition = Integer.valueOf(RESULT_ADDED).equals(finish.getProductionResult());
        finish.setRollNoStatus(ROLL_NO_VOID);
        finish.setFinishStatus(FINISH_STATUS_SCRAPPED);
        finish.setProductionResult(actualAddition ? RESULT_ADDED : RESULT_NOT_PRODUCED);
        finish.setProductionAdjustmentReason(reason);
        finish.setActualWeight(null);
        finish.setRemainingWeight(null);
        finish.setScrapWeight(null);
        finish.setIsAbnormal(0);
        finish.setAbnormalType(null);
        finish.setStockInTime(null);
        finish.setActualRemark(reason);
        BackRecordVoidedFinishWriter.write(finishRollMapper, finish);
    }

    private List<OriginalRoll> bindSelectedSource(FinishRoll finish, BackRecordFinishDTO dto,
                                                  Context context) {
        sourceBinder.bind(new FinishRollSourceBinder.BindRequest(
                finish.getOrderUuid(), finish, dto.getOriginalUuid(), "回录时补齐来源"));
        return context.rolls().stream()
                .filter(roll -> dto.getOriginalUuid().equals(roll.getUuid()))
                .toList();
    }

    private void applyActuals(FinishRoll finish, BackRecordFinishDTO dto, boolean onSite) {
        if (onSite) {
            finish.setFinishWidth(dto.getFinishWidth());
        }
        if (dto.getFinishDiameter() != null) {
            finish.setFinishDiameter(dto.getFinishDiameter());
        }
        if (dto.getFinishCoreDiameter() != null) {
            finish.setFinishCoreDiameter(dto.getFinishCoreDiameter());
        }
        finish.setActualWeight(dto.getActualWeight());
        finish.setRemainingWeight(dto.getActualWeight());
        finish.setScrapWeight(dto.getScrapWeight());
        if (dto.getIsRemain() != null) {
            finish.setIsRemain(dto.getIsRemain());
        }
        finish.setIsAbnormal(dto.getIsAbnormal() == null ? 0 : dto.getIsAbnormal());
        finish.setAbnormalType(BackRecordFinishRules.normalizedAbnormalType(dto));
        finish.setActualRemark(dto.getActualRemark());
        if (Integer.valueOf(1).equals(finish.getIsSpare())) {
            finish.setProductionResult(RESULT_ADDED);
            finish.setProductionAdjustmentReason("备用卷号实际启用");
        } else if (!Integer.valueOf(RESULT_ADDED).equals(finish.getProductionResult())) {
            finish.setProductionResult(RESULT_PRODUCED);
            finish.setProductionAdjustmentReason(null);
        }
    }

    private Map<String, BackRecordFinishDTO> indexDtos(List<BackRecordFinishDTO> dtos) {
        Map<String, BackRecordFinishDTO> result = new LinkedHashMap<>();
        for (BackRecordFinishDTO dto : dtos == null ? List.<BackRecordFinishDTO>of() : dtos) {
            if (result.put(dto.getUuid(), dto) != null) {
                throw new BusinessException("成品回录明细重复：" + dto.getUuid());
            }
        }
        return result;
    }

    private Map<String, OriginalRoll> indexRolls(List<OriginalRoll> rolls) {
        Map<String, OriginalRoll> result = new LinkedHashMap<>();
        rolls.forEach(roll -> result.put(roll.getUuid(), roll));
        return result;
    }

    private Map<String, List<OriginalRoll>> sourceRolls(List<FinishOriginalRel> relations,
                                                        Map<String, OriginalRoll> rolls) {
        Map<String, List<OriginalRoll>> result = new LinkedHashMap<>();
        for (FinishOriginalRel relation : relations) {
            OriginalRoll roll = rolls.get(relation.getOriginalUuid());
            if (roll != null) {
                result.computeIfAbsent(relation.getFinishUuid(), ignored -> new java.util.ArrayList<>()).add(roll);
            }
        }
        return result;
    }

    public record Context(List<FinishRoll> finishes, List<OriginalRoll> rolls,
                          List<FinishOriginalRel> relations) {
    }
}
