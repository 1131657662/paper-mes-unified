package com.paper.mes.oplog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.oplog.dto.OperationLogQuery;
import com.paper.mes.oplog.entity.OperationLog;
import com.paper.mes.oplog.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 操作日志查询接口（管理员功能）
 */
@RestController
@RequestMapping("/api/operation-logs")
@RequirePermission(Permissions.SYSTEM_AUDIT)
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogMapper operationLogMapper;

    /**
     * 分页查询操作日志
     */
    @GetMapping
    public R<PageResult<OperationLog>> page(@Valid OperationLogQuery query) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        // 业务类型筛选
        if (StringUtils.hasText(query.getBizType())) {
            wrapper.eq(OperationLog::getBizType, query.getBizType());
        }

        // 业务单号模糊查询
        if (StringUtils.hasText(query.getBizNo())) {
            wrapper.like(OperationLog::getBizNo, query.getBizNo());
        }

        // 动作类型筛选
        if (StringUtils.hasText(query.getActionType())) {
            wrapper.eq(OperationLog::getActionType, query.getActionType());
        }

        // 操作人筛选
        if (StringUtils.hasText(query.getOperator())) {
            wrapper.like(OperationLog::getOperator, query.getOperator());
        }

        if (StringUtils.hasText(query.getFieldName())) {
            wrapper.like(OperationLog::getFieldName, query.getFieldName());
        }

        if (StringUtils.hasText(query.getRemark())) {
            wrapper.like(OperationLog::getRemark, query.getRemark());
        }

        // 操作日期范围
        if (query.getDateFrom() != null) {
            wrapper.ge(OperationLog::getOperateTime,
                    LocalDateTime.of(query.getDateFrom(), LocalTime.MIN));
        }
        if (query.getDateTo() != null) {
            wrapper.le(OperationLog::getOperateTime,
                    LocalDateTime.of(query.getDateTo(), LocalTime.MAX));
        }

        // 按操作时间降序
        wrapper.orderByDesc(OperationLog::getOperateTime);

        Page<OperationLog> page = operationLogMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );

        return R.success(PageResult.of(page));
    }
}
