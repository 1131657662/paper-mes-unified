package com.paper.mes.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 客户档案 sys_customer。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_customer")
public class Customer extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;

    private String customerCode;
    private String customerName;
    private String contact;
    private String phone;

    /** 1次结 2月结 */
    private Integer settleType;
    /** 月结对账日 */
    private Integer settleDay;
    /** 锯纸单价 元/刀 */
    private BigDecimal sawPrice;
    /** 复卷单价 元/吨 */
    private BigDecimal rewindPrice;

    /** 1开票 2不开票 */
    private Integer defaultInvoice;
    /** 1含税 2不含税 */
    private Integer priceIncludeTax;
    /** 税率% */
    private BigDecimal taxRate;
    private String taxNo;
    private String invoiceAddress;
    private String bankAccount;
    private String deliveryAddress;
    private Integer customerLevel;
    private String exportTemplate;
    private String remark;
}
