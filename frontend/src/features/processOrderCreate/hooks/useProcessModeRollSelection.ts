import { useState } from 'react'
import type { RollDraft } from '../types'

interface ProcessModeRollSelection {
  checkedIds: string[]
  checkedRolls: RollDraft[]
  clear: () => void
  selectAll: () => void
  toggle: (localId: string, checked: boolean) => void
}

export function useProcessModeRollSelection(
  rolls: RollDraft[],
  currentId?: string,
): ProcessModeRollSelection {
  const eligibleRolls = rolls.filter((roll) => Boolean(roll.uuid) && roll.localId !== currentId)
  const [checkedIds, setCheckedIds] = useState<string[]>([])
  const checkedRolls = eligibleRolls.filter((roll) => checkedIds.includes(roll.localId))

  const toggle = (localId: string, checked: boolean) => {
    setCheckedIds((current) => checked
      ? Array.from(new Set([...current, localId]))
      : current.filter((id) => id !== localId))
  }

  return {
    checkedIds: checkedRolls.map((roll) => roll.localId),
    checkedRolls,
    clear: () => setCheckedIds([]),
    selectAll: () => setCheckedIds(eligibleRolls.map((roll) => roll.localId)),
    toggle,
  }
}
