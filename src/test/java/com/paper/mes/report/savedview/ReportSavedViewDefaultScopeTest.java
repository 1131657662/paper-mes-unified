package com.paper.mes.report.savedview;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.savedview.dto.ReportSavedViewSaveDTO;
import com.paper.mes.report.savedview.entity.ReportSavedView;
import com.paper.mes.report.savedview.mapper.ReportSavedViewMapper;
import com.paper.mes.report.savedview.service.ReportSavedViewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportSavedViewDefaultScopeTest {
    private final ReportSavedViewMapper mapper = mock(ReportSavedViewMapper.class);
    private final ReportSavedViewService service = new ReportSavedViewService(mapper,
            new ObjectMapper().findAndRegisterModules(), mock(OperationLogService.class));

    @BeforeAll
    static void initMybatisMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ReportSavedView.class);
    }

    @AfterEach
    void clearAuth() { AuthContextHolder.clear(); }

    @Test
    void create_defaultView_clearsDefaultOnlyForSameReportPath() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-a").roleCode("viewer").build());
        when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(mapper.insert(any(ReportSavedView.class))).thenReturn(1);

        service.create(defaultExplorerView());

        ArgumentCaptor<LambdaUpdateWrapper<ReportSavedView>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(mapper).update(isNull(), captor.capture());
        assertThat(captor.getValue().getCustomSqlSegment()).contains("report_path");
        assertThat(captor.getValue().getParamNameValuePairs().values()).contains("/reports/explorer");
    }

    @Test
    void update_withStaleVersion_rejectsConcurrentOverwrite() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-a").roleCode("viewer").build());
        ReportSavedView stored = storedView();
        when(mapper.selectOne(any())).thenReturn(stored);
        when(mapper.updateById(any(ReportSavedView.class))).thenReturn(0);
        ReportSavedViewSaveDTO staleUpdate = defaultExplorerView();
        staleUpdate.setIsDefault(0);
        staleUpdate.setVersion(1);

        assertThatThrownBy(() -> service.update(stored.getUuid(), staleUpdate))
                .isInstanceOf(BusinessException.class);
    }

    private ReportSavedViewSaveDTO defaultExplorerView() {
        ReportSavedViewSaveDTO dto = new ReportSavedViewSaveDTO();
        dto.setViewName("客户损耗视图");
        dto.setReportPath("/reports/explorer");
        dto.setReportQuery(new ReportQuery());
        dto.setDimensionCode("customer");
        dto.setMetricCodes(List.of("loss_ratio_pct"));
        dto.setIsDefault(1);
        return dto;
    }

    private ReportSavedView storedView() {
        ReportSavedView view = new ReportSavedView();
        view.setUuid("view-1");
        view.setOwnerUuid("user-a");
        view.setReportPath("/reports/explorer");
        view.setVersion(2);
        return view;
    }
}
