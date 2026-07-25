import { describe, expect, it, vi } from 'vitest'
import { runVersionSynchronizedMutation } from './serviceMutationCommit'

describe('附加工艺 mutation 提交顺序', () => {
  it('先确认版本可用，再提交 mutation，最后同步新版本', async () => {
    const events: string[] = []

    await runVersionSynchronizedMutation({
      ensureVersionReady: async () => { events.push('ensure') },
      markVersionSyncRequired: () => { events.push('blocked') },
      mutate: async () => { events.push('mutate'); return 'saved' },
      synchronizeVersion: async () => { events.push('sync') },
    })

    expect(events).toEqual(['ensure', 'mutate', 'blocked', 'sync'])
  })

  it('版本同步失败时向调用方抛错以保留编辑器 dirty', async () => {
    const syncError = new Error('version sync failed')
    const mutation = vi.fn(async () => 'saved')

    await expect(runVersionSynchronizedMutation({
      ensureVersionReady: async () => undefined,
      markVersionSyncRequired: () => undefined,
      mutate: mutation,
      synchronizeVersion: async () => { throw syncError },
    })).rejects.toBe(syncError)
    expect(mutation).toHaveBeenCalledOnce()
  })
})
