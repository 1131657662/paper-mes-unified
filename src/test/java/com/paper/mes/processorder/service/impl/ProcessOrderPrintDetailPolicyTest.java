package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.entity.FinishRoll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderPrintDetailPolicyTest {

    @Test
    void filter_removesScrappedFinishFromEveryPrintDetailPath() {
        ProcessOrderDetailVO detail = new ProcessOrderDetailVO();
        detail.setFinishRolls(List.of(finish("active", null), finish("scrapped", 4)));
        ProcessOrderDetailVO.RollProductionVO production = new ProcessOrderDetailVO.RollProductionVO();
        production.setFinishes(List.of(productionFinish("active"), productionFinish("scrapped")));
        production.setStageOutputs(List.of(stageOutput(null), stageOutput("active"), stageOutput("scrapped")));
        detail.setRollProductions(List.of(production));

        ProcessOrderPrintDetailPolicy.filter(detail);

        assertThat(detail.getFinishRolls()).extracting(FinishRoll::getUuid).containsExactly("active");
        assertThat(production.getFinishes()).extracting(ProcessOrderDetailVO.FinishProductionVO::getUuid)
                .containsExactly("active");
        assertThat(production.getStageOutputs())
                .extracting(ProcessOrderDetailVO.StageOutputVO::getFinishRollUuid)
                .containsExactly(null, "active");
    }

    private FinishRoll finish(String uuid, Integer finishStatus) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setRollNoStatus(1);
        finish.setFinishStatus(finishStatus);
        return finish;
    }

    private ProcessOrderDetailVO.FinishProductionVO productionFinish(String uuid) {
        ProcessOrderDetailVO.FinishProductionVO finish = new ProcessOrderDetailVO.FinishProductionVO();
        finish.setUuid(uuid);
        return finish;
    }

    private ProcessOrderDetailVO.StageOutputVO stageOutput(String finishUuid) {
        ProcessOrderDetailVO.StageOutputVO output = new ProcessOrderDetailVO.StageOutputVO();
        output.setFinishRollUuid(finishUuid);
        return output;
    }
}
