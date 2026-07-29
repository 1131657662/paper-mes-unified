import { useSaveProgress } from './useSaveProgress'
import type { CreateOrderDraftState } from './useCreateOrderDraftState'

export function useCreateOrderStepNavigation(state: CreateOrderDraftState) {
  const { isPending: savingProgress, mutateAsync: saveProgress } = useSaveProgress()

  const moveToStep = async (
    nextStep: number,
    uuid = state.orderUuid,
    expectedVersion = state.getDraftVersion(),
  ) => {
    let version = expectedVersion
    if (uuid) {
      const result = await saveProgress({ uuid, currentStep: nextStep, expectedVersion })
      version = result.version
      state.setDraftVersion(version)
    }
    state.setCurrent(nextStep)
    return version
  }

  return { moveToStep, savingProgress }
}

export type MoveToCreateOrderStep = ReturnType<typeof useCreateOrderStepNavigation>['moveToStep']
