import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import ReportInsightStrip from './ReportInsightStrip'

describe('报表洞察', () => {
  it('按后端生效阈值判断异常', () => {
    const markup = renderToStaticMarkup(<ReportInsightStrip overview={{
      settledAmount: 100, unreceivedAmount: 40, pendingSettleAmount: 0, lossRatio: 4,
      orderCount: 0, originalRollCount: 0, finishRollCount: 0, originalWeight: 0,
      finishWeight: 0, lossWeight: 0, knifeCount: 0, sawAmount: 0, rewindAmount: 0,
      processAmount: 0, extraAmount: 0, totalAmount: 0, receivedAmount: 0,
      cashReceivedAmount: 0, scrapOffsetAmount: 0,
    }} thresholds={{ asOf: '2026-07-20T12:00:00', thresholds: [
      { ruleUuid: 'cash', signalCode: 'UNRECEIVED_RATIO', comparisonOperator: 'GTE',
        thresholdValue: 35, severity: 1, scopeType: 1, scopeLabel: '全局' },
      { ruleUuid: 'loss', signalCode: 'LOSS_RATIO', comparisonOperator: 'GTE',
        thresholdValue: 8, severity: 2, scopeType: 2, scopeLabel: '客户专属' },
    ] }} />)

    expect(markup).toContain('未收占比较高')
    expect(markup).toContain('损耗处于阈值内')
    expect(markup).toContain('客户专属')
    expect(markup).toContain('title="已结算未收')
  })
})
