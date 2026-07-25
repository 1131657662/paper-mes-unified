import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addProcessStepsBatch, type ProcessStepBatchResult, type ProcessStepDTO } from '../../../api/processOrder'
import type { ProcessStep } from '../../../types/processOrder'
import {
  buildServiceStepBatch,
  resolveServiceApplyTargets,
  type FixedAmountScope,
} from '../serviceStepBatchModel'
import { runVersionSynchronizedMutation } from '../serviceMutationCommit'
import type { RollDraft } from '../types'
import {
  draftServiceMutationLifecycle,
  requiredDraftOrderUuid,
  type DraftServiceMutationOptions,
} from './draftServiceMutationLifecycle'

interface Options extends DraftServiceMutationOptions {
  allSteps: ProcessStep[]
  selectedRolls: RollDraft[]
}

interface Variables {
  scope: FixedAmountScope
  values: ProcessStepDTO
}

export function useApplyDraftServiceStep(options: Options) {
  const queryClient = useQueryClient()
  const lifecycle = draftServiceMutationLifecycle(options)
  return useMutation({
    mutationFn: async ({ scope, values }: Variables) => {
      const orderUuid = requiredDraftOrderUuid(options.orderUuid)
      const targets = resolveServiceApplyTargets({
        rolls: options.selectedRolls,
        stepType: values.stepType,
        steps: options.allSteps,
      })
      if (!targets.targetUuids.length) throw noBatchTargetError()
      const result = await runVersionSynchronizedMutation({
        ensureVersionReady: () => lifecycle.ensureVersionReady(queryClient, orderUuid),
        markVersionSyncRequired: lifecycle.markVersionSyncRequired,
        mutate: () => addProcessStepsBatch(orderUuid, {
          steps: buildServiceStepBatch(values, targets.targetUuids, scope),
        }),
        synchronizeVersion: () => lifecycle.synchronizeLatest(queryClient, orderUuid),
      })
      showBatchSuccess(result, targets.excludedCount)
    },
  })
}

function noBatchTargetError(): Error {
  message.warning('请先在左侧勾选已保存且非直发的母卷')
  return new Error('missing service batch target')
}

function showBatchSuccess(result: ProcessStepBatchResult, excludedCount: number) {
  const excluded = excludedCount ? `，${excludedCount} 卷因未保存或为直发未参与` : ''
  message.success(`已应用 ${result.selectedCount} 卷：新增 ${result.createdCount}，更新 ${result.updatedCount}${excluded}`)
}
