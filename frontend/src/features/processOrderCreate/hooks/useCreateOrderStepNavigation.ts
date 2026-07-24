import { useSaveProgress } from './useSaveProgress'
import type { CreateOrderDraftState } from './useCreateOrderDraftState'

export function useCreateOrderStepNavigation(state: CreateOrderDraftState) {
  const { mutateAsync: saveProgress } = useSaveProgress()

  const moveToStep = async (
    nextStep: number,
    uuid = state.orderUuid,
    expectedVersion = state.draftVersion,
  ) => {
    if (uuid) {
      await saveProgress({ uuid, currentStep: nextStep, expectedVersion })
      state.setDraftVersion(expectedVersion + 1)
    }
    state.setCurrent(nextStep)
    return expectedVersion + 1
  }

  return { moveToStep }
}

export type MoveToCreateOrderStep = ReturnType<typeof useCreateOrderStepNavigation>['moveToStep']
