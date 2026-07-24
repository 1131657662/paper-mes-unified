import { isDeliverableProductionFinish } from '../../../components/processOrder/shared/detailHelpers'
import type {
  FinishProductionVO,
  ProcessStep,
  RollProductionVO,
  StageOutputVO,
} from '../../../types/processOrder'
import { formatMm } from '../../../utils/numberFormatters'
import { formatProductionKg } from '../orderDetailUtils'
import { isFinalOutput, isTrimOutput, stageSource } from './printPreviewOutputs'

export function stageRequirement(
  production: RollProductionVO,
  step: ProcessStep | undefined,
  outputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
) {
  const stepType = step?.stepType ?? outputs[0]?.sourceStepType ?? production.mainStepType
  if (stepType === 1) return sawRequirement(production, step, outputs, allOutputs)
  if (stepType === 2) return rewindRequirement(outputs)
  return '按本阶段产物规格加工，完成后填写实际重量和异常说明。'
}

export function singleStageRequirement(
  production: RollProductionVO,
  outputs: FinishProductionVO[],
  step?: ProcessStep,
) {
  const stepType = step?.stepType ?? production.mainStepType
  const deliverableOutputs = outputs.filter(isDeliverableProductionFinish)
  if (stepType === 3 || stepType === 4) {
    const action = stepType === 3 ? '剥除破损、整理并重新缠绕' : '按要求重新包膜或包装'
    return `${action}；不改变品名、克重和门幅；完工后逐件填写实际重量及损耗。`
  }
  if (stepType === 1) {
    return sawText({
      production,
      sourceWidth: production.originalWidth,
      sourceWeight: (production.rollWeight ?? 0) * (production.pieceNum ?? 1),
      knifeCount: step?.knifeCount,
      outputs: deliverableOutputs.map((item) => ({
        width: item.finishWidth,
        status: 'final',
      })),
    })
  }
  return rewindText({
    outputs: deliverableOutputs.map((item) => ({ width: item.finishWidth })),
  })
}

function sawRequirement(
  production: RollProductionVO,
  step: ProcessStep | undefined,
  outputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
) {
  const source = stageSource(production, outputs, allOutputs)
  return sawText({
    production,
    sourceWidth: source.width,
    sourceWeight: source.weight,
    knifeCount: step?.knifeCount,
    outputs: outputs.filter((item) => !isTrimOutput(item)).map((item) => ({
      width: item.finishWidth,
      status: isFinalOutput(item) ? 'final' : 'next',
    })),
  })
}

function rewindRequirement(outputs: StageOutputVO[]) {
  return rewindText({
    outputs: outputs
      .filter((item) => !isTrimOutput(item))
      .map((item) => ({ width: item.finishWidth })),
  })
}

interface SawTextOptions {
  production: RollProductionVO
  sourceWidth?: number
  sourceWeight: number
  knifeCount?: number
  outputs: Array<{ width?: number; status: 'next' | 'final' }>
}

function sawText(options: SawTextOptions) {
  const usedWidth = options.outputs.reduce((sum, item) => sum + (item.width ?? 0), 0)
  const trimWidth = options.sourceWidth == null
    ? 0
    : Math.max(0, options.sourceWidth - usedWidth)
  const layout = groupedWidths(options.outputs)
  const sourceWidth = options.sourceWidth
    ? `原幅 ${formatMm(options.sourceWidth)}`
    : '按来源门幅'
  const derivedKnifeCount = Math.max(0, options.outputs.length - 1) + (trimWidth > 0 ? 1 : 0)
  const knifeCount = options.knifeCount != null && options.knifeCount > 0
    ? options.knifeCount
    : derivedKnifeCount
  const knife = knifeCount > 0 ? `共 ${knifeCount} 刀` : '刀数按排布执行'
  const trim = trimText(options, trimWidth)
  return `${sourceWidth}；产出 ${layout || '按产物表规格'}；${trim}；${knife}。`
}

function trimText(options: SawTextOptions, trimWidth: number) {
  if (trimWidth <= 0) return '无切边'
  const weight = options.sourceWeight > 0
    ? `，约 ${formatProductionKg(
      options.sourceWeight * trimWidth / (options.sourceWidth ?? 1),
      options.production,
    )}`
    : ''
  return `切边 ${formatMm(trimWidth)}${weight}`
}

function rewindText(options: { outputs: Array<{ width?: number }> }) {
  return groupedRewindOutputs(options.outputs) || '复卷门幅按产物表执行。'
}

function groupedWidths(outputs: Array<{ width?: number; status: 'next' | 'final' }>) {
  const groups = new Map<string, { text: string; count: number; status: 'next' | 'final' }>()
  for (const output of outputs) {
    const text = output.width ? formatMm(output.width) : '未填门幅'
    const key = `${text}-${output.status}`
    const existing = groups.get(key)
    groups.set(key, { text, status: output.status, count: (existing?.count ?? 0) + 1 })
  }
  return Array.from(groups.values()).map((item) => {
    const nextStage = item.status === 'next' ? '（进下道）' : ''
    return `${item.text} ×${item.count} 件${nextStage}`
  }).join(' + ')
}

function groupedRewindOutputs(outputs: Array<{ width?: number }>) {
  const groups = new Map<string, { text: string; count: number }>()
  for (const output of [...outputs].sort((a, b) => (a.width ?? 0) - (b.width ?? 0))) {
    const text = output.width ? formatMm(output.width) : '沿用门幅'
    groups.set(text, { text, count: (groups.get(text)?.count ?? 0) + 1 })
  }
  const items = Array.from(groups.values())
  const totalCount = items.reduce((sum, item) => sum + item.count, 0)
  const onlyItem = items[0]
  if (items.length === 1 && onlyItem) {
    return `加工门幅 ${onlyItem.text}，产出 ${totalCount} 件。`
  }
  const widths = items.map((item) => `${item.text} ×${item.count} 件`).join(' + ')
  return `加工门幅 ${widths}，共产出 ${totalCount} 件。`
}
