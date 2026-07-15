package com.paper.mes.integration;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.OriginalRollRemarkDTO;
import com.paper.mes.processorder.dto.ProcessOrderRemarkDTO;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.service.ProcessOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class CompletedOrderRollProtectionBusinessFlowIT {

    @Autowired private BackRecordOnSiteFixture fixture;
    @Autowired private ProcessOrderService processOrderService;
    @Autowired private ProcessOrderMapper processOrderMapper;

    private BackRecordOnSiteFixture.Scenario scenario;

    @BeforeEach
    void setUp() {
        scenario = fixture.arrange();
        scenario.order().setOrderStatus(4);
        processOrderMapper.updateById(scenario.order());
    }

    @Test
    void completedOrder_rejectsRollStructureChanges() {
        assertBlocked(() -> processOrderService.addRoll(scenario.order().getUuid(), new OriginalRollDTO()));
        assertBlocked(() -> processOrderService.updateRoll(scenario.roll().getUuid(), new OriginalRollDTO()));
        assertBlocked(() -> processOrderService.deleteRoll(scenario.roll().getUuid()));
    }

    @Test
    void completedOrder_rejectsProductionRecordChanges() {
        assertBlocked(() -> processOrderService.changeRollStatus(scenario.roll().getUuid(), 4));
        MockMultipartFile image = new MockMultipartFile("files", "damage.png", "image/png", new byte[]{1});
        assertBlocked(() -> processOrderService.uploadDamageImages(
                scenario.roll().getUuid(), new MockMultipartFile[]{image}));
    }

    @Test
    void settledOrder_rejectsDirectRemarkChanges() {
        scenario.order().setOrderStatus(5);
        processOrderMapper.updateById(scenario.order());
        assertBlocked(() -> processOrderService.updateRollRemark(
                scenario.roll().getUuid(), new OriginalRollRemarkDTO()));
        assertBlocked(() -> processOrderService.updateOrderRemark(
                scenario.order().getUuid(), new ProcessOrderRemarkDTO()));
    }

    private void assertBlocked(ThrowingOperation operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(BusinessException.class)
                .hasMessageMatching(".*(锁定|状态|阶段).*");
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run();
    }
}
