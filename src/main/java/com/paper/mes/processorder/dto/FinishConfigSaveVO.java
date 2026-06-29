package com.paper.mes.processorder.dto;

import lombok.Data;

import java.util.List;

/**
 * 单卷成品配置保存结果。
 */
@Data
public class FinishConfigSaveVO {

    private String orderUuid;
    private String originalUuid;
    private List<String> finishRollNos;
    private List<String> spareRollNos;
}
