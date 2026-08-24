import {
  createDefaultPlanOptions,
  toCustomerOptions,
  toReferenceOptions,
} from '../createOrderReferenceModel'
import { useCreateOrderDraftState } from './useCreateOrderDraftState'
import { useCreateOrderPlanActions } from './useCreateOrderPlanActions'
import { useCreateOrderSetupActions } from './useCreateOrderSetupActions'
import { useCreateOrderStepNavigation } from './useCreateOrderStepNavigation'
import { useCreateOrderSubmission } from './useCreateOrderSubmission'
import { useAdvanceFromConfigStep } from './useAdvanceFromConfigStep'
import { useGetDraft } from './useGetDraft'
import { useCustomers, useMachines, useWarehouses } from './useReferenceData'
import { useProcessOrderCreateSettings } from './useProcessOrderCreateSettings'
import { useProcessOrderDetail } from '../../processOrderDetail/hooks/useProcessOrderDetail'
import { serviceStepsForRoll } from '../serviceStepBatchModel'
import { nonDraftOrderUuid } from '../draftAccess'
import { useCreateOrderAiConfirmation } from './useCreateOrderAiConfirmation'
import { useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'

interface UseCreateOrderPageOptions {
  resetLocalDraft?: boolean
}

export function useCreateOrderPage(
  draftUuid?: string,
  options: UseCreateOrderPageOptions = {},
) {
  const queryClient = useQueryClient()
  const resetLocalDraft = options.resetLocalDraft === true
  const {
    data: draft,
    isError: isDraftError,
    isLoading: isLoadingDraft,
    refetch: refetchDraft,
  } = useGetDraft(draftUuid)
  const state = useCreateOrderDraftState({ draft, draftUuid, resetLocalDraft })
  const {
    data: customerPage,
    isError: isCustomerError,
    isLoading: isLoadingCustomers,
    refetch: refetchCustomers,
  } = useCustomers()
  const {
    data: warehousePage,
    isError: isWarehouseError,
    isLoading: isLoadingWarehouses,
    refetch: refetchWarehouses,
  } = useWarehouses()
  const {
    data: machinePage,
    isError: isMachineError,
    isLoading: isLoadingMachines,
    refetch: refetchMachines,
  } = useMachines()

  const customers = customerPage?.records ?? []
  const machines = machinePage?.records ?? []
  const selectedCustomer = customers.find((item) => item.uuid === state.baseInfo?.customerUuid)
  const {
    autoFinishConfigEnabled,
    defaultSpareCount,
    isError: isSettingsError,
    isLoading: isLoadingSettings,
    refetch: refetchSettings,
  } = useProcessOrderCreateSettings()
  const defaultPlanOptions = createDefaultPlanOptions(selectedCustomer, defaultSpareCount)
  const { moveToStep, savingProgress } = useCreateOrderStepNavigation(state)
  const setupActions = useCreateOrderSetupActions({
    defaultPlanOptions,
    machines,
    moveToStep,
    state,
  })
  const planActions = useCreateOrderPlanActions({
    defaultPlanOptions,
    machines,
    state,
  })
  const detailQuery = useProcessOrderDetail(state.orderUuid, {
    enabled: Boolean(state.orderUuid) && state.current >= 3,
  })
  const ai = useCreateOrderAiConfirmation(state, async () => {
    if (!state.orderUuid) throw new Error('当前草稿不存在，无法加载 AI 已保存的配置')
    const refreshed = await queryClient.fetchQuery(queries.createOrder.draft(state.orderUuid))
    state.hydrateDraft(refreshed, { preserveCurrentStep: true })
    await detailQuery.refetch()
  })
  const serviceConfigured = detailQuery.data
    ? Object.fromEntries(state.rolls.filter((roll) => roll.uuid).map((roll) => [
      roll.uuid!,
      serviceStepsForRoll(detailQuery.data?.steps, roll.uuid).length > 0,
    ]))
    : {}
  const configAdvance = useAdvanceFromConfigStep({
    autoFinishConfigEnabled,
    defaultPlanOptions,
    machines,
    moveToStep,
    state,
  })
  const submission = useCreateOrderSubmission(state)
  const referenceLoadError = isCustomerError || isWarehouseError || isMachineError

  const retryLoad = async () => {
    const requests: Promise<unknown>[] = [
      refetchCustomers(),
      refetchWarehouses(),
      refetchMachines(),
      refetchSettings(),
    ]
    if (draftUuid) requests.push(refetchDraft())
    await Promise.all(requests)
  }

  return {
    autoFinishConfigEnabled,
    baseInfo: state.baseInfo,
    current: state.current,
    configuredPlanIds: state.configuredPlanIds,
    draftVersion: state.draftVersion,
    defaultSpareCount,
    defaultPlanOptions,
    loadError: isDraftError
      ? 'draft' as const
      : isSettingsError
        ? 'settings' as const
        : referenceLoadError
          ? 'reference' as const
          : undefined,
    loadingPage: isLoadingDraft || isLoadingCustomers || isLoadingWarehouses
      || isLoadingMachines || isLoadingSettings,
    orderUuid: state.orderUuid,
    nonDraftOrderUuid: nonDraftOrderUuid(draftUuid, draft),
    plans: state.plans,
    previews: state.previews,
    routePreviews: state.routePreviews,
    serviceConfigured,
    routes: state.routes,
    rolls: state.rolls,
    selectedId: state.selectedId ?? state.rolls[0]?.localId,
    submitResult: state.submitResult,
    captureSnapshot: state.captureSnapshot,
    restoreSnapshot: state.restoreSnapshot,
    customerOptions: toCustomerOptions(customers),
    customerProcessPrices: selectedCustomer?.processPrices,
    warehouseOptions: toReferenceOptions(warehousePage?.records ?? [], 'warehouseName'),
    machines,
    creatingDraft: setupActions.creatingDraft,
    savingBase: setupActions.savingBase || savingProgress,
    savingRolls: setupActions.savingRolls || savingProgress,
    updatingRolls: setupActions.updatingRolls || savingProgress,
    savingWorkbench: planActions.savingWorkbench || configAdvance.savingAutoPlans || savingProgress,
    workbenchOperation: configAdvance.savingAutoPlans || savingProgress
      ? 'saving' as const : planActions.operation,
    submitting: submission.submitting,
    workflowPending: setupActions.creatingDraft || setupActions.savingBase
      || setupActions.savingRolls || setupActions.updatingRolls || savingProgress
      || planActions.savingWorkbench || configAdvance.savingAutoPlans || submission.submitting,
    retryLoad,
    setCurrent: state.setCurrent,
    setDraftVersion: state.setDraftVersion,
    setRolls: state.setRolls,
    setSelectedId: state.setSelectedId,
    handleBaseInfoChange: setupActions.handleBaseInfoChange,
    handleBaseNext: setupActions.handleBaseNext,
    handleConfigNext: configAdvance.advance,
    handleImportPreview: setupActions.handleImportPreview,
    handlePlanChange: planActions.handlePlanChange,
    handlePreviewPlan: planActions.handlePreviewPlan,
    handleProcessNext: setupActions.handleProcessNext,
    handleRollsNext: setupActions.handleRollsNext,
    handleSavePlan: planActions.handleSavePlan,
    handleSavePlanBatch: planActions.handleSavePlanBatch,
    handleSubmit: submission.handleSubmit,
    applyAiConfirmation: ai.apply,
  }
}
