package com.paper.mes.processorder.dto;

import lombok.Data;

import java.util.List;

/** 加工单打印版本及其冻结详情。 */
@Data
public class ProcessOrderPrintViewVO {

    private PrintViewVersion version;
    private List<PrintViewVersion> availableVersions;
    /** LIVE_PREVIEW、SNAPSHOT 或 LEGACY_FALLBACK。 */
    private String source;
    private String schemaVersion;
    private String snapshotTime;
    private String snapshotUser;
    private String warning;
    private ProcessOrderDetailVO detail;
}
