import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { ReportDimensionVO } from '../../../types/report'
import ReportTrendPanel from './ReportTrendPanel'

describe('报表月度趋势', () => {
  it('展示最新月份相对上月的环比方向和比较期间', () => {
    const markup = renderToStaticMarkup(
      <ReportTrendPanel monthly={[month('2026-06', 100), month('2026-07', 120)]} />,
    )

    expect(markup).toContain('本期应收')
    expect(markup).toContain('环比')
    expect(markup).toContain('上升 20.0%')
    expect(markup).toContain('较 2026-06')
    expect(markup).toContain('2026-07')
  })

  it('上期为零且本期有金额时显示新增', () => {
    const markup = renderToStaticMarkup(
      <ReportTrendPanel monthly={[month('2026-06', 0), month('2026-07', 120)]} />,
    )

    expect(markup).toContain('新增')
  })

  it('下降时明确展示方向而不只依赖图标', () => {
    const markup = renderToStaticMarkup(
      <ReportTrendPanel monthly={[month('2026-06', 120), month('2026-07', 86.4)]} />,
    )

    expect(markup).toContain('下降 28.0%')
  })
})

function month(key: string, amount: number): ReportDimensionVO {
  return {
    cashReceivedAmount: 0, dimensionKey: key, dimensionName: key, extraAmount: 0,
    finishRollCount: 0, finishWeight: 0, knifeCount: 0, lossRatio: 0, lossWeight: 0,
    orderCount: 0, originalRollCount: 0, originalWeight: 1000, pendingSettleAmount: 0,
    processAmount: amount, receivedAmount: 0, rewindAmount: 0, sawAmount: 0,
    scrapOffsetAmount: 0, settledAmount: 0, totalAmount: amount, unreceivedAmount: 0,
  }
}
