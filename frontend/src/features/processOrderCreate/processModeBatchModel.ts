import type { Machine } from '../../types/machine'
import { processModeRequiresMain } from '../../constants/processOrder'
import { applyDefaultMachineToRoll } from './machineDefaults'
import type { RollDraft } from './types'

interface ApplyProcessModeBatchOptions {
  checkedIds: string[]
  machines: Machine[]
  mainStepType?: number
  processMode: number
  rolls: RollDraft[]
}

export function applyProcessModeBatch(options: ApplyProcessModeBatchOptions): RollDraft[] {
  const checkedIds = new Set(options.checkedIds)
  const requiresMain = processModeRequiresMain(options.processMode)
  return options.rolls.map((roll) => {
    if (!checkedIds.has(roll.localId)) return roll
    return applyDefaultMachineToRoll({
      ...roll,
      processMode: options.processMode,
      mainStepType: requiresMain ? options.mainStepType ?? 2 : undefined,
      machineUuid: requiresMain ? roll.machineUuid : undefined,
    }, options.machines)
  })
}
