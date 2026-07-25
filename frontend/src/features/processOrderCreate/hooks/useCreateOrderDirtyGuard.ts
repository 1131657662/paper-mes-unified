import { useCallback, useRef } from 'react'
import { useUnsavedChangesGuard } from '../../../hooks/useUnsavedChangesGuard'
import {
  addDirtySource,
  removeDirtySource,
  type CreateOrderDirtySource,
} from '../dirtySourceModel'
import type { CreateOrderDraftSnapshot } from './useCreateOrderDraftState'

interface Options {
  captureSnapshot: () => CreateOrderDraftSnapshot
  restoreSnapshot: (snapshot: CreateOrderDraftSnapshot) => void
}

export function useCreateOrderDirtyGuard(options: Options) {
  const { captureSnapshot, restoreSnapshot } = options
  const sources = useRef<Set<CreateOrderDirtySource>>(new Set())
  const draftSnapshot = useRef<CreateOrderDraftSnapshot>()
  const discardChanges = useCallback(() => {
    if (draftSnapshot.current) restoreSnapshot(draftSnapshot.current)
    draftSnapshot.current = undefined
    sources.current.clear()
  }, [restoreSnapshot])
  const { clearDirty, markDirty, runIfClean } = useUnsavedChangesGuard({ onDiscard: discardChanges })

  const markDraftDirty = useCallback(() => {
    if (sources.current.has('draft')) return
    draftSnapshot.current = captureSnapshot()
    sources.current = addDirtySource(sources.current, 'draft')
    markDirty()
  }, [captureSnapshot, markDirty])

  const clearDraftDirty = useCallback(() => {
    draftSnapshot.current = undefined
    clearSource('draft', sources, clearDirty)
  }, [clearDirty])

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
    markDraftDirty,
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
