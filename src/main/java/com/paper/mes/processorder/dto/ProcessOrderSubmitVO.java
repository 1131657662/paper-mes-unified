package com.paper.mes.processorder.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 草稿提交结果。
 */
@Data
public class ProcessOrderSubmitVO {

    private String orderUuid;
    private String orderNo;
    private Integer orderStatus;
    private List<String> finishRollNos = new ArrayList<>();
    private List<String> spareRollNos = new ArrayList<>();
}
