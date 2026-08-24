import { useProcessOrderDetail } from '../../processOrderDetail/hooks/useProcessOrderDetail'
import type { ConfigEditorTab, ConfigStepWorkspaceData } from '../components/configStepWorkspaceTypes'
import type { ConfigStepProps } from '../components/configStepTypes'
import {
  configurableRolls as getConfigurableRolls,
  planBatchSelectionReasons,
  planBatchTargets,
  selectedConfigRoll,
  serviceBatchSelectionReasons,
} from '../configStepSelection'
import { defaultPlanForRoll } from '../draftMappers'
import { applyDefaultMachineToPlan } from '../machineDefaults'
import { mergedSourceLocks } from '../rewindConsumptionUtils'
import { serviceStepsForRoll } from '../serviceStepBatchModel'
import { calculateRollWeightBalance } from '../weightBalanceModel'
import { useAutoPlanPreview } from './useAutoPlanPreview'
import type { useConfigStepBatchSelections } from './useConfigStepBatchSelections'

interface Options {
  preferredEditor: ConfigEditorTab
  props: ConfigStepProps
  selections: ReturnType<typeof useConfigStepBatchSelections>
}

export interface ConfigStepWorkspaceModel {
  batchTargets: ReturnType<typeof planBatchTargets>
  configurableRolls: ReturnType<typeof getConfigurableRolls>
  data: ConfigStepWorkspaceData
  detailQuery: ReturnType<typeof useProcessOrderDetail>
  previewCurrent: () => Promise<void>
  selected: ReturnType<typeof selectedConfigRoll>
  serviceOnlyRolls: ReturnType<typeof getConfigurableRolls>
}

export function useConfigStepWorkspaceModel(options: Options): ConfigStepWorkspaceModel {
  const { preferredEditor, props, selections } = options
  const lockedRolls = mergedSourceLocks(props.rolls, props.plans)
  const configurableRolls = getConfigurableRolls(props.rolls, lockedRolls)
  const selected = selectedConfigRoll(props.rolls, props.selectedId, lockedRolls)
  const defaults = props.defaultPlanOptions ?? { spareCount: props.defaultSpareCount ?? 0 }
  const plan = selected
    ? applyDefaultMachineToPlan(
      props.plans[selected.localId] ?? defaultPlanForRoll(selected, defaults),
      props.machines,
      selected,
    )
    : undefined
  const serviceOnly = selected?.processMode === 4
  const activeEditor = serviceOnly ? 'service' : preferredEditor
  const selectionReasonOptions = {
    locks: lockedRolls, rolls: props.rolls, routePreviews: props.routePreviews, selected,
  }
  const selectionDisabledReasons = activeEditor === 'plan'
    ? planBatchSelectionReasons(selectionReasonOptions)
    : serviceBatchSelectionReasons(selectionReasonOptions)
  const detailQuery = useProcessOrderDetail(props.orderUuid, { enabled: Boolean(props.orderUuid) })
  const localServiceSteps = props.serviceStepsByRoll ?? {}
  const localSteps = Object.values(localServiceSteps).flat()
  const allSteps = [...(detailQuery.data?.steps ?? []), ...localSteps]
  const serviceConfigured = Object.fromEntries(
    props.rolls
      .filter((roll) => roll.uuid)
      .map((roll) => [roll.uuid!, serviceStepsForRoll(allSteps, roll.uuid).length > 0]),
  )
  const routePreview = selected?.uuid ? props.routePreviews[selected.uuid] : undefined
  const batchTargets = planBatchTargets({
    checkedIds: selections.plan.ids,
    locks: lockedRolls,
    rolls: props.rolls,
    routePreviews: props.routePreviews,
    selected,
  })
  const balance = selected ? calculateRollWeightBalance({
    roll: selected,
    rolls: props.rolls,
    plan,
    preview: props.previews[selected.localId],
    routePreview,
  }) : undefined

  const previewCoordinator = useAutoPlanPreview({
    orderUuid: props.orderUuid,
    selected: routePreview || serviceOnly ? undefined : selected,
    selectedPlan: plan,
    onPreviewPlan: props.onPreviewPlan,
  })

  return {
    batchTargets,
    configurableRolls,
    detailQuery,
    previewCurrent: previewCoordinator.previewNow,
    selected,
    serviceOnlyRolls: configurableRolls.filter((roll) => roll.processMode === 4),
    data: buildWorkspaceData({
      activeEditor,
      allSteps,
      balance,
      batchTargets,
      detailQuery,
      lockedRolls,
      plan,
      previewError: previewCoordinator.error,
      previewing: previewCoordinator.previewing,
      props,
      routePreview,
      selections,
      selectionDisabledReasons,
      selected,
      serviceConfigured,
      serviceOnly,
      serviceStepsByRoll: localServiceSteps,
      onServiceStepsChange: props.onServiceStepsChange,
    }),
  }
}

interface DataOptions {
  activeEditor: ConfigEditorTab
  allSteps: ConfigStepWorkspaceData['allSteps']
  balance: ConfigStepWorkspaceData['balance']
  batchTargets: ReturnType<typeof planBatchTargets>
  detailQuery: ReturnType<typeof useProcessOrderDetail>
  lockedRolls: ConfigStepWorkspaceData['lockedRolls']
  plan: ConfigStepWorkspaceData['plan']
  previewError?: string
  previewing: boolean
  props: ConfigStepProps
  routePreview: ConfigStepWorkspaceData['routePreview']
  selections: ReturnType<typeof useConfigStepBatchSelections>
  selectionDisabledReasons: Record<string, string>
  selected: ConfigStepWorkspaceData['roll']
  serviceConfigured: ConfigStepWorkspaceData['serviceConfigured']
  serviceOnly: boolean
  serviceStepsByRoll: ConfigStepWorkspaceData['serviceStepsByRoll']
  onServiceStepsChange?: ConfigStepWorkspaceData['onServiceStepsChange']
}

function buildWorkspaceData(options: DataOptions): ConfigStepWorkspaceData {
  const { props, selections } = options
  return {
    activeEditor: options.activeEditor,
    assistantEntry: props.assistantEntry,
    allSteps: options.allSteps,
    autoFinishConfigEnabled: props.autoFinishConfigEnabled,
    balance: options.balance,
    checkedIds: options.activeEditor === 'service' ? selections.service.ids : selections.plan.ids,
    configuredPlanIds: props.configuredPlanIds,
    customerPrices: props.customerPrices,
    detailError: options.detailQuery.isError,
    detailLoading: options.detailQuery.isLoading,
    draftVersion: props.draftVersion,
    lockedRolls: options.lockedRolls,
    machines: props.machines,
    mainBatchTargetCount: options.batchTargets.length,
    orderUuid: props.orderUuid,
    operation: props.operation,
    plan: options.plan,
    plans: props.plans,
    previewError: options.previewError,
    previewing: options.previewing,
    previews: props.previews,
    roll: options.selected,
    rolls: props.rolls,
    routePreview: options.routePreview,
    routePreviews: props.routePreviews,
    saving: props.saving,
    selectionDisabledReasons: options.selectionDisabledReasons,
    selectedServiceRolls: props.rolls.filter((roll) => selections.service.ids.includes(roll.localId)
      && roll.processMode !== 3 && !options.lockedRolls[roll.localId]),
    serviceConfigured: options.serviceConfigured,
    serviceOnly: options.serviceOnly,
    serviceStepsByRoll: options.serviceStepsByRoll,
    onServiceStepsChange: options.onServiceStepsChange,
  }
}
