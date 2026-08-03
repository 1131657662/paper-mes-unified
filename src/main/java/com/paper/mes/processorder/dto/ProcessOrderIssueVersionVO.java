package com.paper.mes.processorder.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProcessOrderIssueVersionVO {

    private String uuid;
    private String orderUuid;
    private Integer versionNo;
    private Integer previousVersionNo;
    private String status;
    private String changeReason;
    private String operatorName;
    private LocalDateTime changeTime;
    private LocalDateTime issueTime;
    private String issueOperatorName;
    private boolean hasSnapshotBefore;
    private boolean hasSnapshotAfter;
}
