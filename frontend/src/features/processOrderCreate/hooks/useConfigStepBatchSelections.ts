import { useState } from 'react'

interface BatchSelection {
  clear: () => void
  ids: string[]
  replace: (ids: string[]) => void
  toggle: (localId: string, checked: boolean) => void
}

export function useConfigStepBatchSelections(): {
  plan: BatchSelection
  service: BatchSelection
} {
  const [planIds, setPlanIds] = useState<string[]>([])
  const [serviceIds, setServiceIds] = useState<string[]>([])
  return {
    plan: {
      clear: () => setPlanIds([]),
      ids: planIds,
      replace: setPlanIds,
      toggle: (localId, checked) => setPlanIds((current) => toggleId(current, localId, checked)),
    },
    service: {
      clear: () => setServiceIds([]),
      ids: serviceIds,
      replace: setServiceIds,
      toggle: (localId, checked) => setServiceIds((current) => toggleId(current, localId, checked)),
    },
  }
}

function toggleId(current: string[], localId: string, checked: boolean): string[] {
  if (!checked) return current.filter((id) => id !== localId)
  return Array.from(new Set([...current, localId]))
}
