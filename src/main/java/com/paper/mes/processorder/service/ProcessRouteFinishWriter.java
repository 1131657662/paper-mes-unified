package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProcessRouteFinishWriter {

    private static final int ROLL_NO_PRE = 1;
    private static final int FORMAL_FINISH = 0;
    private static final int IS_REMAIN_NO = 0;
    private static final int IS_REMAIN_YES = 1;
    private static final int FINISH_STATUS_PENDING = 1;
    private static final int OUTPUT_FINISH_CREATED = 3;

    private final FinishRollMapper finishRollMapper;
    private final FinishOriginalRelMapper finishOriginalRelMapper;
    private final ProcessStageOutputMapper stageOutputMapper;
    private final RollNoSequenceService rollNoSequenceService;

    public void createFinalFinishes(ProcessRouteContext context,
                                    ProcessRoutePreviewVO preview,
                                    Map<String, ProcessStageOutput> outputsByKey) {
        int rowSort = nextFinishRowSort(context.order().getUuid());
        for (ProcessRoutePreviewVO.RouteOutputVO output : preview.getOutputs()) {
            if (Boolean.TRUE.equals(output.getConsumedByNextStage())) {
                continue;
            }
            FinishRoll finish = buildFinish(context, output, rowSort++);
            allocAndInsertFinish(finish);
            saveRel(context, finish, output.getEstimateWeight());
            markOutputFinishCreated(outputsByKey.get(output.getOutputKey()), finish);
        }
    }

    private FinishRoll buildFinish(ProcessRouteContext context,
                                   ProcessRoutePreviewVO.RouteOutputVO output,
                                   int rowSort) {
        FinishRoll finish = new FinishRoll();
        finish.setOrderUuid(context.order().getUuid());
        finish.setRowSort(rowSort);
        finish.setRollNoStatus(ROLL_NO_PRE);
        finish.setIsSpare(FORMAL_FINISH);
        finish.setIsRemain(isRemainOutput(output) ? IS_REMAIN_YES : IS_REMAIN_NO);
        finish.setSourceType(1);
        finish.setFinishStatus(FINISH_STATUS_PENDING);
        finish.setWarehouseUuid(context.order().getWarehouseUuid());
        finish.setOriginalRollNos(finishOriginalKey(context.roll()));
        finish.setPaperName(output.getPaperName());
        finish.setGramWeight(output.getGramWeight());
        finish.setFinishWidth(output.getFinishWidth());
        finish.setFinishDiameter(output.getFinishDiameter());
        finish.setFinishCoreDiameter(output.getFinishCoreDiameter());
        finish.setEstimateWeight(output.getEstimateWeight());
        finish.setEstimateWeightSnap(output.getEstimateWeight());
        finish.setRemark(isRemainOutput(output) ? "修边/余料" : "后续工艺最终产出：" + output.getOutputKey());
        return finish;
    }

    private boolean isRemainOutput(ProcessRoutePreviewVO.RouteOutputVO output) {
        return output.getIsRemain() != null && output.getIsRemain() == IS_REMAIN_YES;
    }

    private void saveRel(ProcessRouteContext context, FinishRoll finish, BigDecimal weight) {
        FinishOriginalRel rel = new FinishOriginalRel();
        rel.setOrderUuid(context.order().getUuid());
        rel.setFinishUuid(finish.getUuid());
        rel.setOriginalUuid(context.roll().getUuid());
        rel.setShareRatio(new BigDecimal("100.00"));
        rel.setShareWeight(weight);
        rel.setRemark("后续工艺最终产出");
        finishOriginalRelMapper.insert(rel);
    }

    private void markOutputFinishCreated(ProcessStageOutput output, FinishRoll finish) {
        output.setFinishRollUuid(finish.getUuid());
        output.setOutputStatus(OUTPUT_FINISH_CREATED);
        stageOutputMapper.updateById(output);
    }

    private void allocAndInsertFinish(FinishRoll finish) {
        for (int attempt = 0; attempt < 5; attempt++) {
            finish.setUuid(null);
            finish.setFinishRollNo(rollNoSequenceService.nextFinishRollNo());
            try {
                finishRollMapper.insert(finish);
                return;
            } catch (DuplicateKeyException ignored) {
                // 并发抢号后重试。
            }
        }
        throw new BusinessException("卷号分配冲突，请重试");
    }

    private int nextFinishRowSort(String orderUuid) {
        FinishRoll top = finishRollMapper.selectOne(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid)
                .orderByDesc(FinishRoll::getRowSort)
                .last("LIMIT 1"));
        return top == null || top.getRowSort() == null ? 1 : top.getRowSort() + 1;
    }

    private String finishOriginalKey(OriginalRoll roll) {
        return StringUtils.hasText(roll.getRollNo()) ? roll.getRollNo() : roll.getUuid();
    }
}
