package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.PageRequestBounds;
import com.paper.mes.common.PageResult;
import com.paper.mes.settle.dto.SettleDiscountApprovalQuery;
import com.paper.mes.settle.dto.SettleDiscountApprovalVO;
import com.paper.mes.settle.entity.SettleDiscountApproval;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleDiscountApprovalMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettleDiscountApprovalQueryService {
    private final SettleDiscountApprovalMapper approvalMapper;
    private final SettleOrderMapper settleOrderMapper;
    private final PermissionChecker permissionChecker;

    public List<SettleDiscountApprovalVO> list(String settleUuid) {
        permissionChecker.require(Permissions.SETTLE_RECEIVE, Permissions.SETTLE_DISCOUNT_APPROVE,
                Permissions.SETTLE_DISCOUNT_ADMIN_APPROVE);
        SettleOrder settle = settleOrderMapper.selectById(settleUuid);
        return approvalMapper.selectList(baseSettleQuery(settleUuid)).stream()
                .map(item -> SettleDiscountApprovalVO.from(item, settle)).toList();
    }

    public Optional<SettleDiscountApprovalVO> latest(String settleUuid) {
        permissionChecker.require(Permissions.SETTLE_RECEIVE, Permissions.SETTLE_DISCOUNT);
        SettleDiscountApproval item = approvalMapper.selectOne(baseSettleQuery(settleUuid).last("LIMIT 1"));
        return Optional.ofNullable(item)
                .map(value -> SettleDiscountApprovalVO.from(value, settleOrderMapper.selectById(settleUuid)));
    }

    public SettleDiscountApprovalVO detail(String uuid) {
        permissionChecker.require(Permissions.SETTLE_DISCOUNT, Permissions.SETTLE_DISCOUNT_APPROVE,
                Permissions.SETTLE_DISCOUNT_ADMIN_APPROVE);
        SettleDiscountApproval item = approvalMapper.selectById(uuid);
        if (item == null) return null;
        return SettleDiscountApprovalVO.from(item, settleOrderMapper.selectById(item.getSettleUuid()));
    }

    public PageResult<SettleDiscountApprovalVO> page(SettleDiscountApprovalQuery query) {
        permissionChecker.require(Permissions.SETTLE_DISCOUNT, Permissions.SETTLE_DISCOUNT_APPROVE,
                Permissions.SETTLE_DISCOUNT_ADMIN_APPROVE);
        LambdaQueryWrapper<SettleDiscountApproval> wrapper = new LambdaQueryWrapper<>();
        applyScope(wrapper, query.getScope());
        applyApproverLevel(wrapper, query.getScope());
        if (StringUtils.hasText(query.getRequiredLevel())) {
            wrapper.eq(SettleDiscountApproval::getRequiredLevel, query.getRequiredLevel());
        }
        applyKeyword(wrapper, query.getKeyword());
        wrapper.orderByDesc(SettleDiscountApproval::getRequestTime)
                .orderByDesc(SettleDiscountApproval::getUuid);
        Page<SettleDiscountApproval> page = approvalMapper.selectPage(
                PageRequestBounds.of(query.getCurrent(), query.getSize()), wrapper);
        return toResult(page);
    }

    private void applyScope(LambdaQueryWrapper<SettleDiscountApproval> wrapper, String scope) {
        if ("mine".equals(scope)) {
            wrapper.eq(SettleDiscountApproval::getRequestBy, currentUser().getUuid());
        } else if ("processed".equals(scope)) {
            wrapper.ne(SettleDiscountApproval::getApprovalStatus, SettleDiscountApprovalStatus.PENDING);
        } else {
            wrapper.eq(SettleDiscountApproval::getApprovalStatus, SettleDiscountApprovalStatus.PENDING)
                    .ne(SettleDiscountApproval::getRequestBy, currentUser().getUuid());
        }
    }

    private void applyApproverLevel(LambdaQueryWrapper<SettleDiscountApproval> wrapper, String scope) {
        if ("mine".equals(scope) || permissionChecker.has(Permissions.SETTLE_DISCOUNT_ADMIN_APPROVE)) return;
        wrapper.eq(SettleDiscountApproval::getRequiredLevel, SettlementDiscountApprovalLevel.FINANCE.name());
    }

    private void applyKeyword(LambdaQueryWrapper<SettleDiscountApproval> wrapper, String keyword) {
        if (!StringUtils.hasText(keyword)) return;
        String value = keyword.trim();
        List<String> settleUuids = settleOrderMapper.selectList(new LambdaQueryWrapper<SettleOrder>()
                        .select(SettleOrder::getUuid)
                        .and(item -> item.like(SettleOrder::getSettleNo, value)
                                .or().like(SettleOrder::getCustomerName, value)))
                .stream().map(SettleOrder::getUuid).toList();
        if (settleUuids.isEmpty()) wrapper.eq(SettleDiscountApproval::getSettleUuid, "__none__");
        else wrapper.in(SettleDiscountApproval::getSettleUuid, settleUuids);
    }

    private PageResult<SettleDiscountApprovalVO> toResult(Page<SettleDiscountApproval> page) {
        List<String> settleUuids = page.getRecords().stream()
                .map(SettleDiscountApproval::getSettleUuid).distinct().toList();
        Map<String, SettleOrder> settles = settleUuids.isEmpty() ? Map.of()
                : settleOrderMapper.selectBatchIds(settleUuids).stream()
                .collect(Collectors.toMap(SettleOrder::getUuid, Function.identity()));
        PageResult<SettleDiscountApprovalVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream()
                .map(item -> SettleDiscountApprovalVO.from(item, settles.get(item.getSettleUuid()))).toList());
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    private LambdaQueryWrapper<SettleDiscountApproval> baseSettleQuery(String settleUuid) {
        return new LambdaQueryWrapper<SettleDiscountApproval>()
                .eq(SettleDiscountApproval::getSettleUuid, settleUuid)
                .orderByDesc(SettleDiscountApproval::getRequestTime);
    }

    private CurrentUser currentUser() {
        return AuthContextHolder.getCurrentUser();
    }
}
