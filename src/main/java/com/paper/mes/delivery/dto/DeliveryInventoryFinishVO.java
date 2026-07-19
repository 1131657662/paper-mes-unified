package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DeliveryInventoryFinishVO {

    private String customerUuid;
    private String customerName;
    private String finishUuid;
    private String finishRollNo;
    private String orderUuid;
    private String orderNo;
    private LocalDate orderDate;
    private String warehouseUuid;
    private String warehouseName;
    private String warehouseLocation;
    private String paperName;
    private Integer gramWeight;
    private Integer finishWidth;
    private Integer finishDiameter;
    private Integer finishCoreDiameter;
    private BigDecimal remainingWeight;
    private BigDecimal actualWeight;
    private LocalDateTime stockInTime;
    private Long stockAgeDays;
    private Integer isRemain;
    private Integer sourceType;
    /** 1 可出库，2 待出库占用。 */
    private Integer stockState;
    private BigDecimal plannedOutWeight;
    private String deliveryUuid;
    private String deliveryNo;
}
