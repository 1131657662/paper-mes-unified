package com.paper.mes.inventory.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.inventory.dto.InventoryLedgerCommand;
import com.paper.mes.inventory.entity.InventoryLedgerEventType;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

public final class InventoryLedgerCommandValidator {

    private static final int MAX_DECIMAL_SCALE = 3;
    private static final BigDecimal MAX_ABS_VALUE = new BigDecimal("99999999999.999");

    private InventoryLedgerCommandValidator() {
    }

    public static void validate(InventoryLedgerCommand command, boolean openingCommand) {
        if (command == null) {
            throw new BusinessException("inventory command is required");
        }
        requireText(command.getFinishRollUuid(), "finishRollUuid");
        if (command.getEventType() == null) {
            throw new BusinessException("eventType is required");
        }
        if (openingCommand != (command.getEventType() == InventoryLedgerEventType.OPENING_BALANCE)) {
            throw new BusinessException("opening balance must use the explicit opening command");
        }
        requireText(command.getSourceBusinessType(), "sourceBusinessType");
        requireText(command.getSourceBusinessUuid(), "sourceBusinessUuid");
        requireText(command.getIdempotencyKey(), "idempotencyKey");
        validateNumber(command.getQuantityDelta(), "quantityDelta");
        validateNumber(command.getWeightDelta(), "weightDelta");
        validateNumber(command.getReservedQuantityDelta(), "reservedQuantityDelta");
        validateNumber(command.getReservedWeightDelta(), "reservedWeightDelta");
        if (command.getWeightDelta() == null) {
            throw new BusinessException("weightDelta is required");
        }
        if (command.getQuantityDelta() == null) {
            throw new BusinessException("quantityDelta is required");
        }
        if (command.getReservedQuantityDelta() == null) {
            throw new BusinessException("reservedQuantityDelta is required");
        }
        if (command.getReservedWeightDelta() == null) {
            throw new BusinessException("reservedWeightDelta is required");
        }
        validateEventDelta(command);
        if ((command.getEventType() == InventoryLedgerEventType.SCRAP
                || command.getEventType() == InventoryLedgerEventType.ADJUSTMENT)
                && !StringUtils.hasText(command.getReason())) {
            throw new BusinessException("reason is required for scrap or adjustment");
        }
    }

    private static void validateEventDelta(InventoryLedgerCommand command) {
        BigDecimal quantity = command.getQuantityDelta();
        BigDecimal weight = command.getWeightDelta();
        BigDecimal reservedQuantity = command.getReservedQuantityDelta();
        BigDecimal reservedWeight = command.getReservedWeightDelta();
        InventoryLedgerEventType event = command.getEventType();
        boolean valid = switch (event) {
            case OPENING_BALANCE -> quantity.signum() >= 0 && weight.signum() >= 0
                    && reservedWeight.signum() >= 0 && reservedQuantity.signum() >= 0
                    && reservedWeight.compareTo(weight) <= 0
                    && reservedQuantity.compareTo(quantity) <= 0;
            case RECEIPT -> quantity.signum() >= 0 && weight.signum() > 0
                    && reservedWeight.signum() == 0 && reservedQuantity.signum() == 0;
            case RETURN -> weight.signum() > 0 && quantity.signum() >= 0
                    && reservedWeight.signum() >= 0 && reservedQuantity.signum() >= 0
                    && (reservedWeight.signum() == 0 || reservedWeight.compareTo(weight) == 0);
            case RESERVE -> quantity.signum() == 0 && weight.signum() == 0
                    && reservedWeight.signum() > 0 && reservedQuantity.signum() >= 0;
            case RELEASE -> quantity.signum() == 0 && weight.signum() == 0
                    && reservedWeight.signum() < 0 && reservedQuantity.signum() <= 0;
            case ISSUE -> weight.signum() < 0 && quantity.signum() <= 0
                    && reservedWeight.signum() < 0 && reservedQuantity.signum() <= 0
                    && reservedWeight.compareTo(weight) == 0;
            case SCRAP -> weight.signum() < 0 && quantity.signum() <= 0
                    && reservedWeight.signum() == 0 && reservedQuantity.signum() == 0;
            case ADJUSTMENT -> quantity.signum() != 0 || weight.signum() != 0
                    || reservedQuantity.signum() != 0 || reservedWeight.signum() != 0;
        };
        if (!valid) {
            throw new BusinessException("inventory event delta has invalid sign or balance scope (negative or mismatched delta)");
        }
    }

    private static void validateNumber(BigDecimal value, String name) {
        if (value == null) {
            return;
        }
        if (value.scale() > MAX_DECIMAL_SCALE || value.abs().compareTo(MAX_ABS_VALUE) > 0) {
            throw new BusinessException(name + " exceeds DECIMAL(14,3) range");
        }
    }

    private static void requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(name + " is required");
        }
    }
}
