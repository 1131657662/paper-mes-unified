import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  clearBackRecordDraft,
  readBackRecordDraft,
  writeBackRecordDraft,
} from './backRecordDraft'

const values = { warehouseUuid: 'warehouse-1', rolls: { roll1: { actualWeight: 998 } } }

describe('backRecordDraft', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('persists each order in an isolated key and restores its version', () => {
    const storage = memoryStorage()
    vi.stubGlobal('localStorage', storage)

    writeBackRecordDraft('order-1', 7, values)

    expect(readBackRecordDraft('order-1')).toMatchObject({
      orderUuid: 'order-1',
      orderVersion: 7,
      values,
    })
    expect(readBackRecordDraft('order-2')).toBeNull()
  })

  it('clears the draft only after persistence succeeds', () => {
    const storage = memoryStorage()
    vi.stubGlobal('localStorage', storage)
    writeBackRecordDraft('order-1', 7, values)

    clearBackRecordDraft('order-1')

    expect(readBackRecordDraft('order-1')).toBeNull()
  })
})

function memoryStorage(): Storage {
  const values = new Map<string, string>()
  return {
    clear: () => values.clear(),
    getItem: (key) => values.get(key) ?? null,
    key: (index) => Array.from(values.keys())[index] ?? null,
    get length() { return values.size },
    removeItem: (key) => { values.delete(key) },
    setItem: (key, value) => { values.set(key, value) },
  }
}
