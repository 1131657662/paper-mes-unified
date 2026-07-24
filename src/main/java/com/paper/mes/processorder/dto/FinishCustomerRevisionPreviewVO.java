package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FinishCustomerRevisionPreviewVO {

    private String orderUuid;
    private String orderNo;
    private Integer orderVersion;
    private Integer nextRevisionNo;
    private Integer itemCount;
    private Integer validItemCount;
    private BigDecimal physicalTotalWeight;
    private BigDecimal customerTotalWeight;
    private BigDecimal differenceWeight;
    private boolean hasErrors;
    private List<FinishCustomerSpecVO> items;
}
