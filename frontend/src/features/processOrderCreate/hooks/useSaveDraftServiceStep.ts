import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addProcessStep, updateProcessStep, type ProcessStepDTO } from '../../../api/processOrder'
import type { ProcessStep } from '../../../types/processOrder'
import { runVersionSynchronizedMutation } from '../serviceMutationCommit'
import {
  draftServiceMutationLifecycle,
  requiredDraftOrderUuid,
  type DraftServiceMutationOptions,
} from './draftServiceMutationLifecycle'

interface Options extends DraftServiceMutationOptions {
  steps: ProcessStep[]
}

interface Variables {
  stepUuid?: string
  values: ProcessStepDTO
}

export function useSaveDraftServiceStep(options: Options) {
  const queryClient = useQueryClient()
  const lifecycle = draftServiceMutationLifecycle(options)
  return useMutation({
    mutationFn: async ({ stepUuid, values }: Variables) => {
      const orderUuid = requiredDraftOrderUuid(options.orderUuid)
      const matchingSteps = options.steps.filter((step) => step.stepType === values.stepType)
      if (matchingSteps.length > 1) throw duplicateStepError()
      const savedStepUuid = stepUuid ?? matchingSteps[0]?.uuid
      await runVersionSynchronizedMutation({
        ensureVersionReady: () => lifecycle.ensureVersionReady(queryClient, orderUuid),
        markVersionSyncRequired: lifecycle.markVersionSyncRequired,
        mutate: () => savedStepUuid
          ? updateProcessStep(savedStepUuid, values)
          : addProcessStep(orderUuid, values),
        synchronizeVersion: () => lifecycle.synchronizeLatest(queryClient, orderUuid),
      })
      message.success(savedStepUuid ? '当前卷附加工艺已更新' : '当前卷附加工艺已添加')
    },
  })
}

function duplicateStepError(): Error {
  message.error('当前卷存在重复同类工艺，请先删除重复项')
  return new Error('duplicate service steps')
}
