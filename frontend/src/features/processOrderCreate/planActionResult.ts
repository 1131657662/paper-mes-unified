import { message } from 'antd'
import type { Dispatch, SetStateAction } from 'react'
import type { PlanPreviewVO, ProcessPlanDTO } from '../../types/processOrder'
import { configKeysForRoll, reconcileConfiguredPlanIds } from './configuredPlanStatus'
import type { RollDraft } from './types'

export interface PlanCommandState {
  configuredPlanIds: string[]
  getDraftVersion: () => number
  orderUuid?: string
  rolls: RollDraft[]
  setConfiguredPlanIds: Dispatch<SetStateAction<string[]>>
  setDraftVersion: Dispatch<SetStateAction<number>>
  setPlans: Dispatch<SetStateAction<Record<string, ProcessPlanDTO>>>
  setPreviews: Dispatch<SetStateAction<Record<string, PlanPreviewVO>>>
  setRolls: Dispatch<SetStateAction<RollDraft[]>>
}

export function applySavedPlans(
  state: PlanCommandState,
  rolls: RollDraft[],
  plans: Record<string, ProcessPlanDTO>,
  previews: Record<string, PlanPreviewVO>,
) {
  const staleKeys = new Set(rolls.flatMap(configKeysForRoll))
  state.setPlans((previous) => ({ ...previous, ...plans }))
  state.setPreviews((previous) => ({ ...omitKeys(previous, staleKeys), ...previews }))
  state.setConfiguredPlanIds((previous) => reconcileConfiguredPlanIds(
    previous.filter((id) => !staleKeys.has(id)),
    rolls.map((roll) => ({ localId: roll.localId, preview: previews[roll.localId] })),
  ))
  state.setRolls((previous) => previous.map((roll) => (
    plans[roll.localId] ? { ...roll, machineUuid: plans[roll.localId]?.machineUuid } : roll
  )))
}

export function notifySingleSave(preview: PlanPreviewVO) {
  if (preview.ready) message.success('方案已保存')
  else message.warning(preview.errors?.join('；') || '方案已保存，但仍未完成配置')
}

export function notifyBatchSave(
  requested: number,
  applied: number,
  previews: Record<string, PlanPreviewVO>,
) {
  if (applied < requested) {
    message.warning(`保存期间有 ${requested - applied} 卷出现新修改，已保留为待处理`)
    return
  }
  const ready = Object.values(previews).filter((preview) => preview.ready).length
  if (ready === requested) message.success(`已应用到 ${requested} 卷母卷`)
  else message.warning(`已保存 ${requested} 卷，其中 ${requested - ready} 卷仍需检查`)
}

export function pickPreviews(
  previews: Record<string, PlanPreviewVO>,
  rolls: RollDraft[],
) {
  return rolls.reduce<Record<string, PlanPreviewVO>>((result, roll) => {
    const preview = previews[roll.localId]
    if (preview) result[roll.localId] = preview
    return result
  }, {})
}

export function omitKeys<T>(record: Record<string, T>, omitted: Set<string>): Record<string, T> {
  return Object.fromEntries(Object.entries(record).filter(([key]) => !omitted.has(key)))
}
