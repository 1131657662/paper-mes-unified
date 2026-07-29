import { useCallback, useRef, useState } from 'react'
import type { Dispatch, SetStateAction } from 'react'
import type { RollDraft } from '../types'

export function useVersionedRollState(initialRolls: RollDraft[]) {
  const [rolls, setRollsState] = useState(initialRolls)
  const rollsRef = useRef(rolls)
  const revisionRef = useRef(0)
  const setRolls: Dispatch<SetStateAction<RollDraft[]>> = useCallback((update) => {
    const next = typeof update === 'function' ? update(rollsRef.current) : update
    if (next === rollsRef.current) return
    rollsRef.current = next
    revisionRef.current += 1
    setRollsState(next)
  }, [])
  const getRollsRevision = useCallback(() => revisionRef.current, [])
  return { getRollsRevision, rolls, setRolls }
}
