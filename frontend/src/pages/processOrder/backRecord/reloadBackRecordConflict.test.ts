import { describe, expect, it, vi } from 'vitest'
import { reloadBackRecordConflict } from './reloadBackRecordConflict'

describe('reload back-record conflict', () => {
  it('clears dirty only after the latest server data reloads successfully', async () => {
    const onPersisted = vi.fn()
    const onResetInitialization = vi.fn()

    const result = await reloadBackRecordConflict({
      onPersisted,
      onRefetch: async () => ({ data: detail(), isSuccess: true }),
      onReloaded: vi.fn(),
      onResetInitialization,
    })

    expect(result.reloaded).toBe(true)
    expect(onResetInitialization).toHaveBeenCalledOnce()
    expect(onPersisted).toHaveBeenCalledOnce()
  })

  it('preserves dirty when the latest server data cannot be reloaded', async () => {
    const error = new Error('network unavailable')
    const onPersisted = vi.fn()
    const onResetInitialization = vi.fn()

    const result = await reloadBackRecordConflict({
      onPersisted,
      onRefetch: async () => ({ error, isSuccess: false }),
      onReloaded: vi.fn(),
      onResetInitialization,
    })

    expect(result).toEqual({ error, reloaded: false })
    expect(onPersisted).not.toHaveBeenCalled()
    expect(onResetInitialization).not.toHaveBeenCalled()
  })
})

function detail() {
  return { order: { uuid: 'order-1' }, originalRolls: [] }
}
