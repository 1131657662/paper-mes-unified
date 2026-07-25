import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteProcessStep } from '../../../api/processOrder'
import { runVersionSynchronizedMutation } from '../serviceMutationCommit'
import {
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
      await runVersionSynchronizedMutation({
        ensureVersionReady: () => lifecycle.ensureVersionReady(queryClient, orderUuid),
        markVersionSyncRequired: lifecycle.markVersionSyncRequired,
        mutate: () => deleteProcessStep(stepUuid),
        synchronizeVersion: () => lifecycle.synchronizeLatest(queryClient, orderUuid),
      })
      message.success('附加工艺已删除')
    },
  })
}
