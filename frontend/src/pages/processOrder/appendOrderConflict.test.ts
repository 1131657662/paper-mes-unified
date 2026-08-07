import { describe, expect, it } from 'vitest'
import {
  isAppendOrderVersionOnlyConflict,
  mergeAppendConflictRolls,
  summarizeAppendConflict,
} from './appendOrderConflict'
import type { ProcessOrderAppendSessionVO } from '../../types/processOrder'

describe('appendOrderConflict', () => {
  it('treats an order-only version change separately from roll conflicts', () => {
    const previous = { ...session([]), sessionVersion: 3, currentOrderVersion: 8 }
    const latest = { ...session([]), sessionVersion: 3, currentOrderVersion: 9 }

    expect(isAppendOrderVersionOnlyConflict(previous, latest)).toBe(true)
  })

  it('summarizes remote changes and keeps local input during recovery', () => {
    const previous = session([{ uuid: 'roll-1', paperName: 'A', rollWeight: 100 }])
    const latest = session([
      { uuid: 'roll-1', paperName: 'A', rollWeight: 110 },
      { uuid: 'roll-2', paperName: 'B', rollWeight: 200 },
    ])
    const local = [{
      uuid: 'roll-1', localId: 'roll-1', paperName: 'A', gramWeight: 80,
      originalWidth: 1000, rollWeight: 105,
    }]

    expect(summarizeAppendConflict(previous, latest)).toEqual({ added: 1, changed: 1, removed: 0 })
    expect(mergeAppendConflictRolls(previous, local, latest)).toHaveLength(2)
    expect(mergeAppendConflictRolls(previous, local, latest)[0]?.rollWeight).toBe(105)
  })

  it('turns a remotely removed local roll into a recoverable new row', () => {
    const previous = session([{ uuid: 'roll-1', paperName: 'A', rollWeight: 100 }])
    const local = [{
      uuid: 'roll-1', localId: 'roll-1', paperName: 'A', gramWeight: 80,
      originalWidth: 1000, rollWeight: 105,
    }]

    const [recovered] = mergeAppendConflictRolls(previous, local, session([]))

    expect(recovered?.uuid).toBeUndefined()
    expect(recovered?.rollWeight).toBe(105)
  })
})

function session(rolls: ProcessOrderAppendSessionVO['rolls']): ProcessOrderAppendSessionVO {
  return { sessionUuid: 'session-1', orderUuid: 'order-1', rolls }
}
