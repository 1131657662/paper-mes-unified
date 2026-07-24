import {
  BarChartOutlined,
  CheckCircleOutlined,
  FileDoneOutlined,
  ScissorOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'
import type { ReportTopicAnalysisVO } from '../../../types/report'
import { formatNumber, formatPercent, formatTonFromKg } from '../utils/reportFormatters'

interface Props {
  analysis: ReportTopicAnalysisVO
}

export default function ReportTopicMetrics({ analysis }: Props) {
  const overview = analysis.overview
  const metrics = analysis.topicCode === 'production'
    ? productionMetrics(overview)
    : qualityMetrics(overview, analysis.lossLeaders.length)
  return (
    <section className="report-topic-metrics" aria-label="主题关键指标">
      {metrics.map((metric) => (
        <article className={`report-topic-metric report-topic-metric--${metric.tone}`} key={metric.label}
          title={`${metric.label}：${metric.value}，${metric.hint}`}>
          <span className="report-topic-metric__icon">{metric.icon}</span>
          <div><span>{metric.label}</span><strong>{metric.value}</strong><small>{metric.hint}</small></div>
        </article>
      ))}
    </section>
  )
}

function productionMetrics(value: ReportTopicAnalysisVO['overview']): Metric[] {
  return [
    metric('完成加工单', `${formatNumber(value.orderCount)} 单`, '有效完成及已结算', <FileDoneOutlined />),
    metric('投入原纸', formatTonFromKg(value.originalWeight), `${formatNumber(value.originalRollCount)} 卷`, <BarChartOutlined />),
    metric('成品产出', formatTonFromKg(value.finishWeight), `${formatNumber(value.finishRollCount)} 卷`, <CheckCircleOutlined />, 'primary'),
    metric('综合损耗', formatTonFromKg(value.lossWeight), formatPercent(value.lossRatio), <ScissorOutlined />, 'warning'),
    metric('锯纸刀数', formatNumber(value.knifeCount), '已回录有效工序', <ScissorOutlined />),
  ]
}

function qualityMetrics(value: ReportTopicAnalysisVO['overview'], leaderCount: number): Metric[] {
  return [
    metric('核查加工单', `${formatNumber(value.orderCount)} 单`, '当前筛选范围', <FileDoneOutlined />),
    metric('损耗重量', formatTonFromKg(value.lossWeight), '按原卷损耗汇总', <WarningOutlined />, 'warning'),
    metric('综合损耗率', formatPercent(value.lossRatio), '总损耗 / 总投入', <ScissorOutlined />, 'danger'),
    metric('成品产出', formatTonFromKg(value.finishWeight), `投入 ${formatTonFromKg(value.originalWeight)}`, <CheckCircleOutlined />),
    metric('重点追溯', `${leaderCount} 单`, '按损耗率优先排序', <BarChartOutlined />, 'primary'),
  ]
}

function metric(label: string, value: string, hint: string, icon: ReactNode,
  tone: Metric['tone'] = 'default'): Metric {
  return { label, value, hint, icon, tone }
}

interface Metric {
  label: string
  value: string
  hint: string
  icon: ReactNode
  tone: 'danger' | 'default' | 'primary' | 'warning'
}
