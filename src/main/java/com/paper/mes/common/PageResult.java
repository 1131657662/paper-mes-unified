package com.paper.mes.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分页结果。
 */
@Data
public class PageResult<T> {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> records;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private long total;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private long current;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private long size;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.records = page.getRecords();
        result.total = page.getTotal();
        result.current = page.getCurrent();
        result.size = page.getSize();
        return result;
    }
}
