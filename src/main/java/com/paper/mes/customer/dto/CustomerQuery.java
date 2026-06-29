package com.paper.mes.customer.dto;

import lombok.Data;

/**
 * 客户列表查询入参。
 */
@Data
public class CustomerQuery {

    /** 关键字：命中客户编码或客户名称 */
    private String keyword;

    private long current = 1;
    private long size = 10;
}
