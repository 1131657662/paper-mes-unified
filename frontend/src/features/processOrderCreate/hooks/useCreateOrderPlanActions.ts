import { useRef } from 'react'
import type { Machine } from '../../../types/machine'
import type { DefaultPlanOptions } from '../draftMappers'
import { createPlanActionCommands } from '../planActionCommands'
import { PlanOperationTracker } from '../planOperationTracker'
import type { CreateOrderDraftState } from './useCreateOrderDraftState'
import { usePreviewPlan } from './usePreviewPlan'
import { useSavePlan } from './useSavePlan'
import { useSavePlanBatch } from './useSavePlanBatch'

interface UseCreateOrderPlanActionsOptions {
  defaultPlanOptions: DefaultPlanOptions
  machines: Machine[]
  state: CreateOrderDraftState
}

export function useCreateOrderPlanActions(options: UseCreateOrderPlanActionsOptions) {
  const { defaultPlanOptions, machines, state } = options
  const { mutateAsync: previewPlan, isPending: previewingPlan } = usePreviewPlan()
  const { mutateAsync: savePlan, isPending: savingPlan } = useSavePlan()
  const { mutateAsync: savePlanBatch, isPending: savingPlanBatch } = useSavePlanBatch()
  const tracker = useRef(new PlanOperationTracker()).current
  const commands = createPlanActionCommands({
    defaultPlanOptions, machines, previewPlan, savePlan, savePlanBatch, state, tracker,
  })

  return {
    operation: savingPlan || savingPlanBatch ? 'saving' as const
      : previewingPlan ? 'validating' as const : undefined,
    savingWorkbench: savingPlan || savingPlanBatch,
    handlePlanChange: commands.changePlan,
    handlePreviewPlan: commands.previewPlan,
    handleSavePlan: commands.savePlan,
    handleSavePlanBatch: commands.savePlanBatch,
  }
}
