import { describe, expect, it } from 'vitest'
import type { ReportDimensionVO } from '../../../types/report'
import { fillMonthlySeries } from './reportMonthlySeries'

describe('报表月度序列', () => {
  it('补齐查询范围内没有业务的月份并填充零值', () => {
    const rows = [monthlyRow('2026-02', 10), monthlyRow('2026-04', 40)]

    const result = fillMonthlySeries(rows, { dateFrom: '2026-01-10', dateTo: '2026-04-20' })

    expect(result.map((row) => row.dimensionKey)).toEqual(['2026-01', '2026-02', '2026-03', '2026-04'])
    expect(result[0]?.totalAmount).toBe(0)
    expect(result[2]?.totalAmount).toBe(0)
  })

  it('没有任何业务数据时保留空状态而不是生成全零曲线', () => {
    expect(fillMonthlySeries([], { dateFrom: '2026-01-01', dateTo: '2026-03-31' })).toEqual([])
  })
})

function monthlyRow(key: string, totalAmount: number): ReportDimensionVO {
  return {
    cashReceivedAmount: 0, dimensionKey: key, dimensionName: key, extraAmount: 0,
    finishRollCount: 0, finishWeight: 0, knifeCount: 0, lossRatio: 0, lossWeight: 0,
    orderCount: 1, originalRollCount: 1, originalWeight: 1000, pendingSettleAmount: 0,
    processAmount: totalAmount, receivedAmount: 0, rewindAmount: 0, sawAmount: 0,
    scrapOffsetAmount: 0, settledAmount: 0, totalAmount, unreceivedAmount: 0,
  }
}
