package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class ProcessOrderPrintDetailPolicy {

    private ProcessOrderPrintDetailPolicy() {
    }

    static ProcessOrderDetailVO filter(ProcessOrderDetailVO detail) {
        List<FinishRoll> printable = ProcessOrderPrintFinishPolicy.printable(detail.getFinishRolls());
        Set<String> printableIds = printable.stream().map(FinishRoll::getUuid).collect(Collectors.toSet());
        detail.setFinishRolls(printable);
        for (ProcessOrderDetailVO.RollProductionVO production : safe(detail.getRollProductions())) {
            production.setFinishes(safe(production.getFinishes()).stream()
                    .filter(finish -> printableIds.contains(finish.getUuid()))
                    .toList());
            production.setStageOutputs(safe(production.getStageOutputs()).stream()
                    .filter(output -> output.getFinishRollUuid() == null
                            || printableIds.contains(output.getFinishRollUuid()))
                    .toList());
        }
        return detail;
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
