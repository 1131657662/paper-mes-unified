package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** 在标准加工回录中生成现场实际新增成品。 */
@Component
@RequiredArgsConstructor
public class BackRecordStandardFinishAdditionRecorder {

    private static final int ROLL_NO_PRE = 1;
    private static final int FINISH_STATUS_PENDING = 1;
    private static final int SOURCE_PROCESS = 1;
    private static final int RESULT_ADDED = 4;
    private static final int PROCESS_MODE_STANDARD = 1;

    private final FinishRollMapper finishRollMapper;
    private final FinishRollSourceBinder sourceBinder;
    private final RollNoSequenceService rollNoSequenceService;

    public Result record(List<BackRecordFinishDTO> dtos, Context context) {
        List<FinishRoll> created = new ArrayList<>();
        List<FinishOriginalRel> relations = new ArrayList<>();
        int rowSort = nextRowSort(context.finishes());
        for (BackRecordFinishDTO dto : dtos == null ? List.<BackRecordFinishDTO>of() : dtos) {
            if (BackRecordFinishAction.from(dto.getProductionAction()) != BackRecordFinishAction.ADDED) continue;
            if (StringUtils.hasText(dto.getUuid())) {
                throw new BusinessException("新增成品不能携带已有成品编号");
            }
            OriginalRoll source = requireStandardSource(dto, context.rolls());
            BackRecordFinishRules.requireValidActualMetadata(dto);
            BackRecordFinishRules.requireAdjustmentReason(dto);
            BackRecordFinishRules.requireAddedSpec(dto, source);
            BackRecordFinishRules.requireActualWeight(dto);

            FinishRoll finish = buildFinish(context.orderUuid(), source, dto, ++rowSort);
            insertWithRollNo(finish);
            sourceBinder.bind(new FinishRollSourceBinder.BindRequest(
                    context.orderUuid(), finish, source.getUuid(), "标准加工回录新增成品"));
            FinishOriginalRel relation = new FinishOriginalRel();
            relation.setOrderUuid(context.orderUuid());
            relation.setFinishUuid(finish.getUuid());
            relation.setOriginalUuid(source.getUuid());
            relations.add(relation);
            created.add(finish);
        }
        return new Result(created, relations);
    }

    private OriginalRoll requireStandardSource(BackRecordFinishDTO dto, List<OriginalRoll> rolls) {
        if (!StringUtils.hasText(dto.getOriginalUuid())) {
            throw new BusinessException("新增标准成品必须选择来源母卷");
        }
        OriginalRoll source = rolls.stream()
                .filter(roll -> dto.getOriginalUuid().equals(roll.getUuid()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("新增成品来源母卷不属于本批回录"));
        if (Integer.valueOf(2).equals(source.getProcessMode())) {
            throw new BusinessException("现场定尺成品请使用现场实际产出录入");
        }
        if (Integer.valueOf(3).equals(source.getProcessMode())) {
            throw new BusinessException("直发母卷不能新增加工成品");
        }
        if (!Integer.valueOf(PROCESS_MODE_STANDARD).equals(source.getProcessMode())) {
            throw new BusinessException("新增成品仅支持标准加工母卷");
        }
        return source;
    }

    private FinishRoll buildFinish(String orderUuid, OriginalRoll source,
                                   BackRecordFinishDTO dto, int rowSort) {
        FinishRoll finish = new FinishRoll();
        finish.setOrderUuid(orderUuid);
        finish.setRowSort(rowSort);
        finish.setRollNoStatus(ROLL_NO_PRE);
        finish.setIsSpare(0);
        finish.setIsRemain(0);
        finish.setPaperName(source.getPaperName());
        finish.setGramWeight(source.getActualGramWeight() != null
                ? source.getActualGramWeight() : source.getGramWeight());
        finish.setFinishWidth(dto.getFinishWidth());
        finish.setFinishDiameter(dto.getFinishDiameter());
        finish.setFinishCoreDiameter(dto.getFinishCoreDiameter());
        finish.setSourceType(SOURCE_PROCESS);
        finish.setActualWeight(dto.getActualWeight());
        finish.setRemainingWeight(dto.getActualWeight());
        finish.setScrapWeight(dto.getScrapWeight());
        finish.setIsRemain(0);
        finish.setIsAbnormal(dto.getIsAbnormal() == null ? 0 : dto.getIsAbnormal());
        finish.setAbnormalType(BackRecordFinishRules.normalizedAbnormalType(dto));
        finish.setActualRemark(dto.getActualRemark());
        finish.setProductionResult(RESULT_ADDED);
        finish.setProductionAdjustmentReason(dto.getProductionAdjustmentReason().trim());
        finish.setFinishStatus(FINISH_STATUS_PENDING);
        finish.setOriginalRollNos(source.getRollNo() == null ? source.getUuid() : source.getRollNo());
        return finish;
    }

    private void insertWithRollNo(FinishRoll finish) {
        for (int attempt = 0; attempt < 5; attempt++) {
            finish.setUuid(null);
            finish.setFinishRollNo(rollNoSequenceService.nextFinishRollNo());
            try {
                finishRollMapper.insert(finish);
                return;
            } catch (DuplicateKeyException ignored) {
                // 并发抢号时重新申请全局卷号。
            }
        }
        throw new BusinessException("新增成品卷号分配冲突，请重试");
    }

    private int nextRowSort(List<FinishRoll> finishes) {
        return finishes.stream().mapToInt(finish -> finish.getRowSort() == null ? 0 : finish.getRowSort())
                .max().orElse(0);
    }

    public record Context(String orderUuid, List<OriginalRoll> rolls, List<FinishRoll> finishes) {
    }

    public record Result(List<FinishRoll> finishes, List<FinishOriginalRel> relations) {
    }
}
