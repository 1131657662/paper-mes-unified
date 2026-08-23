package com.paper.mes.processorder.service;

import com.paper.mes.processorder.calc.IntegerWeightAllocator;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.model.WeightStatus;

import java.math.BigDecimal;
import java.util.List;

public final class ServiceOnlyFinishFactory {

    public static final int SOURCE_SERVICE_ONLY = 3;

    private ServiceOnlyFinishFactory() {
    }

    public static FinishRoll create(ProcessOrder order, OriginalRoll source, int rowSort) {
        return create(order, source, rowSort, sourceWeight(source));
    }

    public static FinishRoll create(ProcessOrder order, OriginalRoll source, int rowSort,
                                    BigDecimal estimateWeight) {
        FinishRoll finish = new FinishRoll();
        finish.setOrderUuid(order.getUuid());
        finish.setRowSort(rowSort);
        finish.setRollNoStatus(1);
        finish.setIsSpare(0);
        finish.setPaperName(source.getPaperName());
        finish.setGramWeight(source.getActualGramWeight() != null
                ? source.getActualGramWeight() : source.getGramWeight());
        finish.setFinishWidth(source.getActualWidth() != null
                ? source.getActualWidth() : source.getOriginalWidth());
        finish.setFinishDiameter(source.getOriginalDiameter());
        finish.setFinishCoreDiameter(source.getCoreDiameter());
        finish.setSourceType(SOURCE_SERVICE_ONLY);
        finish.setEstimateWeight(estimateWeight);
        finish.setEstimateWeightSnap(estimateWeight);
        finish.setFinishStatus(1);
        finish.setOriginalRollNos(source.getRollNo());
        finish.setRemark("仅附加工艺产出");
        return finish;
    }

    public static List<BigDecimal> pieceWeights(OriginalRoll source) {
        int count = source.getPieceNum() == null ? 1 : source.getPieceNum();
        BigDecimal total = sourceWeight(source);
        if (total == null) return java.util.Collections.nCopies(count, null);
        return IntegerWeightAllocator.allocate(total,
                java.util.Collections.nCopies(count, BigDecimal.ONE));
    }

    private static java.math.BigDecimal sourceWeight(OriginalRoll source) {
        if (source.getActualWeight() != null && source.getActualWeight().signum() > 0) {
            return IntegerWeightAllocator.roundTotal(source.getActualWeight());
        }
        if (WeightStatus.UNKNOWN.name().equalsIgnoreCase(source.getWeightStatus())) {
            return null;
        }
        if (source.getTotalWeight() != null && source.getTotalWeight().signum() > 0) {
            return IntegerWeightAllocator.roundTotal(source.getTotalWeight());
        }
        if (source.getRollWeight() == null || source.getRollWeight().signum() <= 0) {
            return null;
        }
        int pieces = source.getPieceNum() == null ? 1 : source.getPieceNum();
        return IntegerWeightAllocator.roundTotal(source.getRollWeight()
                .multiply(java.math.BigDecimal.valueOf(pieces)));
    }
}
