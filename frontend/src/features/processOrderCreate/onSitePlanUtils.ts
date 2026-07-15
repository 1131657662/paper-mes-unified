import type { ProcessPlanDTO } from '../../types/processOrder'

export function toOnSitePlan(plan: ProcessPlanDTO): ProcessPlanDTO {
  return {
    ...plan,
    rewindMode: undefined,
    spareCount: 0,
    finishSpecs: [],
    segments: [],
    knifeCount: undefined,
  }
}
