import {
  formatKg,
  formatMoney,
  formatNumber,
  formatPercent,
  formatTon,
} from '../utils/reportFormatters'

interface Props {
  summary: {
    avgLossRatio: number
    machineCount: number
    totalAmount: number
    totalFinishWeight: number
    totalKnife: number
    totalLossWeight: number
    totalOrders: number
    totalTon: number
  }
}

export default function ReportMetricStrip({ summary }: Props) {
  return (
    <div className="report-metrics mes-metrics">
      <Metric title="完成加工单" main={`${formatNumber(summary.totalOrders)} 单`} sub={formatMoney(summary.totalAmount)} />
      <Metric title="原纸吨位" main={formatTon(summary.totalTon)} sub={`${formatNumber(summary.totalKnife)} 刀`} />
      <Metric title="成品重量" main={formatKg(summary.totalFinishWeight)} sub={`${summary.machineCount} 台机台`} />
      <Metric title="损耗表现" main={formatKg(summary.totalLossWeight)} sub={formatPercent(summary.avgLossRatio)} />
    </div>
  )
}

function Metric({ main, sub, title }: { title: string; main: string; sub: string }) {
  return (
    <div className="report-metric mes-metric">
      <span>{title}</span>
      <strong>{main}</strong>
      <em>{sub}</em>
    </div>
  )
}
