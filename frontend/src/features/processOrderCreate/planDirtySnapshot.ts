import type { PlanPreviewVO, ProcessPlanDTO } from '../../types/processOrder'
import { configKeysForRoll } from './configuredPlanStatus'
import type { CreateOrderDraftSnapshot } from './hooks/useCreateOrderDraftState'

interface RecordEntry<T> {
  key: string
  present: boolean
  value?: T
}

export interface PlanDirtySnapshot {
  configuredIds: string[]
  localId: string
  plan: RecordEntry<ProcessPlanDTO>
  previews: Array<RecordEntry<PlanPreviewVO>>
}

export function capturePlanDirtySnapshot(
  state: CreateOrderDraftSnapshot,
  localId: string,
): PlanDirtySnapshot {
  const roll = state.rolls.find((item) => item.localId === localId)
  const keys = roll ? configKeysForRoll(roll) : [localId]
  return {
    configuredIds: keys.filter((key) => state.configuredPlanIds.includes(key)),
    localId,
    plan: recordEntry(state.plans, localId),
    previews: keys.map((key) => recordEntry(state.previews, key)),
  }
}

export function restorePlanDirtySnapshots(
  state: CreateOrderDraftSnapshot,
  snapshots: Iterable<PlanDirtySnapshot>,
): CreateOrderDraftSnapshot {
  const plans = { ...state.plans }
  const previews = { ...state.previews }
  const configuredIds = new Set(state.configuredPlanIds)
  for (const snapshot of snapshots) {
    restoreRecordEntry(plans, snapshot.plan)
    snapshot.previews.forEach((entry) => restoreRecordEntry(previews, entry))
    snapshot.previews.forEach((entry) => configuredIds.delete(entry.key))
    snapshot.configuredIds.forEach((id) => configuredIds.add(id))
  }
  return { ...state, configuredPlanIds: [...configuredIds], plans, previews }
}

function recordEntry<T>(record: Record<string, T>, key: string): RecordEntry<T> {
  return { key, present: Object.hasOwn(record, key), value: record[key] }
}

function restoreRecordEntry<T>(record: Record<string, T>, entry: RecordEntry<T>) {
  if (!entry.present || entry.value === undefined) delete record[entry.key]
  else record[entry.key] = entry.value
}
