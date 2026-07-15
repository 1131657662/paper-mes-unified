import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { SettleDetailVO } from '../../types/settle'
import SettlePrintSheet from './SettlePrintSheet'

describe('客户结算单预览', () => {
  it('用计费依据替代重复的加工内容并汇总额外费和税费', () => {
    const markup = renderToStaticMarkup(<SettlePrintSheet detail={detail()} />)

    expect(markup).toContain('加工项目')
    expect(markup).toContain('计费依据')
    expect(markup).toContain('6刀 × 24元/刀 = ¥144.00')
    expect(markup).toContain('额外费：¥80.00（装卸费 80.00）')
    expect(markup).toContain('税费：¥29.12')
    expect(markup).not.toContain('加工内容')
    expect(markup).not.toContain('演示锯纸机')
    expect(markup).not.toContain('直径 1300')
    expect(markup).not.toContain('纸芯 76')
  })
})

function detail(): SettleDetailVO {
  return {
    order: {
      uuid: 'settle-1',
      settleNo: 'JS001',
      customerUuid: 'customer-1',
      customerName: '测试客户',
      settleType: 1,
      settleDate: '2026-07-15',
      sawAmount: 144,
      rewindAmount: 0,
      extraAmount: 80,
      amountNoTax: 224,
      taxAmount: 0,
      totalAmount: 253.12,
      receivedAmount: 0,
      unreceivedAmount: 253.12,
      isInvoice: 1,
      settleStatus: 1,
    },
    details: [],
    receives: [],
    printLines: [{
      settleUuid: 'settle-1',
      orderUuid: 'order-1',
      orderNo: 'JG001',
      orderDate: '2026-07-15',
      originalUuid: 'roll-1',
      originalLabel: '母卷1',
      originalRollNo: 'R001',
      paperName: '白卡',
      gramWeight: 300,
      originalWidth: 2520,
      originalDiameter: 1300,
      coreDiameter: 76,
      originalWeight: 2200,
      processText: '标准锯纸',
      processStepSummary: '标准锯纸（6刀 / 2200kg / 单价 24.00） / 机台演示锯纸机',
      finishSummary: 'A000001、A000002',
      finishCount: 2,
      finishWeight: 2000,
      trimWeight: 20,
      trimSummary: '20 mm / 20 kg',
      processAmount: 144,
      extraAmount: 80,
      extraFeeSummary: '装卸费 80.00',
      taxAmount: 29.12,
      lineAmount: 253.12,
      feeLines: [{
        feeType: 'saw',
        feeName: '锯纸',
        formulaText: '6刀 × 24元/刀',
        amountNoTax: 144,
      }, {
        feeType: 'tax',
        feeName: '开票加价',
        taxAmount: 29.12,
        amountTax: 29.12,
      }],
    }],
  }
}
