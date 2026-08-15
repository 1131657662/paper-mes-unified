package com.paper.mes.processorder.dto;

import com.paper.mes.processorder.model.ProcessRollDispositionAction;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** Result of a source-roll disposition command. */
@Data
public class ProcessRollDispositionVO {
    private String sourceOrderUuid;
    private String sourceOrderNo;
    private String sourceRollUuid;
    private ProcessRollDispositionAction action;
    private String targetOrderUuid;
    private String targetOrderNo;
    private String targetRollUuid;
    private String targetFinishUuid;
    private List<String> targetFinishUuids;
    private LocalDateTime operatedAt;
}
