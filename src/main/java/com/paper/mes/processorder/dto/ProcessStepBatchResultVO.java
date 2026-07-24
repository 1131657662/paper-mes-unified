package com.paper.mes.processorder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProcessStepBatchResultVO {
    private int selectedCount;
    private int createdCount;
    private int updatedCount;
}
