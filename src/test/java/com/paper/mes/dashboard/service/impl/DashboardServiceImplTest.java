package com.paper.mes.dashboard.service.impl;

import com.paper.mes.dashboard.dto.DashboardOverviewVO;
import com.paper.mes.dashboard.mapper.DashboardMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DashboardMapper dashboardMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void overviewUsesRollingTwelveMonthWindowForYearlyTrend() {
        when(dashboardMapper.metrics(any(), any())).thenReturn(new DashboardOverviewVO.DashboardMetricVO());
        when(dashboardMapper.statusQueue()).thenReturn(List.of());
        when(dashboardMapper.monthlyTrend(any(), any())).thenReturn(List.of());

        dashboardService.overview();

        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(dashboardMapper, times(2)).monthlyTrend(startCaptor.capture(), endCaptor.capture());
        LocalDate trendEnd = endCaptor.getAllValues().get(1);
        assertEquals(trendEnd.minusMonths(11).withDayOfMonth(1), startCaptor.getAllValues().get(1));
    }
}
