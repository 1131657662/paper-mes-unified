import { Alert, Tag } from 'antd'
import type { ReportOverviewVO } from '../../../types/report'
import type { ReportThresholdContext, ReportThresholdItem } from '../../reportAlert/types'
import { formatMoney, formatPercent } from '../utils/reportFormatters'

interface Props {
  overview?: ReportOverviewVO
  thresholds?: ReportThresholdContext
}

export default function ReportInsightStrip({ overview, thresholds }: Props) {
  return (
    <div className="report-insights">
      {buildInsights(overview, thresholds).map((item) => (
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
      <span title={item.detail}>{item.detail}</span>
    </div>
  )
}

function buildInsights(overview?: ReportOverviewVO, context?: ReportThresholdContext): Insight[] {
  const settled = Number(overview?.settledAmount ?? 0)
  const unreceived = Number(overview?.unreceivedAmount ?? 0)
  const ratio = settled > 0 ? (unreceived / settled) * 100 : 0
  return [
    cashInsight(unreceived, Number(overview?.pendingSettleAmount ?? 0), ratio,
      threshold(context, 'UNRECEIVED_RATIO')),
    lossInsight(Number(overview?.lossRatio ?? 0), threshold(context, 'LOSS_RATIO')),
    processInsight(Number(overview?.sawAmount ?? 0), Number(overview?.rewindAmount ?? 0)),
  ]
}

function cashInsight(unreceived: number, pending: number, ratio: number,
                     rule?: ReportThresholdItem): Insight {
  const alerted = isTriggered(ratio, rule)
  return {
    detail: `已结算未收 ${formatMoney(unreceived)}，占已结算 ${formatPercent(ratio)}；待结算 ${formatMoney(pending)}${ruleText(rule)}`,
    tag: alerted ? '关注' : '回款',
    title: alerted ? '未收占比较高' : rule ? '回款压力可控' : '回款结构',
    type: alerted ? severityType(rule) : rule ? 'success' : 'info',
  }
}

function lossInsight(ratio: number, rule?: ReportThresholdItem): Insight {
  const alerted = isTriggered(ratio, rule)
  return {
    detail: `当前损耗率 ${formatPercent(ratio)}${ruleText(rule)}`,
    tag: alerted ? '异常' : '损耗',
    title: alerted ? '损耗率偏高' : rule ? '损耗处于阈值内' : '损耗结构',
    type: alerted ? severityType(rule) : 'info',
  }
}

function processInsight(saw: number, rewind: number): Insight {
  return {
    detail: `锯纸费 ${formatMoney(saw)}，复卷费 ${formatMoney(rewind)}`,
    tag: '结构',
    title: rewind > saw ? '复卷贡献更高' : '锯纸贡献更高',
    type: 'info',
  }
}

function tagColor(type: Insight['type']) {
  if (type === 'error') return 'error'
  if (type === 'warning') return 'warning'
  if (type === 'success') return 'success'
  return 'blue'
}

function threshold(context: ReportThresholdContext | undefined, signalCode: string) {
  return context?.thresholds.find((item) => item.signalCode === signalCode)
}

function isTriggered(value: number, rule?: ReportThresholdItem) {
  if (!rule) return false
  if (rule.comparisonOperator === 'GT') return value > rule.thresholdValue
  if (rule.comparisonOperator === 'GTE') return value >= rule.thresholdValue
  if (rule.comparisonOperator === 'LT') return value < rule.thresholdValue
  return value <= rule.thresholdValue
}

function ruleText(rule?: ReportThresholdItem) {
  if (!rule) return '；未配置生效阈值'
  return `；预警线 ${operatorText(rule.comparisonOperator)} ${formatPercent(rule.thresholdValue)}（${rule.scopeLabel}）`
}

function operatorText(operator: ReportThresholdItem['comparisonOperator']) {
  if (operator === 'GT') return '>'
  if (operator === 'GTE') return '≥'
  if (operator === 'LT') return '<'
  return '≤'
}

function severityType(rule?: ReportThresholdItem): Insight['type'] {
  return rule?.severity === 2 ? 'error' : 'warning'
}
