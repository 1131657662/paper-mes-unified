package com.paper.mes.processorder.dto;

/** Shared approval fields for back-record writes and final order closure. */
public interface BackRecordClosureApproval {

    String getReleaseAdminUsername();

    String getReleaseAdminPassword();

    String getReleaseReason();

    String getVarianceReason();
}
