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

interface UseCreateOrderPageOptions {
  resetLocalDraft?: boolean
}

export function useCreateOrderPage(
  draftUuid?: string,
  options: UseCreateOrderPageOptions = {},
) {
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
  const { moveToStep } = useCreateOrderStepNavigation(state)
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
    savingBase: setupActions.savingBase,
    savingRolls: setupActions.savingRolls,
    updatingRolls: setupActions.updatingRolls,
    savingWorkbench: planActions.savingWorkbench || configAdvance.savingAutoPlans,
    submitting: submission.submitting,
    retryLoad,
    setCurrent: state.setCurrent,
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
  }
}
