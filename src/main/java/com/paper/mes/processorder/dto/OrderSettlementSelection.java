package com.paper.mes.processorder.dto;

/** Common settlement input shared by draft and direct order creation. */
public interface OrderSettlementSelection {

    OrderSettlementMode getSettleMode();

    Integer getCustomerVersion();

    Integer getSettleType();

    Integer getSettleDay();

    String getSettleOverrideReason();
}
