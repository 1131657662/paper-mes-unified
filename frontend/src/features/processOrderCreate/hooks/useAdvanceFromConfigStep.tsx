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
      message.warning('请逐卷检查成品配置并点击“保存当前方案”后再继续')
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
    const nextVersion = await persistPlans(items, options, savePlanItemsBatch)
    await moveToPreview(options.state, options.moveToStep, nextVersion)
    return true
  }

  return { advance, savingAutoPlans: savingAutoPlansBatch }
}

async function persistPlans(
  items: AutoFinishConfigItem[],
  options: Options,
  savePlan: (variables: SavePlanItemsBatchVariables) => Promise<PlanPreviewVO[]>,
): Promise<number> {
  if (!options.state.orderUuid) return options.state.draftVersion
  const savedItems = items.filter((item) => item.roll.uuid)
  if (!savedItems.length) return options.state.draftVersion
  const previews = await savePlan({
    orderUuid: options.state.orderUuid,
    dto: {
      expectedVersion: options.state.draftVersion,
      items: savedItems.map((item) => ({ originalUuid: item.roll.uuid!, plan: item.plan })),
    },
  })
  const previewByRoll = new Map(previews.map((preview) => [preview.originalUuid, preview]))
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
  options.state.setConfiguredPlanIds((previous) => [...new Set([
    ...previous,
    ...savedItems.map((item) => item.roll.localId),
  ])])
  const version = options.state.draftVersion + 1
  options.state.setDraftVersion(version)
  return version
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
  expectedVersion = state.draftVersion,
) {
  await moveToStep(4, state.orderUuid, expectedVersion)
}
