package com.paper.mes.paper.dto;

import lombok.Data;

/**
 * 纸张列表查询入参。
 */
@Data
public class PaperQuery {

    /** 关键字：命中纸张编码或品名 */
    private String keyword;

    private long current = 1;
    private long size = 10;
}
