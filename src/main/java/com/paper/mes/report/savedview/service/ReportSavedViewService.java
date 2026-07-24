package com.paper.mes.report.savedview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ResultCode;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.report.savedview.dto.ReportSavedViewSaveDTO;
import com.paper.mes.report.savedview.dto.ReportSavedViewVO;
import com.paper.mes.report.savedview.entity.ReportSavedView;
import com.paper.mes.report.savedview.mapper.ReportSavedViewMapper;
import com.paper.mes.report.dto.ReportQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportSavedViewService {
    private static final TypeReference<List<String>> METRICS = new TypeReference<>() { };
    private final ReportSavedViewMapper mapper;
    private final ObjectMapper objectMapper;
    private final OperationLogService operationLogService;

    public List<ReportSavedViewVO> listMine() {
        String ownerUuid = currentUser().getUuid();
        return mapper.selectList(new LambdaQueryWrapper<ReportSavedView>()
                        .eq(ReportSavedView::getOwnerUuid, ownerUuid)
                        .orderByDesc(ReportSavedView::getIsDefault)
                        .orderByAsc(ReportSavedView::getViewName))
                .stream().map(this::toVO).toList();
    }

    @Transactional
    public String create(ReportSavedViewSaveDTO dto) {
        CurrentUser user = currentUser();
        ReportSavedView view = new ReportSavedView();
        apply(view, dto, user.getUuid());
        try {
            clearDefaultIfNeeded(user.getUuid(), view);
            ConcurrencyGuard.requireRowUpdated(mapper.insert(view));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("视图名称或默认视图已存在");
        }
        record(view, "新增报表保存视图");
        return view.getUuid();
    }

    @Transactional
    public void update(String uuid, ReportSavedViewSaveDTO dto) {
        CurrentUser user = currentUser();
        ReportSavedView view = owned(uuid, user.getUuid());
        if (dto.getVersion() == null) throw new BusinessException("更新视图必须携带数据版本");
        apply(view, dto, user.getUuid());
        view.setVersion(dto.getVersion());
        try {
            clearDefaultIfNeeded(user.getUuid(), view);
            ConcurrencyGuard.requireRowUpdated(mapper.updateById(view));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("视图名称或默认视图已存在");
        }
        record(view, "修改报表保存视图");
    }

    @Transactional
    public void delete(String uuid, int version) {
        CurrentUser user = currentUser();
        owned(uuid, user.getUuid());
        int rows = mapper.update(null, new LambdaUpdateWrapper<ReportSavedView>()
                .eq(ReportSavedView::getUuid, uuid).eq(ReportSavedView::getOwnerUuid, user.getUuid())
                .eq(ReportSavedView::getVersion, version).set(ReportSavedView::getIsDeleted, 1)
                .setSql("version = version + 1"));
        ConcurrencyGuard.requireRowUpdated(rows);
        operationLogService.record("报表保存视图", uuid, null, "删除报表保存视图", null, "删除报表保存视图");
    }

    private void apply(ReportSavedView target, ReportSavedViewSaveDTO dto, String ownerUuid) {
        target.setOwnerUuid(ownerUuid);
        target.setViewName(dto.getViewName().trim());
        target.setReportPath(dto.getReportPath());
        target.setQueryJson(write(dto.getReportQuery()));
        target.setDimensionCode(normalize(dto.getDimensionCode()));
        target.setMetricCodesJson(write(dto.getMetricCodes().stream().distinct().toList()));
        target.setIsDefault(dto.getIsDefault());
    }

    private void clearDefaultIfNeeded(String ownerUuid, ReportSavedView target) {
        if (!Integer.valueOf(1).equals(target.getIsDefault())) return;
        mapper.update(null, new LambdaUpdateWrapper<ReportSavedView>()
                .eq(ReportSavedView::getOwnerUuid, ownerUuid).eq(ReportSavedView::getIsDefault, 1)
                .eq(ReportSavedView::getReportPath, target.getReportPath())
                .set(ReportSavedView::getIsDefault, 0));
    }

    private ReportSavedView owned(String uuid, String ownerUuid) {
        ReportSavedView view = mapper.selectOne(new LambdaQueryWrapper<ReportSavedView>()
                .eq(ReportSavedView::getUuid, uuid).eq(ReportSavedView::getOwnerUuid, ownerUuid));
        if (view == null) throw new BusinessException(ResultCode.NOT_FOUND, "REPORT_SAVED_VIEW_NOT_FOUND", "报表视图不存在");
        return view;
    }

    private ReportSavedViewVO toVO(ReportSavedView view) {
        return new ReportSavedViewVO(view.getUuid(), view.getViewName(), view.getReportPath(),
                readQuery(view.getQueryJson()), view.getDimensionCode(), readMetrics(view.getMetricCodesJson()),
                view.getIsDefault(), view.getCreateTime(), view.getUpdateTime(), view.getVersion());
    }

    private ReportQuery readQuery(String value) {
        try { return objectMapper.readValue(value, com.paper.mes.report.dto.ReportQuery.class); }
        catch (JsonProcessingException exception) { throw new BusinessException("报表视图查询条件损坏"); }
    }

    private List<String> readMetrics(String value) {
        try { return objectMapper.readValue(value, METRICS); }
        catch (JsonProcessingException exception) { throw new BusinessException("报表视图指标配置损坏"); }
    }

    private String write(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new BusinessException("报表视图无法保存"); }
    }

    private CurrentUser currentUser() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "AUTH_REQUIRED", "登录状态已失效");
        return user;
    }

    private String normalize(String value) { return value == null || value.isBlank() ? null : value; }

    private void record(ReportSavedView view, String action) {
        operationLogService.record("报表保存视图", view.getUuid(), view.getViewName(), action, null, action);
    }
}
