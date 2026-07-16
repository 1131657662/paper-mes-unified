import { describe, expect, it } from 'vitest'
import { axisDataIndex } from './dashboardTrendPointer'

describe('仪表盘趋势图月份指示线', () => {
  it('读取 ECharts 6 坐标轴事件中的月份索引', () => {
    expect(axisDataIndex({ axesInfo: [{ axisDim: 'x', seriesDataIndices: [{ dataIndex: 6 }] }] })).toBe(6)
  })

  it('兼容旧版数据索引事件', () => {
    expect(axisDataIndex({ seriesDataIndices: [{ dataIndex: 9 }] })).toBe(9)
  })
})
