package com.paper.mes.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页结果。
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long current;
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
