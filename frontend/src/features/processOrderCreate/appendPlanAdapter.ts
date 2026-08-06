import type {
  FinishConfigSaveDTO,
  PlanPreviewVO,
  ProcessOrderAppendRollVO,
  ProcessPlanDTO,
  RewindSegmentPlanDTO,
} from '../../types/processOrder'
import type { RollDraft } from './types'

export function toAppendConfig(plan: ProcessPlanDTO): FinishConfigSaveDTO {
  return {
    processMode: plan.processMode,
    mainStepType: plan.mainStepType,
    machineUuid: plan.machineUuid,
    spareCount: plan.spareCount,
    rewindMode: plan.rewindMode,
    knifeCount: plan.knifeCount,
    unitPrice: plan.unitPrice,
    widthDifferencePolicy: plan.widthDifferencePolicy,
    finishSpecs: plan.finishSpecs,
    rewindSegments: plan.segments?.map(toRewindSegment),
  }
}

export function fromAppendConfig(roll: ProcessOrderAppendRollVO): ProcessPlanDTO | undefined {
  const config = roll.config
  if (!config) return undefined
  return {
    processMode: config.processMode,
    mainStepType: config.mainStepType,
    machineUuid: config.machineUuid,
    spareCount: config.spareCount,
    rewindMode: config.rewindMode,
    knifeCount: config.knifeCount,
    unitPrice: config.unitPrice,
    widthDifferencePolicy: config.widthDifferencePolicy,
    finishSpecs: config.finishSpecs,
    segments: config.rewindSegments?.map((segment) => ({
      segmentSort: segment.segmentSort,
      segmentRatio: segment.segmentRatio,
      targetDiameter: segment.targetDiameter,
      finishCoreDiameter: segment.finishCoreDiameter,
      repeatCount: segment.repeatCount,
      sources: segment.sources?.map((source) => ({
        originalUuid: source.originalUuid,
        shareRatio: source.shareRatio,
        consumeRatio: source.consumeRatio,
      })),
      layoutItems: segment.layoutItems?.map((item) => ({
        width: item.width,
        quantity: item.quantity,
        itemType: item.itemType,
        customerPaperName: item.customerPaperName,
        customerGramWeight: item.customerGramWeight,
        customerFinishWidth: item.customerFinishWidth,
        customerSpecOverrideReason: item.customerSpecOverrideReason,
        layers: item.layers,
      })),
    })),
  }
}

export function appendPreview(roll: ProcessOrderAppendRollVO): PlanPreviewVO | undefined {
  return roll.preview
}

function toRewindSegment(segment: RewindSegmentPlanDTO) {
  return {
    segmentSort: segment.segmentSort,
    segmentRatio: segment.segmentRatio,
    targetDiameter: segment.targetDiameter,
    finishCoreDiameter: segment.finishCoreDiameter,
    repeatCount: segment.repeatCount,
    sources: segment.sources?.map((source) => ({
      originalUuid: source.originalUuid,
      shareRatio: source.shareRatio,
      consumeRatio: source.consumeRatio,
    })),
    layoutItems: segment.layoutItems?.map((item) => ({
      width: item.width,
      quantity: item.quantity,
      itemType: item.itemType,
      customerPaperName: item.customerPaperName,
      customerGramWeight: item.customerGramWeight,
      customerFinishWidth: item.customerFinishWidth,
      customerSpecOverrideReason: item.customerSpecOverrideReason,
      layers: item.layers,
    })),
  }
}

export function localAppendPreview(roll: RollDraft, plan: ProcessPlanDTO): PlanPreviewVO {
  const finishCount = plan.finishSpecs?.reduce((sum, spec) => sum + Number(spec.count || 0), 0)
    ?? plan.segments?.reduce((sum, segment) => sum + (segment.layoutItems ?? [])
      .filter((item) => item.itemType !== 'TRIM')
      .reduce((itemSum, item) => itemSum + Number(item.quantity || 1), 0), 0)
    ?? 0
  return {
    originalUuid: roll.uuid,
    processMode: plan.processMode,
    mainStepType: plan.mainStepType,
    rewindMode: plan.rewindMode,
    finishCount,
    trimCount: 0,
    spareCount: plan.spareCount ?? 0,
    ready: true,
    summary: `已确认配置，预计生成 ${finishCount} 个成品号`,
  }
}
