import { PRIORITY } from '../../../constants/processOrder'
import type { ProcessOrderDetailVO, ProcessStep } from '../../../types/processOrder'
import { formatOptionalTon as formatRawTon } from '../../../utils/numberFormatters'
import { buildDetailMetrics, formatTon } from '../orderDetailUtils'
import type { PrintSummaryItem } from './printPreviewTypes'

export function buildPrintSummary(detail: ProcessOrderDetailVO): PrintSummaryItem[] {
  const metrics = buildDetailMetrics(detail)
  const rewindWeight = (detail.steps ?? []).reduce((sum, step) => (
    step.stepType === 2 ? sum + (step.processWeight ?? 0) : sum
  ), 0)
  const finalCount = (detail.finishRolls ?? []).filter(isFinalFinishRoll).length
  return [
    { label: '原卷', value: `${metrics.rollCount} 卷 / ${formatTon(metrics.totalOriginalWeight)}` },
    { label: '最终成品', value: `${finalCount} 件 / ${formatTon(metrics.totalEstimateWeight)}` },
    { label: '锯纸刀数', value: `${detail.order.actualTotalKnife ?? sumKnifeCount(detail.steps)} 刀` },
    { label: '复卷吨位', value: formatRawTon(rewindWeight) },
    { label: '工序数', value: `${detail.steps?.length ?? 0} 道` },
    { label: '订单标记', value: PRIORITY[detail.order.priority ?? 1] ?? '普通' },
  ]
}

function isFinalFinishRoll(item: {
  isSpare?: number
  isRemain?: number
  rollNoStatus?: number
}) {
  return item.isSpare !== 1 && item.isRemain !== 1 && item.rollNoStatus !== 3
}

function sumKnifeCount(steps?: ProcessStep[]) {
  return (steps ?? []).reduce(
    (sum, step) => step.stepType === 1 ? sum + (step.knifeCount ?? 0) : sum,
    0,
  )
}
