import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteDraftProcessStep } from '../../../api/processOrder'
import { runVersionSynchronizedMutation } from '../serviceMutationCommit'
import { serviceStepIsAbsent } from '../serviceStepWriteModel'
import {
  currentProcessOrderSteps,
  draftServiceMutationLifecycle,
  requiredDraftOrderUuid,
  type DraftServiceMutationOptions,
} from './draftServiceMutationLifecycle'

export function useDeleteDraftServiceStep(options: DraftServiceMutationOptions) {
  const queryClient = useQueryClient()
  const lifecycle = draftServiceMutationLifecycle(options)
  return useMutation({
    mutationFn: async (stepUuid: string) => {
      const orderUuid = requiredDraftOrderUuid(options.orderUuid)
      let expectedVersion = options.draftVersion
      await runVersionSynchronizedMutation({
        clearVersionSyncRequired: lifecycle.clearVersionSyncRequired,
        ensureVersionReady: async () => {
          expectedVersion = await lifecycle.ensureVersionReady(queryClient, orderUuid)
        },
        isAppliedAfterSync: () => serviceStepIsAbsent(
          currentProcessOrderSteps(queryClient, orderUuid),
          stepUuid,
        ),
        markVersionSyncRequired: lifecycle.markVersionSyncRequired,
        mutate: () => serviceStepIsAbsent(currentProcessOrderSteps(queryClient, orderUuid), stepUuid)
          ? Promise.resolve()
          : deleteDraftProcessStep(stepUuid, expectedVersion),
        synchronizeVersion: () => lifecycle.synchronizeLatest(queryClient, orderUuid),
      })
      message.success('附加工艺已删除')
    },
  })
}
