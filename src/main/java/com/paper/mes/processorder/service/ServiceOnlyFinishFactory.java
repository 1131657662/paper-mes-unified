package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;

public final class ServiceOnlyFinishFactory {

    public static final int SOURCE_SERVICE_ONLY = 3;

    private ServiceOnlyFinishFactory() {
    }

    public static FinishRoll create(ProcessOrder order, OriginalRoll source, int rowSort) {
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
        finish.setEstimateWeight(source.getRollWeight());
        finish.setEstimateWeightSnap(source.getRollWeight());
        finish.setFinishStatus(1);
        finish.setOriginalRollNos(source.getRollNo());
        finish.setRemark("仅附加工艺产出");
        return finish;
    }
}
