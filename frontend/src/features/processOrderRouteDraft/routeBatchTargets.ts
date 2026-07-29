import type { OriginalRoll } from '../../types/processOrder'

const STANDARD_PROCESS_MODE = 1

export function routeBatchTargets(rolls: OriginalRoll[], current: OriginalRoll): OriginalRoll[] {
  return rolls.filter((roll) => roll.uuid !== current.uuid
    && roll.processMode === STANDARD_PROCESS_MODE
    && roll.mainStepType === current.mainStepType
    && roll.paperName === current.paperName
    && roll.gramWeight === current.gramWeight
    && roll.originalWidth === current.originalWidth)
}
