package com.paper.mes.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardOverviewVO {

    private DashboardMetricVO metrics;
    private List<DashboardStatusVO> statusQueue;
    private List<DashboardTrendVO> monthlyTrend;
    private List<DashboardTrendVO> yearlyTrend;
    private List<DashboardRankVO> customerRank;
    private List<DashboardRankVO> customerYearRank;
    private List<DashboardRankVO> machineRank;
    private List<DashboardTodoVO> todos;
    private List<DashboardRecentOrderVO> recentOrders;

    @Data
    public static class DashboardMetricVO {
        private Integer monthOrderCount;
        private BigDecimal monthOriginalWeight;
        private BigDecimal monthFinishWeight;
        private BigDecimal monthAmount;
        private BigDecimal monthLossWeight;
        private BigDecimal monthLossRatio;
        private Integer inStockFinishCount;
        private BigDecimal inStockFinishWeight;
        private Integer receivableCount;
        private BigDecimal receivableAmount;
    }

    @Data
    public static class DashboardStatusVO {
        private Integer status;
        private String statusName;
        private Integer orderCount;
        private BigDecimal originalWeight;
    }

    @Data
    public static class DashboardTrendVO {
        private String month;
        private Integer orderCount;
        private BigDecimal originalWeight;
        private BigDecimal finishWeight;
        private BigDecimal amount;
    }

    @Data
    public static class DashboardRankVO {
        private String id;
        private String name;
        private Integer count;
        private BigDecimal weight;
        private BigDecimal amount;
    }

    @Data
    public static class DashboardTodoVO {
        private String key;
        private String title;
        private String description;
        private Integer count;
        private BigDecimal amount;
        private String level;
        private String targetPath;
    }

    @Data
    public static class DashboardRecentOrderVO {
        private String uuid;
        private String orderNo;
        private String customerName;
        private String orderDate;
        private Integer priority;
        private Integer orderStatus;
        private Integer printStatus;
        private BigDecimal originalWeight;
        private BigDecimal finishWeight;
    }
}
