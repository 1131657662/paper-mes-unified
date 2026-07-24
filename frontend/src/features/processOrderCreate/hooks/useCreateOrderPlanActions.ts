import { message } from 'antd'
import type { Machine } from '../../../types/machine'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import {
  plansFromBatch,
  previewsFromBatch,
} from '../createOrderState'
import {
  type DefaultPlanOptions,
} from '../draftMappers'
import { normalizeLayeredRewindPlan } from '../rewindLayerPlanUtils'
import { prepareBatchPlan, prepareSingleRollPlan } from '../prepareProcessPlan'
import type { RollDraft } from '../types'
import type { CreateOrderDraftState } from './useCreateOrderDraftState'
import { usePreviewPlan } from './usePreviewPlan'
import { useSavePlan } from './useSavePlan'
import { useSavePlanBatch } from './useSavePlanBatch'

interface UseCreateOrderPlanActionsOptions {
  defaultPlanOptions: DefaultPlanOptions
  machines: Machine[]
  state: CreateOrderDraftState
}

export function useCreateOrderPlanActions(options: UseCreateOrderPlanActionsOptions) {
  const { defaultPlanOptions, machines, state } = options
  const { mutateAsync: previewPlan, isPending: previewingPlan } = usePreviewPlan()
  const { mutateAsync: savePlan, isPending: savingPlan } = useSavePlan()
  const { mutateAsync: savePlanBatch, isPending: savingPlanBatch } = useSavePlanBatch()

  const saveRollPlan = async (roll: RollDraft, plan: ProcessPlanDTO, expectedVersion: number) => {
    if (!state.orderUuid || !roll.uuid) return false
    const nextPlan = prepareSingleRollPlan({ defaultPlanOptions, machines, plan, roll })
    const preview = await savePlan({
      orderUuid: state.orderUuid,
      rollUuid: roll.uuid,
      request: { expectedVersion, originalUuid: roll.uuid, plan: nextPlan },
    })
    state.setPlans((previous) => ({ ...previous, [roll.localId]: nextPlan }))
    state.setPreviews((previous) => ({ ...previous, [roll.localId]: preview }))
    state.setConfiguredPlanIds((previous) => [...new Set([...previous, roll.localId])])
    return true
  }

  const handlePreviewPlan = async (roll: RollDraft, plan: ProcessPlanDTO) => {
    if (!state.orderUuid || !roll.uuid) return
    const nextPlan = prepareSingleRollPlan({ defaultPlanOptions, machines, plan, roll })
    const preview = await previewPlan({
      orderUuid: state.orderUuid,
      request: {
        expectedVersion: state.draftVersion,
        originalUuid: roll.uuid,
        plan: nextPlan,
      },
    })
    state.setPlans((previous) => ({ ...previous, [roll.localId]: nextPlan }))
    state.setPreviews((previous) => ({ ...previous, [roll.localId]: preview }))
  }

  const handleSavePlan = async (roll: RollDraft, plan: ProcessPlanDTO) => {
    if (!await saveRollPlan(roll, plan, state.draftVersion)) return false
    state.setDraftVersion((version) => version + 1)
    message.success('方案已保存')
    return true
  }

  const handleSavePlanBatch = async (targetRolls: RollDraft[], plan: ProcessPlanDTO) => {
    if (!state.orderUuid) return false
    const savedRolls = targetRolls.filter((roll) => roll.uuid)
    const firstSavedRoll = savedRolls[0]
    if (!firstSavedRoll) {
      message.warning('请选择已保存的母卷')
      return false
    }
    const batchPlan = prepareBatchPlan({ defaultPlanOptions, machines, plan, roll: firstSavedRoll })
    const result = await savePlanBatch({
      orderUuid: state.orderUuid,
      dto: {
        expectedVersion: state.draftVersion,
        originalUuids: savedRolls.map((roll) => roll.uuid!),
        plan: batchPlan,
      },
    })
    state.setPlans((previous) => ({ ...previous, ...plansFromBatch(savedRolls, batchPlan) }))
    state.setPreviews((previous) => ({ ...previous, ...previewsFromBatch(savedRolls, result) }))
    state.setConfiguredPlanIds((previous) => [...new Set([...previous, ...savedRolls.map((roll) => roll.localId)])])
    state.setDraftVersion((version) => version + 1)
    message.success(`已应用到 ${savedRolls.length} 卷母卷`)
    return true
  }

  return {
    savingWorkbench: savingPlan || savingPlanBatch || previewingPlan,
    handlePlanChange: (localId: string, plan: ProcessPlanDTO) => {
      const roll = state.rolls.find((item) => item.localId === localId)
      const nextPlan = roll ? normalizeLayeredRewindPlan(plan, roll) : plan
      state.setPlans((previous) => ({ ...previous, [localId]: nextPlan }))
      state.setConfiguredPlanIds((previous) => previous.filter((id) => id !== localId))
    },
    handlePreviewPlan,
    handleSavePlan,
    handleSavePlanBatch,
  }
}
