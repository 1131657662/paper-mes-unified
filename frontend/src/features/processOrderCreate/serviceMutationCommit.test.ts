import { describe, expect, it, vi } from 'vitest'
import { BizError } from '../../api/request'
import { runVersionSynchronizedMutation } from './serviceMutationCommit'

vi.mock('antd', () => ({
  message: { error: vi.fn(), success: vi.fn() },
}))

describe('附加工艺 mutation 提交顺序', () => {
  it('先确认版本可用，再提交 mutation，最后同步新版本', async () => {
    const events: string[] = []

    await runVersionSynchronizedMutation({
      ensureVersionReady: async () => { events.push('ensure') },
      clearVersionSyncRequired: () => { events.push('clear') },
      markVersionSyncRequired: () => { events.push('blocked') },
      mutate: async () => { events.push('mutate'); return 'saved' },
      synchronizeVersion: async () => { events.push('sync') },
    })

    expect(events).toEqual(['ensure', 'blocked', 'mutate', 'sync', 'clear'])
  })

  it('版本同步失败时向调用方抛错以保留编辑器 dirty', async () => {
    const syncError = new Error('version sync failed')
    const mutation = vi.fn(async () => 'saved')

    await expect(runVersionSynchronizedMutation({
      ensureVersionReady: async () => undefined,
      clearVersionSyncRequired: () => undefined,
      markVersionSyncRequired: () => undefined,
      mutate: mutation,
      synchronizeVersion: async () => { throw syncError },
    })).rejects.toBe(syncError)
    expect(mutation).toHaveBeenCalledOnce()
  })

  it('recovers an additional-process write confirmed by the refreshed detail', async () => {
    const events: string[] = []
    const result = await runVersionSynchronizedMutation({
      clearVersionSyncRequired: () => { events.push('clear') },
      ensureVersionReady: async () => { events.push('ensure') },
      isAppliedAfterSync: () => true,
      markVersionSyncRequired: () => { events.push('blocked') },
      mutate: async () => {
        events.push('mutate')
        throw new BizError('Bad Gateway', 502)
      },
      recoverResult: () => 'recovered',
      synchronizeVersion: async () => { events.push('sync') },
    })

    expect(result).toBe('recovered')
    expect(events).toEqual(['ensure', 'blocked', 'mutate', 'sync', 'clear'])
  })

  it('waits for a later refresh when the server commits after the request timeout', async () => {
    vi.useFakeTimers()
    try {
      const events: string[] = []
      const synchronizeVersion = vi.fn(async () => { events.push('sync') })
      const resultPromise = runVersionSynchronizedMutation({
        clearVersionSyncRequired: () => { events.push('clear') },
        ensureVersionReady: async () => { events.push('ensure') },
        isAppliedAfterSync: () => synchronizeVersion.mock.calls.length >= 2,
        markVersionSyncRequired: () => { events.push('blocked') },
        mutate: async () => { throw new BizError('timeout', 502) },
        recoverResult: () => 'recovered',
        synchronizeVersion,
      })

      await vi.advanceTimersByTimeAsync(500)

      await expect(resultPromise).resolves.toBe('recovered')
      expect(events).toEqual(['ensure', 'blocked', 'sync', 'sync', 'clear'])
    } finally {
      vi.useRealTimers()
    }
  })

  it('keeps version synchronization blocked when every refresh is inconclusive', async () => {
    vi.useFakeTimers()
    try {
      const error = new BizError('timeout', 502)
      const clearVersionSyncRequired = vi.fn()
      const resultPromise = runVersionSynchronizedMutation({
        clearVersionSyncRequired,
        ensureVersionReady: async () => undefined,
        isAppliedAfterSync: () => false,
        markVersionSyncRequired: () => undefined,
        mutate: async () => { throw error },
        synchronizeVersion: async () => undefined,
      })
      const rejection = expect(resultPromise).rejects.toBe(error)

      await vi.runAllTimersAsync()

      await rejection
      expect(clearVersionSyncRequired).not.toHaveBeenCalled()
    } finally {
      vi.useRealTimers()
    }
  })
})
