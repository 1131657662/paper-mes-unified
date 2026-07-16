import dayjs from 'dayjs'
import { describe, expect, it } from 'vitest'
import { buildTrendChartOption, formatTrendTooltip } from './dashboardTrendChartOption'
import { buildTrendModel } from './dashboardTrendModel'

describe('仪表盘近12个月趋势模型', () => {
  it('以当前月份为终点补齐连续12个月', () => {
    const model = buildTrendModel([{ month: '2026-05', amount: 1200, orderCount: 2 }], dayjs('2026-07-16'))

    expect(model.monthly.map((item) => item.month)).toEqual([
      '2025-08', '2025-09', '2025-10', '2025-11', '2025-12', '2026-01',
      '2026-02', '2026-03', '2026-04', '2026-05', '2026-06', '2026-07',
    ])
  })

  it('月均金额使用完整12个月口径', () => {
    const model = buildTrendModel([
      { month: '2026-05', amount: 1200, orderCount: 2 },
      { month: '2026-06', amount: 600, orderCount: 1 },
    ], dayjs('2026-07-16'))

    expect(model.totalAmount).toBe(1800)
    expect(model.averageAmount).toBe(150)
    expect(model.totalOrders).toBe(3)
  })

  it('使用按月份触发的虚线和单调平滑曲线', () => {
    const model = buildTrendModel([
      { month: '2026-05', amount: 10000, orderCount: 1 },
      { month: '2026-06', amount: 0, orderCount: 0 },
    ], dayjs('2026-07-16'))
    const option = buildTrendChartOption(model)

    expect(option).toMatchObject({
      tooltip: {
        className: 'dashboard-trend__tooltip',
        trigger: 'axis',
        triggerOn: 'mousemove',
        axisPointer: { type: 'none' },
      },
      series: [{
        smooth: 0.35,
        smoothMonotone: 'x',
        areaStyle: { color: { colorStops: [
          { offset: 0, color: 'rgba(22, 119, 255, 0.18)' },
          { offset: 1, color: 'rgba(22, 119, 255, 0.01)' },
        ] } },
      }],
    })
  })

  it('悬浮内容同时显示月份应收和完成单数', () => {
    const tooltip = formatTrendTooltip([{ data: { month: '2026-06', orderCount: 7, value: 12164.6 } }])

    expect(tooltip).toContain('2026年6月')
    expect(tooltip).toContain('¥12164.60')
    expect(tooltip).toContain('7 单')
  })
})
