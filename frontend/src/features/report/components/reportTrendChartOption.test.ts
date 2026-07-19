import { describe, expect, it } from 'vitest'
import type { ReportDimensionVO } from '../../../types/report'
import { buildReportTrendChartOption, formatReportTrendTooltip } from './reportTrendChartOption'

describe('报表月度加工应收曲线', () => {
  it('使用与仪表盘一致的单调平滑折线', () => {
    const option = buildReportTrendChartOption([reportPoint()])

    expect(option).toMatchObject({
      series: [{
        type: 'line',
        smooth: 0.35,
        smoothMonotone: 'x',
      }],
    })
  })

  it('悬浮提示包含月份、应收和原纸重量', () => {
    const tooltip = formatReportTrendTooltip([{
      data: { name: '2026-06', value: 12164.6, weight: 1250 },
    }])

    expect(tooltip).toContain('2026-06')
    expect(tooltip).toContain('¥12164.60')
    expect(tooltip).toContain('1.25 t')
  })
})

function reportPoint(): ReportDimensionVO {
  return {
    dimensionKey: '2026-06', dimensionName: '2026-06', orderCount: 1,
    originalRollCount: 1, finishRollCount: 1, originalWeight: 1250, finishWeight: 1200,
    lossWeight: 50, lossRatio: 0.04, knifeCount: 1, sawAmount: 0, rewindAmount: 0,
    processAmount: 12164.6, extraAmount: 0, totalAmount: 12164.6, settledAmount: 0,
    pendingSettleAmount: 12164.6, receivedAmount: 0, cashReceivedAmount: 0,
    scrapOffsetAmount: 0, unreceivedAmount: 12164.6,
  }
}
