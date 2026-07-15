package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordTrimDTO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BackRecordOnSiteTrimRecorder {

    private static final int PROCESS_MODE_ON_SITE = 2;
    private static final int ROLL_NO_PRE = 1;
    private static final int FORMAL = 0;
    private static final int REMAIN = 1;
    private static final int SOURCE_PROCESS = 1;
    private static final int FINISH_PENDING = 1;

    private final FinishRollMapper finishRollMapper;
    private final FinishOriginalRelMapper relationMapper;
    private final RollNoSequenceService rollNoSequenceService;

    public Result record(List<BackRecordTrimDTO> dtos, Context context) {
        if (dtos == null || dtos.isEmpty()) {
            return Result.empty();
        }
        Map<String, OriginalRoll> sources = indexSources(context.rolls());
        validateWidths(dtos, sources);
        int rowSort = nextRowSort(context.finishes());
        List<FinishRoll> finishes = new ArrayList<>();
        List<FinishOriginalRel> relations = new ArrayList<>();
        for (BackRecordTrimDTO dto : dtos) {
            OriginalRoll source = requireOnSiteSource(dto, sources);
            FinishRoll finish = buildFinish(context.order(), source, dto, rowSort++);
            insertWithRollNo(finish);
            FinishOriginalRel relation = saveRelation(context.order(), source, finish, dto);
            finishes.add(finish);
            relations.add(relation);
        }
        return new Result(finishes, relations);
    }

    private void validateWidths(List<BackRecordTrimDTO> dtos, Map<String, OriginalRoll> sources) {
        Map<String, Integer> totalBySource = new LinkedHashMap<>();
        for (BackRecordTrimDTO dto : dtos) {
            OriginalRoll source = requireOnSiteSource(dto, sources);
            if (dto.getFinishWidth() == null || dto.getFinishWidth() <= 0) {
                throw new BusinessException("切边宽度必须大于0");
            }
            totalBySource.merge(source.getUuid(), dto.getFinishWidth(), Integer::sum);
        }
        totalBySource.forEach((uuid, total) -> requireWithinSourceWidth(sources.get(uuid), total));
    }

    private OriginalRoll requireOnSiteSource(BackRecordTrimDTO dto, Map<String, OriginalRoll> sources) {
        OriginalRoll source = sources.get(dto.getOriginalUuid());
        if (source == null) {
            throw new BusinessException("切边来源母卷不属于当前加工单");
        }
        if (!Integer.valueOf(PROCESS_MODE_ON_SITE).equals(source.getProcessMode())) {
            throw new BusinessException("只有现场定尺母卷可以在回录时新增切边");
        }
        if (dto.getActualWeight() == null || dto.getActualWeight().signum() <= 0) {
            throw new BusinessException("切边实际重量必须大于0");
        }
        return source;
    }

    private void requireWithinSourceWidth(OriginalRoll source, int trimWidth) {
        int sourceWidth = effectiveWidth(source);
        if (sourceWidth <= 0) {
            throw new BusinessException("来源母卷门幅无效，无法新增切边");
        }
        if (trimWidth > sourceWidth) {
            throw new BusinessException("切边宽度合计不能超过来源母卷门幅 " + sourceWidth + "mm");
        }
    }

    private FinishRoll buildFinish(ProcessOrder order, OriginalRoll source,
                                   BackRecordTrimDTO dto, int rowSort) {
        FinishRoll finish = new FinishRoll();
        finish.setOrderUuid(order.getUuid());
        finish.setRowSort(rowSort);
        finish.setRollNoStatus(ROLL_NO_PRE);
        finish.setIsSpare(FORMAL);
        finish.setIsRemain(REMAIN);
        finish.setPaperName(source.getPaperName());
        finish.setGramWeight(source.getActualGramWeight() != null
                ? source.getActualGramWeight() : source.getGramWeight());
        finish.setFinishWidth(dto.getFinishWidth());
        finish.setSourceType(SOURCE_PROCESS);
        finish.setActualWeight(dto.getActualWeight());
        finish.setRemainingWeight(dto.getActualWeight());
        finish.setFinishStatus(FINISH_PENDING);
        finish.setWarehouseUuid(order.getWarehouseUuid());
        finish.setOriginalRollNos(sourceKey(source));
        finish.setActualRemark(dto.getActualRemark());
        finish.setRemark("现场定尺切边/余料");
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
                // 并发抢号后重新分配。
            }
        }
        throw new BusinessException("切边卷号分配冲突，请重试");
    }

    private FinishOriginalRel saveRelation(ProcessOrder order, OriginalRoll source,
                                           FinishRoll finish, BackRecordTrimDTO dto) {
        FinishOriginalRel relation = new FinishOriginalRel();
        relation.setOrderUuid(order.getUuid());
        relation.setFinishUuid(finish.getUuid());
        relation.setOriginalUuid(source.getUuid());
        relation.setShareRatio(new BigDecimal("100.00"));
        relation.setShareWeight(dto.getActualWeight());
        relation.setRemark("现场定尺回录新增切边");
        relationMapper.insert(relation);
        return relation;
    }

    private Map<String, OriginalRoll> indexSources(List<OriginalRoll> rolls) {
        Map<String, OriginalRoll> result = new LinkedHashMap<>();
        rolls.forEach(roll -> result.put(roll.getUuid(), roll));
        return result;
    }

    private int nextRowSort(List<FinishRoll> finishes) {
        return finishes.stream().mapToInt(finish -> finish.getRowSort() == null ? 0 : finish.getRowSort())
                .max().orElse(0) + 1;
    }

    private int effectiveWidth(OriginalRoll roll) {
        return roll.getActualWidth() != null && roll.getActualWidth() > 0
                ? roll.getActualWidth() : roll.getOriginalWidth() == null ? 0 : roll.getOriginalWidth();
    }

    private String sourceKey(OriginalRoll source) {
        return StringUtils.hasText(source.getRollNo()) ? source.getRollNo() : source.getUuid();
    }

    public record Context(ProcessOrder order, List<OriginalRoll> rolls, List<FinishRoll> finishes) {
    }

    public record Result(List<FinishRoll> finishes, List<FinishOriginalRel> relations) {
        private static Result empty() {
            return new Result(List.of(), List.of());
        }
    }
}
