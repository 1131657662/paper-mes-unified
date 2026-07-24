package com.paper.mes.processorder.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Result of an atomic finish configuration batch save. */
@Data
public class FinishConfigBatchSaveVO {

    private String orderUuid;
    private List<FinishConfigSaveVO> results = new ArrayList<>();
}
