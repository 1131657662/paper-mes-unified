package com.paper.mes.processorder.dto;

import lombok.Data;

/**
 * 打印加工单入参。
 * 首次打印 reason 可空；补打（已打印过）时 reason 必填，作为补打原因留痕。
 */
@Data
public class PrintDTO {

    /** 补打原因（首打可空，补打必填）。 */
    private String reason;
}
