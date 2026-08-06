package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 按母卷 UUID 幂等生成或恢复原纸直发库存。 */
@Component
@RequiredArgsConstructor
public class BackRecordDirectShipRecorder {

    private static final int PROCESS_MODE_DIRECT_SHIP = 3;
    private static final int SOURCE_DIRECT_SHIP = 2;
    private static final int ROLL_NO_USED = 2;
    private static final int FINISH_IN_STOCK = 2;
    private static final int RESULT_PRODUCED = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final FinishRollMapper finishRollMapper;
    private final FinishOriginalRelMapper relationMapper;
    private final RollNoSequenceService rollNoSequenceService;

    public Result record(ProcessOrder order, List<OriginalRoll> rolls, List<OriginalRoll> allOrderRolls) {
        List<OriginalRoll> sources = directSources(rolls);
        if (sources.isEmpty()) {
            return new Result(0, List.of());
        }
        List<FinishRoll> allFinishes = loadFinishes(order.getUuid());
        List<FinishRoll> directFinishes = allFinishes.stream()
                .filter(finish -> Integer.valueOf(SOURCE_DIRECT_SHIP).equals(finish.getSourceType()))
                .toList();
        List<FinishOriginalRel> relations = loadRelations(order.getUuid());
        Map<String, List<FinishRoll>> assigned = DirectShipFinishMatcher.assign(
                directSources(allOrderRolls), directFinishes, relations);
        Map<String, FinishOriginalRel> relationIndex = relationIndex(relations);
        int nextRowSort = maxRowSort(allFinishes);
        int generated = 0;
        List<FinishRoll> touched = new ArrayList<>();
        for (OriginalRoll source : sources) {
            List<FinishRoll> matched = assigned.getOrDefault(source.getUuid(), List.of());
            List<BigDecimal> pieceWeights = DirectShipPiecePlan.from(source).weights();
            for (int index = 0; index < pieceWeights.size(); index++) {
                FinishRoll finish = index < matched.size() ? matched.get(index) : null;
                BigDecimal pieceWeight = pieceWeights.get(index);
                if (finish == null) {
                    finish = create(order, source, pieceWeight, ++nextRowSort);
                    generated++;
                } else {
                    update(order, source, pieceWeight, finish);
                }
                upsertRelation(order, source, finish, pieceWeight, relationIndex);
                touched.add(finish);
            }
        }
        return new Result(generated, touched);
    }

    private List<OriginalRoll> directSources(List<OriginalRoll> rolls) {
        return rolls.stream()
                .filter(roll -> Integer.valueOf(PROCESS_MODE_DIRECT_SHIP).equals(roll.getProcessMode()))
                .sorted(Comparator.comparing(OriginalRoll::getRowSort,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<FinishRoll> loadFinishes(String orderUuid) {
        return finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid)
                .orderByAsc(FinishRoll::getRowSort));
    }

    private List<FinishOriginalRel> loadRelations(String orderUuid) {
        return relationMapper.selectList(new LambdaQueryWrapper<FinishOriginalRel>()
                .eq(FinishOriginalRel::getOrderUuid, orderUuid));
    }

    private FinishRoll create(ProcessOrder order, OriginalRoll source, BigDecimal pieceWeight, int rowSort) {
        FinishRoll finish = new FinishRoll();
        finish.setOrderUuid(order.getUuid());
        finish.setRowSort(rowSort);
        applyActuals(order, source, pieceWeight, finish);
        insertWithBusinessNo(finish);
        return finish;
    }

    private void update(ProcessOrder order, OriginalRoll source, BigDecimal pieceWeight, FinishRoll finish) {
        boolean needsBusinessNumber = !StringUtils.hasText(finish.getFinishRollNo());
        applyActuals(order, source, pieceWeight, finish);
        if (needsBusinessNumber) {
            updateWithBusinessNo(finish);
            return;
        }
        ConcurrencyGuard.requireRowUpdated(finishRollMapper.updateById(finish));
    }

    private void updateWithBusinessNo(FinishRoll finish) {
        for (int attempt = 0; attempt < 5; attempt++) {
            finish.setFinishRollNo(rollNoSequenceService.nextFinishRollNo());
            try {
                ConcurrencyGuard.requireRowUpdated(finishRollMapper.updateById(finish));
                return;
            } catch (DuplicateKeyException ignored) {
                FinishRoll current = finishRollMapper.selectById(finish.getUuid());
                finish.setVersion(current == null ? finish.getVersion() : current.getVersion());
            }
        }
        throw new BusinessException("直发成品卷号迁移冲突，请重试");
    }

    private void applyActuals(ProcessOrder order, OriginalRoll source, BigDecimal pieceWeight, FinishRoll finish) {
        finish.setRollNoStatus(ROLL_NO_USED);
        finish.setIsSpare(0);
        finish.setIsRemain(0);
        finish.setPaperName(source.getPaperName());
        finish.setGramWeight(source.getActualGramWeight() != null
                ? source.getActualGramWeight() : source.getGramWeight());
        finish.setFinishWidth(source.getActualWidth() != null
                ? source.getActualWidth() : source.getOriginalWidth());
        finish.setSourceType(SOURCE_DIRECT_SHIP);
        finish.setActualWeight(pieceWeight);
        finish.setRemainingWeight(pieceWeight);
        finish.setEstimateWeight(pieceWeight);
        finish.setFinishStatus(FINISH_IN_STOCK);
        finish.setProductionResult(RESULT_PRODUCED);
        finish.setProductionAdjustmentReason(null);
        finish.setWarehouseUuid(order.getWarehouseUuid());
        finish.setOriginalRollNos(sourceKey(source));
    }

    private void insertWithBusinessNo(FinishRoll finish) {
        for (int attempt = 0; attempt < 5; attempt++) {
            finish.setUuid(null);
            finish.setFinishRollNo(rollNoSequenceService.nextFinishRollNo());
            try {
                finishRollMapper.insert(finish);
                return;
            } catch (DuplicateKeyException ignored) {
                // 并发抢号后重新申请全局成品号。
            }
        }
        throw new BusinessException("直发成品卷号分配冲突，请重试");
    }

    private void upsertRelation(ProcessOrder order, OriginalRoll source, FinishRoll finish,
                                BigDecimal pieceWeight,
                                Map<String, FinishOriginalRel> relationIndex) {
        String key = source.getUuid() + ":" + finish.getUuid();
        FinishOriginalRel relation = relationIndex.get(key);
        if (relation == null) {
            relation = new FinishOriginalRel();
            relation.setOrderUuid(order.getUuid());
            relation.setOriginalUuid(source.getUuid());
            relation.setFinishUuid(finish.getUuid());
            relation.setRemark("原纸直发来源");
            relationIndex.put(key, relation);
            relation.setShareRatio(HUNDRED);
            relation.setShareWeight(pieceWeight);
            relationMapper.insert(relation);
            return;
        }
        relation.setShareRatio(HUNDRED);
        relation.setShareWeight(pieceWeight);
        ConcurrencyGuard.requireRowUpdated(relationMapper.updateById(relation));
    }

    private Map<String, FinishOriginalRel> relationIndex(List<FinishOriginalRel> relations) {
        Map<String, FinishOriginalRel> result = new HashMap<>();
        relations.forEach(relation -> result.put(
                relation.getOriginalUuid() + ":" + relation.getFinishUuid(), relation));
        return result;
    }

    private int maxRowSort(List<FinishRoll> finishes) {
        return finishes.stream().mapToInt(finish -> finish.getRowSort() == null ? 0 : finish.getRowSort())
                .max().orElse(0);
    }

    private String sourceKey(OriginalRoll source) {
        return StringUtils.hasText(source.getRollNo()) ? source.getRollNo() : source.getUuid();
    }

    public record Result(int generated, List<FinishRoll> finishes) {
    }
}
