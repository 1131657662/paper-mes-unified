package com.paper.mes.dashboard.service.impl;

import com.paper.mes.dashboard.dto.DashboardOverviewVO;
import com.paper.mes.dashboard.mapper.DashboardMapper;
import com.paper.mes.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;

    @Override
    public DashboardOverviewVO overview() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate rollingYearStart = today.minusMonths(11).withDayOfMonth(1);
        DashboardOverviewVO vo = new DashboardOverviewVO();
        DashboardOverviewVO.DashboardMetricVO metrics = dashboardMapper.metrics(monthStart, today);
        vo.setMetrics(metrics);
        vo.setStatusQueue(dashboardMapper.statusQueue());
        vo.setMonthlyTrend(dashboardMapper.monthlyTrend(today.minusMonths(5).withDayOfMonth(1), today));
        vo.setYearlyTrend(dashboardMapper.monthlyTrend(rollingYearStart, today));
        vo.setCustomerRank(dashboardMapper.customerRank(monthStart, today));
        vo.setCustomerYearRank(dashboardMapper.customerYearRank(yearStart, today));
        vo.setMachineRank(dashboardMapper.machineRank(monthStart, today));
        vo.setMachineYearRank(dashboardMapper.machineYearRank(yearStart, today));
        vo.setRecentOrders(dashboardMapper.recentOrders());
        vo.setTodos(buildTodos(metrics, vo.getStatusQueue()));
        return vo;
    }

    private List<DashboardOverviewVO.DashboardTodoVO> buildTodos(DashboardOverviewVO.DashboardMetricVO metrics,
                                                                 List<DashboardOverviewVO.DashboardStatusVO> queue) {
        List<DashboardOverviewVO.DashboardTodoVO> todos = new ArrayList<>();
        todos.add(todo("issue", "待下发加工单", "确认打印快照后下发车间", countStatus(queue, 1), null, "info", "/process-orders"));
        todos.add(todo("record", "待回录加工单", "影响入库、出库和结算闭环", countStatus(queue, 3), null, "warning", "/process-orders"));
        todos.add(todo("stock", "成品待出库", "已入库但尚未出库的正式成品", metrics.getInStockFinishCount(), metrics.getInStockFinishWeight(), "success", "/delivery-orders"));
        todos.add(todo("receive", "已结算未收", "只统计已生成结算单但尚未收齐的应收款", metrics.getReceivableCount(), metrics.getReceivableAmount(), "danger", "/settle-orders"));
        todos.add(todo("pending-settle", "待生成结算", "已完成但尚未生成结算单的加工金额", null, metrics.getPendingSettleAmount(), "warning", "/settle-orders"));
        return todos;
    }

    private DashboardOverviewVO.DashboardTodoVO todo(String key, String title, String description,
                                                    Integer count, BigDecimal amount, String level, String targetPath) {
        DashboardOverviewVO.DashboardTodoVO item = new DashboardOverviewVO.DashboardTodoVO();
        item.setKey(key);
        item.setTitle(title);
        item.setDescription(description);
        item.setCount(count == null ? 0 : count);
        item.setAmount(amount == null ? BigDecimal.ZERO : amount);
        item.setLevel(level);
        item.setTargetPath(targetPath);
        return item;
    }

    private Integer countStatus(List<DashboardOverviewVO.DashboardStatusVO> queue, int status) {
        if (queue == null) return 0;
        return queue.stream()
                .filter(item -> item.getStatus() != null && item.getStatus().intValue() == status)
                .map(DashboardOverviewVO.DashboardStatusVO::getOrderCount)
                .findFirst()
                .orElse(0);
    }
}
