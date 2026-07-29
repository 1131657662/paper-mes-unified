import { message } from 'antd'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  addDraftProcessStep,
  updateDraftProcessStep,
  type ProcessStepDTO,
} from '../../../api/processOrder'
import type { ProcessStep } from '../../../types/processOrder'
import { runVersionSynchronizedMutation } from '../serviceMutationCommit'
import { resolveServiceStepWriteTarget, serviceStepsMatchRequests } from '../serviceStepWriteModel'
import {
  currentProcessOrderSteps,
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
      let expectedVersion = options.draftVersion
      await runVersionSynchronizedMutation({
        clearVersionSyncRequired: lifecycle.clearVersionSyncRequired,
        ensureVersionReady: async () => {
          expectedVersion = await lifecycle.ensureVersionReady(queryClient, orderUuid)
        },
        isAppliedAfterSync: () => serviceStepsMatchRequests(
          currentProcessOrderSteps(queryClient, orderUuid),
          [values],
        ),
        markVersionSyncRequired: lifecycle.markVersionSyncRequired,
        mutate: () => persistStep(
          { orderUuid, queryClient, stepUuid, values, expectedVersion },
          options.steps,
        ),
        synchronizeVersion: () => lifecycle.synchronizeLatest(queryClient, orderUuid),
      })
      message.success(stepUuid ? '当前卷附加工艺已更新' : '当前卷附加工艺已保存')
    },
  })
}

async function persistStep(
  variables: Variables & {
    expectedVersion: number
    orderUuid: string
    queryClient: ReturnType<typeof useQueryClient>
  },
  fallbackSteps: ProcessStep[],
) {
  const steps = (currentProcessOrderSteps(variables.queryClient, variables.orderUuid, fallbackSteps) ?? [])
    .filter((step) => step.originalUuid === variables.values.originalUuid && step.isMain !== 1)
  const target = resolveServiceStepWriteTarget(steps, variables.values.stepType, variables.stepUuid)
  if (target.kind === 'duplicate') throw duplicateStepError()
  if (target.kind === 'update') {
    return updateDraftProcessStep(target.stepUuid, variables.values, variables.expectedVersion)
  }
  return addDraftProcessStep(variables.orderUuid, variables.values, variables.expectedVersion)
}

function duplicateStepError(): Error {
  message.error('当前卷存在重复同类工艺，请先删除重复项')
  return new Error('duplicate service steps')
}
