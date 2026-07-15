package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BackRecordOnSiteFinishRecorder {

    private static final int ROLL_NO_PRE = 1;
    private static final int ROLL_NO_VOID = 3;
    private static final int FORMAL = 0;
    private static final int PRODUCT = 0;
    private static final int SOURCE_PROCESS = 1;
    private static final int FINISH_PENDING = 1;

    private final FinishRollMapper finishRollMapper;
    private final BackRecordOnSiteFinishRelationWriter relationWriter;
    private final RollNoSequenceService rollNoSequenceService;

    public Result record(List<BackRecordFinishDTO> dtos, Context context) {
        BackRecordOnSiteFinishIndex index = new BackRecordOnSiteFinishIndex(
                context.rolls(), context.finishes(), context.relations());
        Set<String> managed = index.managedUuids();
        List<FinishRoll> created = new ArrayList<>();
        List<FinishOriginalRel> createdRelations = new ArrayList<>();
        Set<String> submitted = new LinkedHashSet<>();
        List<BackRecordFinishDTO> rows = dtos == null ? List.of() : dtos;
        validateCandidates(rows, index, submitted);
        int rowSort = nextRowSort(context.finishes());

        for (BackRecordFinishDTO dto : rows) {
            if (!index.accepts(dto)) {
                continue;
            }
            OriginalRoll source = index.requireSource(dto);
            FinishRoll finish = resolveFinish(dto, context.order(), source, index, rowSort++);
            BackRecordFinishRules.validateWidth(finish, dto, List.of(source), true);
            BackRecordFinishRules.requireActualWeight(finish, dto);
            applyActuals(finish, dto);
            if (!StringUtils.hasText(dto.getUuid())) {
                insertWithRollNo(finish);
            }
            FinishOriginalRel relation = persist(dto, context.order(), source, finish, index);
            submitted.add(finish.getUuid());
            managed.add(finish.getUuid());
            if (!index.hasFinish(finish.getUuid())) {
                created.add(finish);
            }
            if (relation != null) {
                createdRelations.add(relation);
            }
        }
        voidOmitted(managed, submitted, index);
        return new Result(created, createdRelations, managed);
    }

    private void validateCandidates(List<BackRecordFinishDTO> dtos,
                                    BackRecordOnSiteFinishIndex index,
        Set<String> submitted) {
        for (BackRecordFinishDTO dto : dtos) {
            if (!StringUtils.hasText(dto.getUuid())) {
                index.requireSource(dto);
                continue;
            }
            if (!index.accepts(dto)) {
                continue;
            }
            index.requireSource(dto);
            if (StringUtils.hasText(dto.getUuid()) && !submitted.add(dto.getUuid())) {
                throw new BusinessException("现场定尺成品回录明细重复：" + dto.getUuid());
            }
        }
    }

    private FinishRoll resolveFinish(BackRecordFinishDTO dto, ProcessOrder order,
                                     OriginalRoll source, BackRecordOnSiteFinishIndex index,
                                     int rowSort) {
        if (StringUtils.hasText(dto.getUuid())) {
            FinishRoll finish = index.finish(dto.getUuid());
            if (finish == null || !order.getUuid().equals(finish.getOrderUuid())) {
                throw new BusinessException("现场定尺成品不属于当前加工单");
            }
            return finish;
        }
        return buildFinish(order, source, rowSort);
    }

    private FinishRoll buildFinish(ProcessOrder order, OriginalRoll source, int rowSort) {
        FinishRoll finish = new FinishRoll();
        finish.setOrderUuid(order.getUuid());
        finish.setRowSort(rowSort);
        finish.setRollNoStatus(ROLL_NO_PRE);
        finish.setIsSpare(FORMAL);
        finish.setIsRemain(PRODUCT);
        finish.setPaperName(source.getPaperName());
        finish.setGramWeight(source.getActualGramWeight() != null
                ? source.getActualGramWeight() : source.getGramWeight());
        finish.setSourceType(SOURCE_PROCESS);
        finish.setFinishStatus(FINISH_PENDING);
        finish.setWarehouseUuid(order.getWarehouseUuid());
        finish.setOriginalRollNos(sourceKey(source));
        finish.setRemark("现场定尺回录新增成品");
        return finish;
    }

    private void applyActuals(FinishRoll finish, BackRecordFinishDTO dto) {
        finish.setFinishWidth(dto.getFinishWidth());
        finish.setFinishDiameter(dto.getFinishDiameter());
        finish.setFinishCoreDiameter(dto.getFinishCoreDiameter());
        finish.setActualWeight(dto.getActualWeight());
        finish.setRemainingWeight(dto.getActualWeight());
        finish.setScrapWeight(dto.getScrapWeight());
        finish.setIsRemain(PRODUCT);
        finish.setIsAbnormal(dto.getIsAbnormal());
        finish.setAbnormalType(dto.getAbnormalType());
        finish.setActualRemark(dto.getActualRemark());
    }

    private FinishOriginalRel persist(BackRecordFinishDTO dto, ProcessOrder order,
                                      OriginalRoll source, FinishRoll finish,
                                      BackRecordOnSiteFinishIndex index) {
        if (StringUtils.hasText(dto.getUuid())) {
            ConcurrencyGuard.requireRowUpdated(finishRollMapper.updateById(finish));
            if (index.hasLinkedSource(finish.getUuid())) {
                relationWriter.updateWeights(index.relations(finish.getUuid()), dto.getActualWeight());
                return null;
            }
        }
        return relationWriter.create(order, source, finish, dto.getActualWeight());
    }

    private void voidOmitted(Set<String> managed, Set<String> submitted,
                             BackRecordOnSiteFinishIndex index) {
        for (String uuid : managed) {
            FinishRoll finish = index.finish(uuid);
            if (finish == null || submitted.contains(uuid)
                    || Integer.valueOf(ROLL_NO_VOID).equals(finish.getRollNoStatus())) {
                continue;
            }
            finish.setRollNoStatus(ROLL_NO_VOID);
            finish.setActualRemark("现场定尺回录未使用，自动作废");
            ConcurrencyGuard.requireRowUpdated(finishRollMapper.updateById(finish));
        }
    }

    private void insertWithRollNo(FinishRoll finish) {
        for (int attempt = 0; attempt < 5; attempt++) {
            finish.setUuid(null);
            finish.setFinishRollNo(rollNoSequenceService.nextFinishRollNo());
            try {
                finishRollMapper.insert(finish);
                return;
            } catch (DuplicateKeyException ignored) {
                // 并发抢号后重新分配。
            }
        }
        throw new BusinessException("成品卷号分配冲突，请重试");
    }

    private int nextRowSort(List<FinishRoll> finishes) {
        return finishes.stream().mapToInt(finish -> finish.getRowSort() == null ? 0 : finish.getRowSort())
                .max().orElse(0) + 1;
    }

    private String sourceKey(OriginalRoll source) {
        return StringUtils.hasText(source.getRollNo()) ? source.getRollNo() : source.getUuid();
    }

    public record Context(ProcessOrder order, List<OriginalRoll> rolls,
                          List<FinishRoll> finishes, List<FinishOriginalRel> relations) {
    }

    public record Result(List<FinishRoll> finishes, List<FinishOriginalRel> relations,
                         Set<String> managedExistingUuids) {
    }
}
