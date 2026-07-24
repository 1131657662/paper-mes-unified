import type { Machine } from '../../types/machine'
import type { ProcessPlanDTO } from '../../types/processOrder'
import type { RollDraft } from './types'

const STATUS_ENABLED = 1

export interface MachineContext {
  diameter?: number
  weight?: number
  width?: number
}

export function applyDefaultMachinesToRolls(rolls: RollDraft[], machines: Machine[]): RollDraft[] {
  return rolls.map((roll) => applyDefaultMachineToRoll(roll, machines))
}

export function applyDefaultMachineToRoll(roll: RollDraft, machines: Machine[]): RollDraft {
  const machineUuid = suggestedMachineUuid({
    mainStepType: roll.mainStepType,
    currentMachineUuid: roll.machineUuid,
    machines,
    context: rollContext(roll),
  })
  return roll.machineUuid === machineUuid ? roll : { ...roll, machineUuid }
}

export function applyDefaultMachineToPlan(
  plan: ProcessPlanDTO,
  machines: Machine[],
  roll?: RollDraft,
): ProcessPlanDTO {
  const machineUuid = suggestedMachineUuid({
    mainStepType: plan.mainStepType,
    currentMachineUuid: plan.machineUuid,
    machines,
    context: roll ? rollContext(roll) : undefined,
  })
  return plan.machineUuid === machineUuid ? plan : { ...plan, machineUuid }
}

export function machinesForStep(
  mainStepType: number | undefined,
  machines: Machine[],
  context?: MachineContext,
): Machine[] {
  if (!mainStepType) return []
  return machines
    .filter((machine) => machine.status === STATUS_ENABLED)
    .filter((machine) => supportsStep(machine, mainStepType, context))
    .sort((left, right) => candidateOrder(left, right, mainStepType))
}

export function suggestedMachineUuid(options: {
  mainStepType?: number
  currentMachineUuid?: string
  machines: Machine[]
  context?: MachineContext
}): string | undefined {
  const { mainStepType, currentMachineUuid, machines, context } = options
  const candidates = machinesForStep(mainStepType, machines, context)
  if (currentMachineUuid && candidates.some((machine) => machine.uuid === currentMachineUuid)) {
    return currentMachineUuid
  }
  const preferred = candidates.filter((machine) => capability(machine, mainStepType)?.defaultCapability)
  if (preferred.length === 1) return preferred[0]?.uuid
  return candidates.length === 1 ? candidates[0]?.uuid : undefined
}

function supportsStep(machine: Machine, stepType: number, context?: MachineContext) {
  const current = capability(machine, stepType)
  if (!current) return machine.capabilities?.length ? false : legacySupports(machine, stepType)
  if (context?.width != null && current.minWidth != null && context.width < current.minWidth) return false
  if (context?.width != null && current.maxWidth != null && context.width > current.maxWidth) return false
  if (context?.weight != null && current.maxRollWeight != null && context.weight > current.maxRollWeight) return false
  return !(context?.diameter != null && current.maxDiameter != null && context.diameter > current.maxDiameter)
}

function capability(machine: Machine, stepType?: number) {
  return machine.capabilities?.find((item) => item.stepType === stepType)
}

function legacySupports(machine: Machine, stepType: number) {
  return machine.machineType === stepType || machine.machineType === 3
}

function candidateOrder(left: Machine, right: Machine, stepType: number) {
  const leftCapability = capability(left, stepType)
  const rightCapability = capability(right, stepType)
  return Number(rightCapability?.defaultCapability) - Number(leftCapability?.defaultCapability)
    || (leftCapability?.priority ?? 100) - (rightCapability?.priority ?? 100)
    || left.machineName.localeCompare(right.machineName, 'zh-CN')
}

function rollContext(roll: RollDraft): MachineContext {
  return {
    diameter: roll.originalDiameter,
    weight: Number(roll.rollWeight ?? 0),
    width: roll.originalWidth,
  }
}
