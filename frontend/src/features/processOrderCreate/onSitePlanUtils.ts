import type { FinishConfigSpecDTO, ProcessPlanDTO } from '../../types/processOrder'

export function toOnSitePlan(plan: ProcessPlanDTO, count = onSiteCount(plan.finishSpecs)): ProcessPlanDTO {
  return {
    ...plan,
    rewindMode: plan.mainStepType === 2 ? plan.rewindMode ?? 2 : plan.rewindMode,
    finishSpecs: [{ itemType: 'FINISH', finishWidth: 0, count, estimateWeight: 0 }],
    segments: [],
    knifeCount: plan.mainStepType === 1 ? 0 : plan.knifeCount,
  }
}

export function onSiteCount(specs?: FinishConfigSpecDTO[]): number {
  const count = (specs ?? [])
    .filter((spec) => spec.itemType !== 'TRIM')
    .reduce((sum, spec) => sum + Number(spec.count ?? 1), 0)
  return Math.max(1, count)
}
