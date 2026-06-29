package com.paper.mes.processorder.dto;

import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import lombok.Data;

import java.util.List;

@Data
public class DraftOrderVO {

    private ProcessOrder order;
    private Integer currentStep;
    private List<OriginalRoll> rolls;
    private List<ProcessConfigDraftVO> configs;
}
