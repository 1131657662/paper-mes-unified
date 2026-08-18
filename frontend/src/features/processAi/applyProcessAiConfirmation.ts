import type { PlanPreviewVO, ProcessPlanDTO } from '../../types/processOrder'
import type { RollDraft } from '../processOrderCreate/types'
import type {
  ProcessAiConfirmResponse,
  ProcessAiCompiledPlan,
  ProcessAiPackagingCandidate,
  ProcessAiPackagingDraft,
} from './types'

interface ApplyInput {
  rolls: RollDraft[]
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  configuredPlanIds: string[]
  confirmation: ProcessAiConfirmResponse
}

export interface AppliedProcessAiDraft {
  rolls: RollDraft[]
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  configuredPlanIds: string[]
  packagingDrafts: ProcessAiPackagingDraft[]
  updatedLocalIds: string[]
}

export function applyProcessAiConfirmation(input: ApplyInput): AppliedProcessAiDraft {
  const accepted = new Set(input.confirmation.acceptedFieldPaths)
  const candidates = Object.values(input.confirmation.plans)
    .filter((candidate) => hasAccepted(accepted, base(candidate)))
  const byOriginalUuid = new Map(candidates.map((candidate) => [candidate.originalUuid, candidate]))
  const updatedLocalIds = input.rolls.filter((roll) => roll.uuid && byOriginalUuid.has(roll.uuid))
    .map((roll) => roll.localId)
  const rolls = input.rolls.map((roll) => applyRollCandidate(roll, byOriginalUuid))
  const packagingDrafts = input.confirmation.packagingCandidates
    .filter((candidate) => accepted.has(`${base(candidate)}/ancillaryRequirements/packaging`))
    .map((candidate) => packagingDraftFromCandidate(input.confirmation.parseId, candidate))
  const plans = { ...input.plans }
  const previews = { ...input.previews }
  const configuredPlanIds = new Set(input.configuredPlanIds)
  for (const roll of rolls) {
    const candidate = roll.uuid ? byOriginalUuid.get(roll.uuid) : undefined
    if (!candidate) continue
    plans[roll.localId] = structuredClone(candidate.plan)
    previews[roll.localId] = structuredClone(candidate.preview)
    if (roll.uuid && roll.uuid !== roll.localId) delete previews[roll.uuid]
    configuredPlanIds.add(roll.localId)
  }
  return {
    rolls,
    plans,
    previews,
    configuredPlanIds: [...configuredPlanIds],
    packagingDrafts,
    updatedLocalIds,
  }
}

function applyRollCandidate(
  roll: RollDraft,
  candidates: Map<string, ProcessAiCompiledPlan>,
) {
  const candidate = roll.uuid ? candidates.get(roll.uuid) : undefined
  if (!candidate) return roll
  return {
    ...roll,
    processMode: candidate.plan.processMode,
    mainStepType: candidate.plan.mainStepType,
    machineUuid: candidate.plan.machineUuid ?? roll.machineUuid,
  }
}

function hasAccepted(accepted: Set<string>, prefix: string) {
  return [...accepted].some((path) => path.startsWith(`${prefix}/`))
}

function base(candidate: { ownerRollRef: string }) {
  return `/assignments/${candidate.ownerRollRef}`
}

export function packagingDraftFromCandidate(
  parseId: string,
  candidate: ProcessAiPackagingCandidate,
): ProcessAiPackagingDraft {
  return {
    parseId,
    ownerRollRef: candidate.ownerRollRef,
    values: {
      originalUuid: candidate.originalUuid,
      stepType: candidate.stepType,
      stepName: candidate.stepName,
      isMain: 0,
      billingBasis: candidate.billingBasis,
      serviceQuantity: candidate.serviceQuantity,
      billingMode: candidate.billingMode,
      billingAmount: candidate.billingAmount,
      unitPrice: candidate.unitPrice,
      remark: candidate.remark,
      aiParseId: parseId,
      aiOwnerRollRef: candidate.ownerRollRef,
    },
  }
}
