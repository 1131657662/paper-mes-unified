package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRollDispositionDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.model.ProcessRollDispositionAction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProcessRollDispositionPolicyTest {

    @Test
    void blankReason_isRejectedBeforeLengthValidation() {
        ProcessRollDispositionDTO command = command();
        command.setReason("  ");

        assertThatThrownBy(() -> ProcessRollDispositionPolicy.requireCommand(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("原因");
    }

    @Test
    void oversizedReason_isRejectedAtServiceBoundary() {
        ProcessRollDispositionDTO command = command();
        command.setReason("x".repeat(501));

        assertThatThrownBy(() -> ProcessRollDispositionPolicy.requireCommand(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("500");
    }

    @Test
    void oversizedWarehouse_isRejectedAtServiceBoundary() {
        ProcessRollDispositionDTO command = command();
        command.setWarehouseUuid("w".repeat(65));

        assertThatThrownBy(() -> ProcessRollDispositionPolicy.requireCommand(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("64");
    }

    @Test
    void completedRoll_isRejectedEvenWhenLegacyCheckFlagIsMissing() {
        OriginalRoll roll = new OriginalRoll();
        roll.setRollStatus(3);
        roll.setIsChecked(0);

        assertThatThrownBy(() -> ProcessRollDispositionPolicy.requireRollEditable(roll))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已回录或已处置");
    }

    @Test
    void legacyRollWithoutStatus_isRejectedAsBusinessError() {
        OriginalRoll roll = new OriginalRoll();
        roll.setIsChecked(0);

        assertThatCode(() -> ProcessRollDispositionPolicy.requireRollEditable(roll))
                .doesNotThrowAnyException();
    }

    private ProcessRollDispositionDTO command() {
        ProcessRollDispositionDTO command = new ProcessRollDispositionDTO();
        command.setAction(ProcessRollDispositionAction.CANCEL);
        command.setRequestId("request-1");
        command.setReason("客户取消本次加工");
        command.setExpectedOrderVersion(1);
        return command;
    }
}
