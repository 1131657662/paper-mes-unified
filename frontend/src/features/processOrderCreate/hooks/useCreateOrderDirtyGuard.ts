import { useCallback, useRef } from 'react'
import { useUnsavedChangesGuard } from '../../../hooks/useUnsavedChangesGuard'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import {
  addDirtySource,
  removeDirtySource,
  type CreateOrderDirtySource,
} from '../dirtySourceModel'
import {
  capturePlanDirtySnapshot,
  restorePlanDirtySnapshots,
  type PlanDirtySnapshot,
} from '../planDirtySnapshot'
import type { CreateOrderDraftSnapshot } from './useCreateOrderDraftState'

interface Options {
  captureSnapshot: () => CreateOrderDraftSnapshot
  pending?: boolean
  restoreSnapshot: (snapshot: CreateOrderDraftSnapshot) => void
}

export function useCreateOrderDirtyGuard(options: Options) {
  const { captureSnapshot, restoreSnapshot } = options
  const sources = useRef<Set<CreateOrderDirtySource>>(new Set())
  const draftSnapshot = useRef<CreateOrderDraftSnapshot | undefined>(undefined)
  const planSnapshots = useRef(new Map<string, PlanDirtySnapshot>())
  const discardChanges = useCallback(() => {
    const restored = draftSnapshot.current
      ?? restorePlanDirtySnapshots(captureSnapshot(), planSnapshots.current.values())
    if (draftSnapshot.current || planSnapshots.current.size) restoreSnapshot(restored)
    draftSnapshot.current = undefined
    planSnapshots.current.clear()
    sources.current.clear()
  }, [captureSnapshot, restoreSnapshot])
  const { clearDirty, markDirty, runIfClean } = useUnsavedChangesGuard({
    onDiscard: discardChanges,
    pending: options.pending,
  })

  const markDraftDirty = useCallback(() => {
    if (sources.current.has('draft')) return
    draftSnapshot.current = captureSnapshot()
    sources.current = addDirtySource(sources.current, 'draft')
    markDirty()
  }, [captureSnapshot, markDirty])

  const clearDraftDirty = useCallback(() => {
    draftSnapshot.current = undefined
    planSnapshots.current.clear()
    clearSource('draft', sources, clearDirty)
  }, [clearDirty])

  const markPlanDirty = useCallback((localId: string) => {
    if (!planSnapshots.current.has(localId)) {
      planSnapshots.current.set(localId, capturePlanDirtySnapshot(captureSnapshot(), localId))
    }
    sources.current = addDirtySource(sources.current, 'draft')
    markDirty()
  }, [captureSnapshot, markDirty])

  const commitPlanChanges = useCallback((localIds: Iterable<string>) => {
    for (const localId of localIds) planSnapshots.current.delete(localId)
    if (!draftSnapshot.current && planSnapshots.current.size === 0) {
      clearSource('draft', sources, clearDirty)
    }
  }, [clearDirty])

  const reconcilePlanDirty = useCallback((localId: string, plan: ProcessPlanDTO) => {
    const snapshot = planSnapshots.current.get(localId)
    if (!snapshot) return
    const savedPlan = snapshot.plan.present ? snapshot.plan.value : undefined
    if (JSON.stringify(savedPlan) !== JSON.stringify(plan)) return
    restoreSnapshot(restorePlanDirtySnapshots(captureSnapshot(), [snapshot]))
    planSnapshots.current.delete(localId)
    if (!draftSnapshot.current && planSnapshots.current.size === 0) {
      clearSource('draft', sources, clearDirty)
    }
  }, [captureSnapshot, clearDirty, restoreSnapshot])

  const setServiceDirty = useCallback((dirty: boolean) => {
    if (dirty) {
      sources.current = addDirtySource(sources.current, 'service')
      markDirty()
      return
    }
    clearSource('service', sources, clearDirty)
  }, [clearDirty, markDirty])

  return {
    clearDraftDirty,
    commitPlanChanges,
    markDraftDirty,
    markPlanDirty,
    reconcilePlanDirty,
    runIfClean,
    setServiceDirty,
  }
}

function clearSource(
  source: CreateOrderDirtySource,
  sources: { current: Set<CreateOrderDirtySource> },
  clearDirty: () => void,
) {
  sources.current = removeDirtySource(sources.current, source)
  if (sources.current.size === 0) clearDirty()
}
