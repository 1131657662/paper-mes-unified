import type { ReportDimension, ReportDimensionVO, ReportMetricItemVO, ReportQuery } from '../../types/report'

export const explorerDimensions: Array<{ label: string; value: ReportDimension }> = [
  { label: '月份', value: 'month' },
  { label: '客户', value: 'customer' },
  { label: '产品', value: 'paper' },
  { label: '工艺', value: 'process' },
  { label: '机台', value: 'machine' },
  { label: '开票状态', value: 'invoice' },
  { label: '结算方式', value: 'settleType' },
  { label: '单据状态', value: 'status' },
]

export const explorerMetricDefaults = [
  'order_count', 'finish_weight_kg', 'loss_ratio_pct', 'total_amount', 'received_amount',
]

const metricFieldMap: Record<string, keyof ReportDimensionVO> = {
  order_count: 'orderCount', original_roll_count: 'originalRollCount', finish_roll_count: 'finishRollCount',
  original_weight_kg: 'originalWeight', finish_weight_kg: 'finishWeight', loss_weight_kg: 'lossWeight',
  loss_ratio_pct: 'lossRatio', knife_count: 'knifeCount', saw_amount: 'sawAmount', rewind_amount: 'rewindAmount',
  process_amount: 'processAmount', extra_amount: 'extraAmount', total_amount: 'totalAmount',
  settled_amount: 'settledAmount', pending_settle_amount: 'pendingSettleAmount', received_amount: 'receivedAmount',
  cash_received_amount: 'cashReceivedAmount', scrap_offset_amount: 'scrapOffsetAmount', unreceived_amount: 'unreceivedAmount',
}

export function metricField(metricCode: string): keyof ReportDimensionVO | undefined {
  return metricFieldMap[metricCode]
}

export function metricLabel(metric: ReportMetricItemVO): string {
  return `${metric.metricName} (${metric.unitCode})`
}

export function selectedMetricItems(metrics: ReportMetricItemVO[], selectedCodes: string[]) {
  return selectedCodes
    .map((code) => metrics.find((item) => item.metricCode === code))
    .filter(Boolean) as ReportMetricItemVO[]
}

export function drillQuery(dimension: ReportDimension, key: string, query: ReportQuery): ReportQuery | undefined {
  if (!key || key === 'none' || dimension === 'month') return undefined
  const next = { ...query }
  if (dimension === 'customer') next.customerUuid = key
  if (dimension === 'paper') next.paperName = key
  if (dimension === 'machine') next.machineUuid = key
  if (dimension === 'process') next.processStepType = processStepType(key)
  if (dimension === 'invoice') next.isInvoice = finiteInteger(key)
  if (dimension === 'settleType') next.settleType = finiteInteger(key)
  if (dimension === 'status') next.orderStatus = finiteInteger(key)
  return next
}

function processStepType(key: string): number | undefined {
  if (key === 'saw') return 1
  if (key === 'rewind') return 2
  const match = /^step-([34])$/.exec(key)
  return match ? Number(match[1]) : undefined
}

function finiteInteger(key: string): number | undefined {
  const value = Number(key)
  return Number.isInteger(value) ? value : undefined
}
