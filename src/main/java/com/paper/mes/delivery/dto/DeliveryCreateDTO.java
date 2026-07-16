package com.paper.mes.delivery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 出库单创建入参。勾选客户已入库成品合并出库；forceRelease 保留给前端风险确认留痕。
 */
@Data
public class DeliveryCreateDTO {

    @NotBlank(message = "客户不能为空")
    private String customerUuid;

    @NotNull(message = "出库日期不能为空")
    private LocalDate deliveryDate;

    @Size(max = 50, message = "提货人不能超过50个字符")
    private String pickerName;
    @Size(max = 50, message = "车牌号不能超过50个字符")
    private String carNo;
    @Size(max = 50, message = "柜号不能超过50个字符")
    private String containerNo;
    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    /** 现结客户存在未结清款项时，前端确认后传 true；后端默认按配置警告放行或强制拦截。 */
    private boolean forceRelease;

    @NotEmpty(message = "出库成品不能为空")
    @Size(max = 500, message = "单次出库成品不能超过500条")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotBlank(message = "成品uuid不能为空")
        private String finishUuid;
        /** 本件出库重量，留空则取成品实际重量。 */
        @Positive(message = "出库重量必须大于0")
        private BigDecimal outWeight;
        @Size(max = 255, message = "单卷备注不能超过255个字符")
        private String remark;
    }
}
