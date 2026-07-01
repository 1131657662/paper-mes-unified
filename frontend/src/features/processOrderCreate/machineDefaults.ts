import type { Machine } from '../../types/machine'
import type { ProcessPlanDTO } from '../../types/processOrder'
import type { RollDraft } from './types'

const STATUS_ENABLED = 1
const MACHINE_TYPE_GENERAL = 3

export function applyDefaultMachinesToRolls(rolls: RollDraft[], machines: Machine[]): RollDraft[] {
  return rolls.map((roll) => applyDefaultMachineToRoll(roll, machines))
}

export function applyDefaultMachineToRoll(roll: RollDraft, machines: Machine[]): RollDraft {
  const machineUuid = resolveMachineUuid(roll.mainStepType, roll.machineUuid, machines)
  return roll.machineUuid === machineUuid ? roll : { ...roll, machineUuid }
}

export function applyDefaultMachineToPlan(plan: ProcessPlanDTO, machines: Machine[]): ProcessPlanDTO {
  const machineUuid = resolveMachineUuid(plan.mainStepType, plan.machineUuid, machines)
  return plan.machineUuid === machineUuid ? plan : { ...plan, machineUuid }
}

export function machinesForStep(mainStepType: number | undefined, machines: Machine[]): Machine[] {
  if (!mainStepType) return []
  return machines
    .filter((machine) => machine.status === STATUS_ENABLED)
    .filter((machine) => machine.machineType === mainStepType || machine.machineType === MACHINE_TYPE_GENERAL)
}

function resolveMachineUuid(
  mainStepType: number | undefined,
  currentMachineUuid: string | undefined,
  machines: Machine[],
): string | undefined {
  const candidates = machinesForStep(mainStepType, machines)
  if (currentMachineUuid && candidates.some((machine) => machine.uuid === currentMachineUuid)) {
    return currentMachineUuid
  }
  return candidates.length === 1 ? candidates[0]?.uuid : undefined
}
