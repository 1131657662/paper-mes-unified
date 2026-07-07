import type { ProcessRoutePreviewVO } from '../../../types/processOrder'
import { formatKg } from '../../../utils/numberFormatters'

export function routeFinalCount(preview?: ProcessRoutePreviewVO) {
  return routeFinalOutputs(preview).length
}

export function routePreviewSummary(preview: ProcessRoutePreviewVO) {
  const finals = routeFinalOutputs(preview)
  const weight = finals.reduce((sum, item) => sum + Number(item.estimateWeight ?? 0), 0)
  return `链式 ${preview.stages?.length ?? 0} 道，最终 ${finals.length} 件 / ${formatKg(weight)}`
}

export function routeFinalOutputs(preview?: ProcessRoutePreviewVO) {
  return (preview?.outputs ?? []).filter((item) => item.isRemain !== 1 && !item.consumedByNextStage)
}
