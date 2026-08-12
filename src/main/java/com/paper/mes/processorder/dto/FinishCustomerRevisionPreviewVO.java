package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FinishCustomerRevisionPreviewVO {

    private String orderUuid;
    private String orderNo;
    private Integer orderVersion;
    private String sourceStage;
    private Integer nextRevisionNo;
    private Integer itemCount;
    private Integer validItemCount;
    private BigDecimal physicalTotalWeight;
    private BigDecimal customerTotalWeight;
    private BigDecimal differenceWeight;
    private boolean hasErrors;
    /** Processing orders require reissue only when production-print fields changed. */
    private boolean reissueRequired;
    /** Number of unconfirmed delivery documents that will continue with the new commercial display. */
    private Integer pendingDeliveryCount;
    private List<FinishCustomerSpecVO> items;
}
