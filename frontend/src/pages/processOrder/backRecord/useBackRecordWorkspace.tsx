import { useProcessOrderDetail } from '../../../features/processOrderDetail/hooks/useProcessOrderDetail'
import BackRecordWorkspaceModals from './BackRecordWorkspaceModals'
import { useBackRecordChangeActions } from './useBackRecordChangeActions'
import { useBackRecordFormState } from './useBackRecordFormState'
import { useBackRecordSelection } from './useBackRecordSelection'
import { useBackRecordSubmission } from './useBackRecordSubmission'
import { useBackRecordWarehouseSelection } from './useBackRecordWarehouseSelection'

interface Params {
  uuid?: string | null
  enabled?: boolean
  onClose: () => void
  onPersisted?: () => void
  onSuccess: () => void
}

export function useBackRecordWorkspace({ uuid, enabled = true, onClose, onPersisted, onSuccess }: Params) {
  const detailQuery = useProcessOrderDetail(uuid ?? undefined, { enabled })
  const formState = useBackRecordFormState({ detail: detailQuery.data, enabled })
  const warehouse = useBackRecordWarehouseSelection({
    detail: detailQuery.data,
    enabled,
    form: formState.form,
  })
  const selection = useBackRecordSelection(detailQuery.data)
  const submission = useBackRecordSubmission({
    detail: detailQuery.data,
    enabled,
    form: formState.form,
    onClose,
    onPersisted,
    onRefetch: detailQuery.refetch,
    onReloaded: formState.initialize,
    onResetInitialization: formState.resetInitialization,
    onSuccess,
    selectedWarehouseName: warehouse.selectedName,
    selection,
    uuid,
  })
  const changes = useBackRecordChangeActions({
    detail: detailQuery.data,
    enabled,
    onClose,
    onRefetch: detailQuery.refetch,
    onResetInitialization: formState.resetInitialization,
    onSuccess,
    uuid,
  })

  return {
    detail: detailQuery.data,
    form: formState.form,
    isDetailError: detailQuery.isError,
    isLoadingDetail: detailQuery.isLoading,
    refetchDetail: detailQuery.refetch,
    isSubmitting: submission.isSubmitting,
    modals: (
      <BackRecordWorkspaceModals
        authForm={submission.authForm}
        authOpen={submission.authOpen}
        varianceForm={submission.varianceForm}
        varianceOpen={submission.varianceOpen}
        changeItem={changes.changeItem}
        changeOpen={changes.changeOpen}
        detail={detailQuery.data ?? null}
        addingStep={changes.addingStep}
        rollingBack={changes.rollingBack}
        onAddExtraStep={changes.addExtraStep}
        onCancelAuth={() => submission.setAuthOpen(false)}
        onCancelVariance={() => submission.setVarianceOpen(false)}
        onCancelChange={() => changes.setChangeOpen(false)}
        onCancelStep={() => changes.setStepFormOpen(false)}
        onOpenStep={() => changes.setStepFormOpen(true)}
        onRollbackToDraft={changes.rollbackToDraft}
        onRollbackToConfig={changes.rollbackToConfig}
        onSubmitAuth={submission.submitAuthorization}
        onSubmitVariance={submission.submitVariance}
        stepFormOpen={changes.stepFormOpen}
      />
    ),
    values: formState.displayValues,
    selection,
    warehouse,
    syncFilledValues: formState.syncFilledValues,
    openChangeGuide: changes.openChangeGuide,
    submit: submission.submit,
  }
}
