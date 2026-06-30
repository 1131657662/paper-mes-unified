import { Alert, Tag } from 'antd'
import type { ReportOverviewVO } from '../../../types/report'
import { formatMoney, formatPercent } from '../utils/reportFormatters'

interface Props {
  overview?: ReportOverviewVO
}

export default function ReportInsightStrip({ overview }: Props) {
  const insights = buildInsights(overview)

  return (
    <div className="report-insights">
      {insights.map((item) => (
        <Alert
          key={item.title}
          className="report-insight"
          message={<InsightMessage item={item} />}
          showIcon={false}
          type={item.type}
        />
      ))}
    </div>
  )
}

interface Insight {
  detail: string
  tag: string
  title: string
  type: 'error' | 'info' | 'success' | 'warning'
}

function InsightMessage({ item }: { item: Insight }) {
  return (
    <div className="report-insight__message">
      <Tag color={tagColor(item.type)}>{item.tag}</Tag>
      <strong>{item.title}</strong>
      <span>{item.detail}</span>
    </div>
  )
}

function buildInsights(overview?: ReportOverviewVO): Insight[] {
  const total = Number(overview?.totalAmount ?? 0)
  const unreceived = Number(overview?.unreceivedAmount ?? 0)
  const lossRatio = Number(overview?.lossRatio ?? 0)
  const rewind = Number(overview?.rewindAmount ?? 0)
  const saw = Number(overview?.sawAmount ?? 0)
  const unreceivedRatio = total > 0 ? (unreceived / total) * 100 : 0
  return [
    {
      detail: `未收 ${formatMoney(unreceived)}，占应收 ${formatPercent(unreceivedRatio)}`,
      tag: unreceivedRatio >= 35 ? '关注' : '回款',
      title: unreceivedRatio >= 35 ? '未收占比较高' : '回款压力可控',
      type: unreceivedRatio >= 35 ? 'warning' : 'success',
    },
    {
      detail: `当前损耗率 ${formatPercent(lossRatio)}，建议结合产品和工艺维度定位高损耗来源`,
      tag: lossRatio >= 5 ? '异常' : '损耗',
      title: lossRatio >= 5 ? '损耗率偏高' : '损耗处于常规范围',
      type: lossRatio >= 5 ? 'error' : 'info',
    },
    {
      detail: `锯纸费 ${formatMoney(saw)}，复卷费 ${formatMoney(rewind)}`,
      tag: '结构',
      title: rewind > saw ? '复卷贡献更高' : '锯纸贡献更高',
      type: 'info',
    },
  ]
}

function tagColor(type: Insight['type']) {
  if (type === 'error') return 'error'
  if (type === 'warning') return 'warning'
  if (type === 'success') return 'success'
  return 'blue'
}
