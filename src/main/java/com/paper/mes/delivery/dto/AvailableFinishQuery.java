package com.paper.mes.delivery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AvailableFinishQuery {

    @NotBlank(message = "客户不能为空")
    @Size(max = 64, message = "客户标识不能超过64个字符")
    private String customerUuid;

    @NotBlank(message = "仓库不能为空")
    @Size(max = 64, message = "仓库标识不能超过64个字符")
    private String warehouseUuid;

    @Size(max = 100, message = "搜索关键字不能超过100个字符")
    private String keyword;

    /** product 普通成品及直发，remain 余料，all 仅用于恢复已选卷。 */
    @Pattern(regexp = "all|product|remain", message = "库存类型无效")
    private String scope = "product";

    /** all 全部，missingSource 缧少母卷关联，missingIdentity 母卷缺少卷号和编号。 */
    @Pattern(regexp = "all|missingSource|missingIdentity", message = "来源筛选无效")
    private String sourceIssue = "all";

    @Size(max = 100, message = "单次最多查询100卷")
    private List<@NotBlank(message = "成品卷标识不能为空")
            @Size(max = 64, message = "成品卷标识不能超过64个字符") String> finishUuids;

    @Min(value = 1, message = "页码不能小于1")
    private long current = 1;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private long size = 20;
}
