package com.paper.mes.delivery.dto;

import com.paper.mes.common.PageResult;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AvailableFinishPageVO {
    private List<AvailableFinishVO> records;
    private long total;
    private long current;
    private long size;
    private ScopeCounts scopeCounts;
    private ExcludedCounts excluded;
    private LocalDateTime asOf;

    public static AvailableFinishPageVO of(PageResult<AvailableFinishVO> page,
                                           AvailableFinishStatsVO stats,
                                           LocalDateTime asOf) {
        AvailableFinishPageVO result = new AvailableFinishPageVO();
        result.records = page.getRecords();
        result.total = page.getTotal();
        result.current = page.getCurrent();
        result.size = page.getSize();
        result.scopeCounts = new ScopeCounts(stats.getProductCount(), stats.getRemainCount());
        result.excluded = new ExcludedCounts(
                stats.getUnassignedWarehouseCount(), stats.getOtherWarehouseCount(),
                stats.getLockedCount(), stats.getInvalidWeightCount());
        result.asOf = asOf;
        return result;
    }

    public record ScopeCounts(long product, long remain) {
    }

    public record ExcludedCounts(long unassignedWarehouseCount, long otherWarehouseCount,
                                 long lockedCount, long invalidWeightCount) {
    }
}
