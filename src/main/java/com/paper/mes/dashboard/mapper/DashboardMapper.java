package com.paper.mes.dashboard.mapper;

import com.paper.mes.dashboard.dto.DashboardOverviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DashboardMapper {

    DashboardOverviewVO.DashboardMetricVO metrics(@Param("monthStart") LocalDate monthStart,
                                                  @Param("today") LocalDate today);

    List<DashboardOverviewVO.DashboardStatusVO> statusQueue();

    List<DashboardOverviewVO.DashboardTrendVO> monthlyTrend(@Param("monthStart") LocalDate monthStart,
                                                            @Param("today") LocalDate today);

    List<DashboardOverviewVO.DashboardRankVO> customerRank(@Param("monthStart") LocalDate monthStart,
                                                           @Param("today") LocalDate today);

    List<DashboardOverviewVO.DashboardRankVO> customerYearRank(@Param("yearStart") LocalDate yearStart,
                                                               @Param("today") LocalDate today);

    List<DashboardOverviewVO.DashboardRankVO> machineRank(@Param("monthStart") LocalDate monthStart,
                                                          @Param("today") LocalDate today);

    List<DashboardOverviewVO.DashboardRankVO> machineYearRank(@Param("yearStart") LocalDate yearStart,
                                                              @Param("today") LocalDate today);

    List<DashboardOverviewVO.DashboardRecentOrderVO> recentOrders();
}
