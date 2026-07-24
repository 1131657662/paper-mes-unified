import { useCallback, useState } from 'react'
import type { RouteDraftStage } from './routeDraftModel'

const MAX_HISTORY = 30

interface RouteDraftHistory {
  future: RouteDraftStage[][]
  past: RouteDraftStage[][]
  present: RouteDraftStage[]
}

interface RouteDraftHistoryApi {
  canRedo: boolean
  canUndo: boolean
  commit: (nextStages: RouteDraftStage[]) => void
  redo: () => void
  reset: (nextStages: RouteDraftStage[]) => void
  stages: RouteDraftStage[]
  undo: () => void
}

export function useRouteDraftHistory(initialStages: RouteDraftStage[] = []): RouteDraftHistoryApi {
  const [history, setHistory] = useState<RouteDraftHistory>(() => ({
    future: [],
    past: [],
    present: cloneStages(initialStages),
  }))

  const reset = useCallback((nextStages: RouteDraftStage[]) => {
    setHistory({ future: [], past: [], present: cloneStages(nextStages) })
  }, [])

  const commit = useCallback((nextStages: RouteDraftStage[]) => {
    setHistory((current) => commitHistory(current, nextStages))
  }, [])

  const undo = useCallback(() => {
    setHistory(undoHistory)
  }, [])

  const redo = useCallback(() => {
    setHistory(redoHistory)
  }, [])

  return {
    canRedo: history.future.length > 0,
    canUndo: history.past.length > 0,
    commit,
    redo,
    reset,
    stages: history.present,
    undo,
  }
}

function commitHistory(current: RouteDraftHistory, nextStages: RouteDraftStage[]): RouteDraftHistory {
  const next = cloneStages(nextStages)
  if (sameStages(current.present, next)) return current
  return {
    future: [],
    past: [...current.past, cloneStages(current.present)].slice(-MAX_HISTORY),
    present: next,
  }
}

function undoHistory(current: RouteDraftHistory): RouteDraftHistory {
  const previous = current.past.at(-1)
  if (!previous) return current
  return {
    future: [cloneStages(current.present), ...current.future].slice(0, MAX_HISTORY),
    past: current.past.slice(0, -1),
    present: cloneStages(previous),
  }
}

function redoHistory(current: RouteDraftHistory): RouteDraftHistory {
  const [next, ...restFuture] = current.future
  if (!next) return current
  return {
    future: restFuture,
    past: [...current.past, cloneStages(current.present)].slice(-MAX_HISTORY),
    present: cloneStages(next),
  }
}

function cloneStages(stages: RouteDraftStage[]): RouteDraftStage[] {
  return cloneValue(stages)
}

function cloneValue<T>(value: T): T {
  if (typeof structuredClone === 'function') return structuredClone(value)
  return JSON.parse(JSON.stringify(value)) as T
}

function sameStages(left: RouteDraftStage[], right: RouteDraftStage[]) {
  return JSON.stringify(left) === JSON.stringify(right)
}
