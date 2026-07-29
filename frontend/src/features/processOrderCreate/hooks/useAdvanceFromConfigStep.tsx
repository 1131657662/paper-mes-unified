import { message } from 'antd'
import type { Machine } from '../../../types/machine'
import { pendingConfigurationRolls } from '../autoFinishConfigModel'
import { confirmAutoFinishConfigs, type AutoFinishConfigItem } from '../components/AutoFinishConfigConfirm'
import { defaultPlanForRoll, type DefaultPlanOptions } from '../draftMappers'
import { prepareSingleRollPlan } from '../prepareProcessPlan'
import type { PlanPreviewVO, ProcessPlanDTO } from '../../../types/processOrder'
import type { CreateOrderDraftState } from './useCreateOrderDraftState'
import type { MoveToCreateOrderStep } from './useCreateOrderStepNavigation'
import { useSavePlanItemsBatch } from './useSavePlanItemsBatch'
import { reconcileConfiguredPlanIds } from '../configuredPlanStatus'
import type { VersionedWriteResult } from '../draftWriteTypes'

interface Options {
  autoFinishConfigEnabled: boolean
  defaultPlanOptions: DefaultPlanOptions
  machines: Machine[]
  moveToStep: MoveToCreateOrderStep
  state: CreateOrderDraftState
}

export function useAdvanceFromConfigStep(options: Options) {
  const { mutateAsync: savePlanItemsBatch, isPending: savingAutoPlansBatch } = useSavePlanItemsBatch()

  const advance = async () => {
    const pending = pendingConfigurationRolls(options.state)
    if (!pending.length) {
      await moveToPreview(options.state, options.moveToStep)
      return true
    }
    if (!options.autoFinishConfigEnabled) {
      options.state.setSelectedId(pending[0]?.localId)
      message.warning(`仍有 ${pending.length} 卷加工方案未保存，请保存本卷方案或批量应用后再继续`)
      return false
    }

    const items = pending.map((roll) => ({
      roll,
      plan: prepareSingleRollPlan({
        defaultPlanOptions: options.defaultPlanOptions,
        machines: options.machines,
        plan: options.state.plans[roll.localId] ?? defaultPlanForRoll(roll, options.defaultPlanOptions),
        roll,
      }),
    }))
    if (!await confirmAutoFinishConfigs(items)) return false
    const result = await persistPlans(items, options, savePlanItemsBatch)
    if (result.blockedRolls.length) {
      options.state.setSelectedId(result.blockedRolls[0]?.localId)
      message.warning(`仍有 ${result.blockedRolls.length} 卷方案未通过校验，请检查后重新保存`)
      return false
    }
    await moveToPreview(options.state, options.moveToStep, result.version)
    return true
  }

  return { advance, savingAutoPlans: savingAutoPlansBatch }
}

async function persistPlans(
  items: AutoFinishConfigItem[],
  options: Options,
  savePlan: (variables: SavePlanItemsBatchVariables) => Promise<VersionedWriteResult<PlanPreviewVO[]>>,
): Promise<PersistPlansResult> {
  const expectedVersion = options.state.getDraftVersion()
  if (!options.state.orderUuid) return { blockedRolls: [], version: expectedVersion }
  const savedItems = items.filter((item) => item.roll.uuid)
  if (!savedItems.length) return { blockedRolls: [], version: expectedVersion }
  const saveResult = await savePlan({
    orderUuid: options.state.orderUuid,
    dto: {
      expectedVersion,
      items: savedItems.map((item) => ({ originalUuid: item.roll.uuid!, plan: item.plan })),
    },
  })
  const previewByRoll = new Map(saveResult.data.map((preview) => [preview.originalUuid, preview]))
  const savedPreviews: Record<string, PlanPreviewVO> = {}
  for (const item of savedItems) {
    const preview = previewByRoll.get(item.roll.uuid!)
    if (preview) savedPreviews[item.roll.localId] = preview
  }
  options.state.setPreviews((previous) => ({
    ...previous,
    ...savedPreviews,
  }))
  options.state.setPlans((previous) => ({
    ...previous,
    ...Object.fromEntries(savedItems.map((item) => [item.roll.localId, item.plan])),
  }))
  options.state.setConfiguredPlanIds((previous) => reconcileConfiguredPlanIds(previous,
    savedItems.map((item) => ({
      localId: item.roll.localId,
      preview: savedPreviews[item.roll.localId],
    }))))
  const version = saveResult.version
  options.state.setDraftVersion(version)
  return {
    blockedRolls: savedItems
      .filter((item) => savedPreviews[item.roll.localId]?.ready !== true)
      .map((item) => item.roll),
    version,
  }
}

interface PersistPlansResult {
  blockedRolls: AutoFinishConfigItem['roll'][]
  version: number
}

interface SavePlanItemsBatchVariables {
  orderUuid: string
  dto: {
    expectedVersion: number
    items: Array<{ originalUuid: string; plan: ProcessPlanDTO }>
  }
}

async function moveToPreview(
  state: CreateOrderDraftState,
  moveToStep: MoveToCreateOrderStep,
  expectedVersion = state.getDraftVersion(),
) {
  await moveToStep(4, state.orderUuid, expectedVersion)
}
