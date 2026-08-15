package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.OrderSettlementMode;
import com.paper.mes.processorder.dto.ProcessOrderCreateDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;

import java.util.List;

/** Builds a new editable order draft from one source roll. */
public final class ProcessRollSplitFactory {
    private ProcessRollSplitFactory() {
    }

    public static ProcessOrderCreateDTO orderRequest(ProcessOrder sourceOrder, OriginalRoll sourceRoll) {
        ProcessOrderCreateDTO dto = new ProcessOrderCreateDTO();
        dto.setCustomerUuid(sourceOrder.getCustomerUuid());
        dto.setOrderDate(sourceOrder.getOrderDate());
        dto.setExpectFinishDate(sourceOrder.getExpectFinishDate());
        dto.setPriority(sourceOrder.getPriority());
        dto.setLabelBrand(sourceOrder.getLabelBrand());
        dto.setWarehouseUuid(sourceOrder.getWarehouseUuid());
        dto.setIsInvoice(sourceOrder.getIsInvoice());
        dto.setSettleType(sourceOrder.getSettleType());
        dto.setSettleDay(sourceOrder.getSettleDay());
        copySettlementIntent(sourceOrder, dto);
        dto.setTaxRate(sourceOrder.getTaxRate());
        dto.setUrgentFee(sourceOrder.getUrgentFee());
        dto.setPalletFee(sourceOrder.getPalletFee());
        dto.setLoadingFee(sourceOrder.getLoadingFee());
        dto.setFreightFee(sourceOrder.getFreightFee());
        dto.setOtherFee(sourceOrder.getOtherFee());
        dto.setRemark("拆分自加工单 " + sourceOrder.getOrderNo());
        dto.setOriginalRolls(List.of(rollRequest(sourceRoll)));
        return dto;
    }

    private static void copySettlementIntent(ProcessOrder sourceOrder, ProcessOrderCreateDTO target) {
        if (OrderSettlementMode.OVERRIDE.name().equals(sourceOrder.getSettleSource())) {
            target.setSettleMode(OrderSettlementMode.OVERRIDE);
            target.setCustomerVersion(sourceOrder.getSettleCustomerVersion());
            target.setSettleOverrideReason(sourceOrder.getSettleOverrideReason());
            return;
        }
        if (OrderSettlementMode.INHERIT.name().equals(sourceOrder.getSettleSource())) {
            target.setSettleMode(OrderSettlementMode.INHERIT);
            target.setCustomerVersion(sourceOrder.getSettleCustomerVersion());
        }
    }

    private static OriginalRollDTO rollRequest(OriginalRoll source) {
        OriginalRollDTO dto = new OriginalRollDTO();
        dto.setExtraNo(source.getExtraNo());
        dto.setRollNo(source.getRollNo());
        dto.setPaperName(source.getPaperName());
        dto.setGramWeight(source.getGramWeight());
        dto.setOriginalWidth(source.getOriginalWidth());
        dto.setActualWidth(source.getActualWidth());
        dto.setOriginalDiameter(source.getOriginalDiameter());
        dto.setCoreDiameter(source.getCoreDiameter());
        dto.setOriginalLength(source.getOriginalLength());
        applyPendingWeight(source, dto);
        dto.setPieceNum(source.getPieceNum());
        dto.setBatchNo(source.getBatchNo());
        dto.setDamageDesc(source.getDamageDesc());
        dto.setProcessMode(source.getProcessMode());
        dto.setMainStepType(source.getMainStepType());
        dto.setMachineUuid(source.getMachineUuid());
        dto.setRemark(source.getRemark());
        return dto;
    }

    private static void applyPendingWeight(OriginalRoll source, OriginalRollDTO target) {
        com.paper.mes.processorder.model.WeightStatus status = weightStatus(source);
        if (status == com.paper.mes.processorder.model.WeightStatus.MEASURED) {
            target.setRollWeight(null);
            target.setWeightStatus(com.paper.mes.processorder.model.WeightStatus.UNKNOWN);
            return;
        }
        target.setRollWeight(source.getRollWeight());
        target.setWeightStatus(status);
    }

    private static com.paper.mes.processorder.model.WeightStatus weightStatus(OriginalRoll source) {
        if (source.getWeightStatus() == null || source.getWeightStatus().isBlank()) {
            return source.getRollWeight() != null && source.getRollWeight().signum() > 0
                    ? com.paper.mes.processorder.model.WeightStatus.ESTIMATED
                    : com.paper.mes.processorder.model.WeightStatus.UNKNOWN;
        }
        try {
            return com.paper.mes.processorder.model.WeightStatus.valueOf(
                    source.getWeightStatus());
        } catch (IllegalArgumentException ignored) {
            return com.paper.mes.processorder.model.WeightStatus.UNKNOWN;
        }
    }
}
