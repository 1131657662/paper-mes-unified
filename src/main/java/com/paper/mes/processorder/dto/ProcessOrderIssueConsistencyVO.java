package com.paper.mes.processorder.dto;

import lombok.Data;

import java.util.List;

/** Read-only comparison between the active issued snapshot and mutable business data. */
@Data
public class ProcessOrderIssueConsistencyVO {

    private String status;
    private Integer currentIssueVersion;
    private List<String> changedGroups;
    private String blockingReason;
    private Integer pendingDeliveryCount;
}
