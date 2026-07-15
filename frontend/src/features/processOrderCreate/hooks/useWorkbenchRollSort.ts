import { useState } from 'react'
import type { RollDraft } from '../types'
import {
  DEFAULT_WORKBENCH_ROLL_SORT,
  sortWorkbenchRolls,
  type WorkbenchRollSortDirection,
  type WorkbenchRollSortMode,
  type WorkbenchRollSortPreference,
} from '../workbenchRollSort'

const STORAGE_KEY = 'paper-mes:workbench-roll-sort'

interface WorkbenchRollSortState {
  preference: WorkbenchRollSortPreference
  sortedRolls: RollDraft[]
  setPreference: (preference: WorkbenchRollSortPreference) => void
}

export function useWorkbenchRollSort(rolls: RollDraft[]): WorkbenchRollSortState {
  const [preference, setPreferenceState] = useState(loadPreference)
  const setPreference = (next: WorkbenchRollSortPreference) => {
    setPreferenceState(next)
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    } catch {
      // Sorting remains usable when storage is unavailable.
    }
  }
  return { preference, sortedRolls: sortWorkbenchRolls(rolls, preference), setPreference }
}

function loadPreference(): WorkbenchRollSortPreference {
  try {
    const value: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null')
    if (isPreference(value)) return value
  } catch {
    return DEFAULT_WORKBENCH_ROLL_SORT
  }
  return DEFAULT_WORKBENCH_ROLL_SORT
}

function isPreference(value: unknown): value is WorkbenchRollSortPreference {
  if (typeof value !== 'object' || value === null) return false
  if (!('mode' in value) || !('direction' in value)) return false
  return isSortMode(value.mode) && isSortDirection(value.direction)
}

function isSortMode(value: unknown): value is WorkbenchRollSortMode {
  return value === 'original' || value === 'spec' || value === 'width' || value === 'weight'
}

function isSortDirection(value: unknown): value is WorkbenchRollSortDirection {
  return value === 'asc' || value === 'desc'
}
