package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessRouteModePolicyTest {

    private final ProcessRouteModePolicy policy = new ProcessRouteModePolicy();

    @Test
    void requireCompatible_multiplePhysicalPieces_rejectsAggregatedRoute() {
        OriginalRoll roll = new OriginalRoll();
        roll.setProcessMode(ProcessModePolicy.STANDARD);
        roll.setPieceNum(2);

        assertThatThrownBy(() -> policy.requireCompatible(roll, new ProcessRoutePreviewDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("拆分为单件母卷");
    }
}
