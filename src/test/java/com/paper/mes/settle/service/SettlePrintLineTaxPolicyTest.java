package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettleFeeLineVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettlePrintLineTaxPolicyTest {

    @Test
    void nonInvoiceLineNeverContributesTaxFromLegacyAmountDifference() {
        SettlePrintLineVO line = line(2);
        line.setProcessAmount(new BigDecimal("45.00"));
        line.setExtraAmount(BigDecimal.ZERO);
        line.setLineAmount(new BigDecimal("1325.00"));

        assertEquals(BigDecimal.ZERO, SettlePrintLineTaxPolicy.amount(line));
    }

    @Test
    void invoiceLineFallsBackToFeeTaxWhenTaxFieldIsMissing() {
        SettlePrintLineVO line = line(1);
        SettleFeeLineVO fee = new SettleFeeLineVO();
        fee.setFeeType("tax");
        fee.setTaxAmount(new BigDecimal("128.00"));
        line.setFeeLines(List.of(fee));

        assertEquals(new BigDecimal("128.00"), SettlePrintLineTaxPolicy.amount(line));
    }

    @Test
    void normalizeNonInvoiceRemovesLegacyTaxLines() {
        SettlePrintLineVO line = line(2);
        SettleFeeLineVO tax = new SettleFeeLineVO();
        tax.setFeeType("tax");
        SettleFeeLineVO extra = new SettleFeeLineVO();
        extra.setFeeType("extra");
        line.setFeeLines(List.of(tax, extra));
        line.setTaxAmount(new BigDecimal("128.00"));
        line.setTaxRate(new BigDecimal("13.00"));

        SettlePrintLineTaxPolicy.normalizeNonInvoice(line);

        assertEquals(BigDecimal.ZERO, line.getTaxAmount());
        assertEquals(BigDecimal.ZERO, line.getTaxRate());
        assertEquals(new BigDecimal("128.00"), line.getHistoricalDifferenceAmount());
        assertEquals(List.of(extra), line.getFeeLines());
    }

    private SettlePrintLineVO line(int isInvoice) {
        SettlePrintLineVO line = new SettlePrintLineVO();
        line.setIsInvoice(isInvoice);
        return line;
    }
}
