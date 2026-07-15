package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.auth.service.PasswordService;
import com.paper.mes.common.BusinessException;
import com.paper.mes.oplog.entity.OperationLog;
import com.paper.mes.oplog.mapper.OperationLogMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.service.ProcessOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class BackRecordSecurityBusinessFlowIT {

    @Autowired private BackRecordOnSiteFixture fixture;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private ProcessOrderMapper processOrderMapper;
    @Autowired private OriginalRollMapper originalRollMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired private PasswordService passwordService;
    @Autowired private OperationLogMapper operationLogMapper;

    @Test
    void backRecord_whenOrderRollIsMissingFromRequest_rejectsBeforeWritingAnyWeight() {
        BackRecordOnSiteFixture.Scenario scenario = fixture.arrange();
        originalRollMapper.insert(omittedRoll(scenario.order()));

        assertThatThrownBy(() -> processOrderService.backRecord(
                scenario.order().getUuid(), fixture.request(scenario, 900, 800)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MISSING-ROLL");

        assertThat(originalRollMapper.selectById(scenario.roll().getUuid()).getActualWeight()).isNull();
        assertThat(processOrderMapper.selectById(scenario.order().getUuid()).getOrderStatus()).isEqualTo(3);
    }

    @Test
    void overToleranceRelease_withEnabledAdminCredentials_completesOrder() {
        BackRecordOnSiteFixture.Scenario scenario = fixture.arrange();
        BackRecordDTO request = blockedRequest(scenario);
        SysUser admin = user("admin", "授权管理员", "admin");
        userMapper.insert(admin);
        request.setReleaseAdminUsername(admin.getUsername());
        request.setReleaseAdminPassword("correct-password");
        request.setReleaseReason("现场复核后确认放行");

        processOrderService.backRecord(scenario.order().getUuid(), request);

        ProcessOrder completed = processOrderMapper.selectById(scenario.order().getUuid());
        OperationLog releaseLog = operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getBizUuid, scenario.order().getUuid())
                .eq(OperationLog::getActionType, OperationLogService.ACTION_OVER_TOLERANCE_RELEASE))
                .getFirst();
        assertThat(completed.getOrderStatus()).isEqualTo(4);
        assertThat(completed.getBackRecordUser()).isEqualTo("system");
        assertThat(completed.getSnapFinish()).contains("\"back_record_user\": \"system\"");
        assertThat(releaseLog.getOperator()).isEqualTo("授权管理员");
        assertThat(releaseLog.getRemark()).contains("授权账号=" + admin.getUsername());
    }

    @Test
    void overToleranceRelease_withOperatorCredentials_rejectsRelease() {
        BackRecordOnSiteFixture.Scenario scenario = fixture.arrange();
        BackRecordDTO request = blockedRequest(scenario);
        SysUser operator = user("operator", "普通回录员", "operator");
        userMapper.insert(operator);
        request.setReleaseAdminUsername(operator.getUsername());
        request.setReleaseAdminPassword("correct-password");
        request.setReleaseReason("尝试自行放行");

        assertThatThrownBy(() -> processOrderService.backRecord(scenario.order().getUuid(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无放行权限");
    }

    @Test
    void warnVariance_withoutReason_rejectsCompletion() {
        BackRecordOnSiteFixture.Scenario scenario = fixture.arrange();
        BackRecordDTO request = warnRequest(scenario);

        assertThatThrownBy(() -> processOrderService.backRecord(scenario.order().getUuid(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("填写原因");
    }

    @Test
    void warnVariance_withReason_completesAndRecordsReason() {
        BackRecordOnSiteFixture.Scenario scenario = fixture.arrange();
        BackRecordDTO request = warnRequest(scenario);
        request.setVarianceReason("原纸含水率变化，现场已复核");

        processOrderService.backRecord(scenario.order().getUuid(), request);

        OperationLog log = operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getBizUuid, scenario.order().getUuid())
                .eq(OperationLog::getActionType, OperationLogService.ACTION_WEIGHT_VARIANCE_CONFIRM))
                .getFirst();
        assertThat(processOrderMapper.selectById(scenario.order().getUuid()).getOrderStatus()).isEqualTo(4);
        assertThat(log.getRemark()).contains("原纸含水率变化");
    }

    private BackRecordDTO blockedRequest(BackRecordOnSiteFixture.Scenario scenario) {
        BackRecordDTO request = fixture.request(scenario, 900, 800);
        request.getRolls().getFirst().setActualWeight(new BigDecimal("1000.000"));
        return request;
    }

    private BackRecordDTO warnRequest(BackRecordOnSiteFixture.Scenario scenario) {
        BackRecordDTO request = fixture.request(scenario, 900, 800);
        request.getRolls().getFirst().setActualWeight(new BigDecimal("208.000"));
        return request;
    }

    private SysUser user(String prefix, String realName, String roleCode) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        SysUser user = new SysUser();
        user.setUsername(prefix + "-" + suffix);
        user.setPasswordHash(passwordService.encode("correct-password"));
        user.setRealName(realName);
        user.setRoleCode(roleCode);
        user.setStatus(1);
        return user;
    }

    private OriginalRoll omittedRoll(ProcessOrder order) {
        OriginalRoll roll = new OriginalRoll();
        roll.setOrderUuid(order.getUuid());
        roll.setRowSort(99);
        roll.setRollNo("MISSING-ROLL");
        roll.setPaperName("integration-paper");
        roll.setGramWeight(80);
        roll.setOriginalWidth(1200);
        roll.setRollWeight(new BigDecimal("100.000"));
        roll.setPieceNum(1);
        roll.setTotalWeight(new BigDecimal("100.000"));
        roll.setProcessMode(2);
        roll.setMainStepType(1);
        roll.setRollStatus(1);
        return roll;
    }
}
