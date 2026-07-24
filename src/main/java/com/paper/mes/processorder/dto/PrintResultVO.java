package com.paper.mes.processorder.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打印结果。返回本次打印的元信息与预生成成品卷号清单，供前端渲染纸质单据。
 */
@Data
public class PrintResultVO {

    private String orderUuid;
    private String orderNo;
    /** 本次打印后的累计打印次数。 */
    private Integer printCount;
    /** 0已下发但未确认物理打印，1已确认打印。 */
    private Integer printStatus;
    /** 是否补打（print_count>1）。 */
    private Boolean reprint;
    private LocalDateTime printTime;
    private Integer orderStatus;
    /** 预生成的正式成品卷号清单（is_spare=0 且未作废）。 */
    private List<String> finishRollNos;
    /** 备用卷号清单（is_spare=1 且未作废）。 */
    private List<String> spareRollNos;
}
