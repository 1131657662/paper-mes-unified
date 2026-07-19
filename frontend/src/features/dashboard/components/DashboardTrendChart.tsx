import { useRef } from 'react'
import type { EChartsType } from 'echarts/core'
import ReactEChartsCore from 'echarts-for-react/esm/core'
import lineChartRuntime from '../../../components/charts/lineChartRuntime'
import { buildTrendChartOption } from './dashboardTrendChartOption'
import type { DashboardTrendModel } from './dashboardTrendModel'
import { axisDataIndex } from './dashboardTrendPointer'

export default function DashboardTrendChart({ model }: { model: DashboardTrendModel }) {
  const chartRef = useRef<ReactEChartsCore>(null)
  const lineRef = useRef<HTMLSpanElement>(null)

  const hideTooltip = () => {
    const chart = chartRef.current?.getEchartsInstance()
    if (lineRef.current) lineRef.current.hidden = true
    chart?.dispatchAction({ type: 'hideTip' })
    chart?.dispatchAction({ type: 'updateAxisPointer', currTrigger: 'leave' })
  }

  const movePointer = (event: unknown) => {
    const chart = chartRef.current?.getEchartsInstance()
    const line = lineRef.current
    if (!chart || !line) return
    const index = axisDataIndex(event)
    if (index < 0) {
      line.hidden = true
      return
    }
    line.hidden = false
    line.style.transform = `translateX(${axisPixel(chart, index)}px)`
  }

  return (
    <div className="dashboard-trend__chart-shell" onMouseLeave={hideTooltip}>
      <ReactEChartsCore
        ref={chartRef}
        aria-label="近12个月加工应收趋势"
        className="dashboard-trend__chart"
        echarts={lineChartRuntime}
        lazyUpdate
        notMerge
        onEvents={{ updateAxisPointer: movePointer }}
        option={buildTrendChartOption(model)}
        opts={{ renderer: 'svg' }}
      />
      <span ref={lineRef} className="dashboard-trend__axis-pointer" hidden />
    </div>
  )
}

function axisPixel(chart: EChartsType, index: number): number {
  const pixel = chart.convertToPixel({ xAxisIndex: 0 }, index)
  if (typeof pixel === 'number') return pixel
  return Array.isArray(pixel) ? Number(pixel[0]) : Number.NaN
}
