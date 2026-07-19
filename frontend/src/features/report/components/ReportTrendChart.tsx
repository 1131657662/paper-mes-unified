import ReactEChartsCore from 'echarts-for-react/esm/core'
import lineChartRuntime from '../../../components/charts/lineChartRuntime'
import type { ReportDimensionVO } from '../../../types/report'
import { buildReportTrendChartOption } from './reportTrendChartOption'

export default function ReportTrendChart({ monthly }: { monthly: ReportDimensionVO[] }) {
  return (
    <ReactEChartsCore
      aria-label="月度加工应收曲线"
      className="report-curve__echarts"
      echarts={lineChartRuntime}
      lazyUpdate
      notMerge
      option={buildReportTrendChartOption(monthly)}
      opts={{ renderer: 'svg' }}
    />
  )
}
