import { useRef, useState } from 'react'
import type { ProcessStep } from '../../../types/processOrder'
import { useApplyDraftServiceStep } from './useApplyDraftServiceStep'
import { useDeleteDraftServiceStep } from './useDeleteDraftServiceStep'
import { useSaveDraftServiceStep } from './useSaveDraftServiceStep'
import type { RollDraft } from '../types'

interface Options {
  allSteps: ProcessStep[]
  draftVersion: number
  onSynchronizeVersion: () => Promise<number>
  onVersionSyncBlockedChange: (blocked: boolean) => void
  onWritePendingChange: (pending: boolean) => void
  orderUuid?: string
  selectedRolls: RollDraft[]
  steps: ProcessStep[]
  versionSyncBlocked: boolean
}

export function useDraftServiceStepWrites(options: Options) {
  const [writing, setWriting] = useState(false)
  const pendingCount = useRef(0)
  const mutationOptions = {
    draftVersion: options.draftVersion,
    onSynchronizeVersion: options.onSynchronizeVersion,
    onVersionSyncBlockedChange: options.onVersionSyncBlockedChange,
    orderUuid: options.orderUuid,
    versionSyncBlocked: options.versionSyncBlocked,
  }
  const save = useSaveDraftServiceStep({ ...mutationOptions, steps: options.steps })
  const apply = useApplyDraftServiceStep({
    ...mutationOptions,
    allSteps: options.allSteps,
    selectedRolls: options.selectedRolls,
  })
  const remove = useDeleteDraftServiceStep(mutationOptions)
  const writePending = writing || save.isPending || apply.isPending || remove.isPending

  const run = async (operation: () => Promise<void>) => {
    pendingCount.current += 1
    if (pendingCount.current === 1) changeWriting(true)
    try {
      await operation()
    } finally {
      pendingCount.current -= 1
      if (pendingCount.current === 0) changeWriting(false)
    }
  }

  function changeWriting(pending: boolean) {
    setWriting(pending)
    options.onWritePendingChange(pending)
  }

  return { apply, remove, run, save, writePending }
}
