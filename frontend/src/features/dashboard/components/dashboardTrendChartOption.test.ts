import { describe, expect, it } from 'vitest'
import { buildTrendChartOption } from './dashboardTrendChartOption'

describe('仪表盘加工应收趋势图', () => {
  it('使用 ECharts 6 外边界约束容纳坐标轴标签', () => {
    const option = buildTrendChartOption({
      averageAmount: 0,
      hasReceivable: false,
      maxAmount: 0,
      monthly: [],
      totalAmount: 0,
      totalOrders: 0,
    })

    expect(option.grid).toMatchObject({
      outerBoundsMode: 'same',
      outerBoundsContain: 'axisLabel',
      left: 8,
      right: 14,
      top: 18,
      bottom: 8,
    })
    expect(option.grid).not.toHaveProperty('containLabel')
  })
})
