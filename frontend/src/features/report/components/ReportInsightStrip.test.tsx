import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { ReportDetailVO } from '../../../types/report'
import ReportInsightStrip from './ReportInsightStrip'

describe('报表重量洞察', () => {
  it('将成品重量正差作为业务提示而不是错误', () => {
    const markup = renderToStaticMarkup(
      <ReportInsightStrip details={[detailWithPositiveWeightDifference()]} />,
    )

    expect(markup).toContain('存在成品重量正差')
    expect(markup).toContain('业务允许该情况')
    expect(markup).not.toContain('重量守恒异常')
  })
})

function detailWithPositiveWeightDifference(): ReportDetailVO {
  return {
    accountingDate: '2026-07-02', cashReceivedAmount: 0, customerName: '测试客户',
    extraAmount: 0, finishRollCount: 1, finishWeight: 1010, isInvoice: 2,
    knifeCount: 0, lossRatio: 0, lossWeight: 0, orderDate: '2026-06-30',
    orderNo: 'JG202606300001', orderStatus: 4, orderUuid: 'order-1',
    originalRollCount: 1, originalWeight: 1000, paperSummary: '测试纸',
    pendingSettleAmount: 0, processAmount: 0, processSummary: '复卷',
    receivedAmount: 0, rewindAmount: 0, sawAmount: 0, scrapOffsetAmount: 0,
    settleType: 2, settledAmount: 0, totalAmount: 0, unreceivedAmount: 0,
  }
}
