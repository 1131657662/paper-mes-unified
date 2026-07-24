package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.entity.FinishRoll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderPrintFinishPolicyTest {

    @Test
    void printable_excludesVoidedNumbersAndScrappedFinishes() {
        FinishRoll active = finish("active", 1, null);
        FinishRoll legacy = finish("legacy", null, null);
        FinishRoll voided = finish("voided", 3, null);
        FinishRoll scrapped = finish("scrapped", 1, 4);

        List<FinishRoll> result = ProcessOrderPrintFinishPolicy.printable(
                List.of(active, legacy, voided, scrapped));

        assertThat(result).extracting(FinishRoll::getUuid).containsExactly("active", "legacy");
    }

    private FinishRoll finish(String uuid, Integer rollNoStatus, Integer finishStatus) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setRollNoStatus(rollNoStatus);
        finish.setFinishStatus(finishStatus);
        return finish;
    }
}
