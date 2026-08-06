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
  const detailQuery = useProcessOrderDetail(uuid ?? undefined, { enabled, freshOnMount: true })
  const detail = getFreshDetail(detailQuery.data, detailQuery.isFetchedAfterMount, detailQuery.isError)
  const detailLoading = enabled && Boolean(uuid) && !detailQuery.isFetchedAfterMount && !detailQuery.isError
  const formState = useBackRecordFormState({ detail, enabled })
  const warehouse = useBackRecordWarehouseSelection({
    detail,
    enabled,
    form: formState.form,
  })
  const selection = useBackRecordSelection(detail)
  const handlePersisted = () => {
    formState.clearDraft()
    onPersisted?.()
  }
  const submission = useBackRecordSubmission({
    detail,
    enabled,
    form: formState.form,
    getInitializedVersion: formState.getInitializedVersion,
    onClose,
    onPersistDraft: formState.persistDraft,
    onPersisted: handlePersisted,
    onRefetch: detailQuery.refetch,
    onConflictReloaded: formState.refreshPreservingValues,
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
    onPersisted,
    onRefetch: detailQuery.refetch,
    onReloaded: formState.initialize,
    onResetInitialization: formState.resetInitialization,
    onSelectAfterRefresh: selection.selectAfterRefresh,
    onSuccess,
    uuid,
  })

  return {
    detail,
    form: formState.form,
    isDetailError: detailQuery.isError,
    isLoadingDetail: detailQuery.isLoading || detailLoading,
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
        detail={detail ?? null}
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
    persistDraft: formState.persistDraft,
    openChangeGuide: changes.openChangeGuide,
    reopenBatch: changes.reopenBatch,
    reopening: changes.reopening,
    submit: submission.submit,
  }
}

function getFreshDetail(
  detail: ReturnType<typeof useProcessOrderDetail>['data'],
  fetchedAfterMount: boolean,
  isError: boolean,
) {
  if (!fetchedAfterMount || isError) return undefined
  return detail
}
