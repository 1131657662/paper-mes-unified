package com.paper.mes.exporttask.dto;

import com.paper.mes.delivery.dto.DeliverySortSpec;

import java.util.List;

public record DeliveryOrderExportTaskPayload(
        int schemaVersion,
        int customerRevisionNo,
        String documentFingerprint,
        List<DeliverySortSpec> sortChain,
        List<DeliverySortSpec> customerSortChain,
        List<DeliverySortSpec> traceSortChain,
        String documentView) {
    public static final int LEGACY_SCHEMA_VERSION = 1;
    public static final int OLDEST_SUPPORTED_SCHEMA_VERSION = 2;
    public static final int PREVIOUS_SCHEMA_VERSION = 3;
    public static final int CURRENT_SCHEMA_VERSION = 4;

    public DeliveryOrderExportTaskPayload(int schemaVersion, int customerRevisionNo, String documentFingerprint) {
        this(schemaVersion, customerRevisionNo, documentFingerprint, List.of(), List.of(), List.of(), "physical");
    }

    public DeliveryOrderExportTaskPayload(int schemaVersion, int customerRevisionNo, String documentFingerprint,
                                          List<DeliverySortSpec> sortChain) {
        this(schemaVersion, customerRevisionNo, documentFingerprint, sortChain, List.of(), List.of(), "physical");
    }
}
