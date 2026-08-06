import { describe, expect, it, vi } from 'vitest'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { refreshBackRecordBeforeSubmit } from './backRecordSubmissionFreshness'

describe('回录提交前详情刷新', () => {
  it('服务端版本未变化时返回最新详情', async () => {
    const current = detail(14)

    const result = await refreshBackRecordBeforeSubmit({
      expectedVersion: current.order.version,
      onConflictReloaded: vi.fn(),
      onRefetch: async () => ({ data: detail(14), isSuccess: true }),
    })

    expect(result).toEqual({
      status: 'current',
      detail: expect.objectContaining({ order: expect.objectContaining({ version: 14 }) }),
    })
  })

  it('查询缓存已刷新但表单仍是旧版本时保留当前值并阻止提交', async () => {
    const onConflictReloaded = vi.fn()

    const result = await refreshBackRecordBeforeSubmit({
      expectedVersion: 14,
      onConflictReloaded,
      onRefetch: async () => ({ data: detail(15), isSuccess: true }),
    })

    expect(result.status).toBe('changed')
    expect(onConflictReloaded).toHaveBeenCalledWith(expect.objectContaining({
      order: expect.objectContaining({ version: 15 }),
    }))
  })

  it('详情刷新失败时不冒充当前版本', async () => {
    const error = new Error('network unavailable')

    const result = await refreshBackRecordBeforeSubmit({
      expectedVersion: 14,
      onConflictReloaded: vi.fn(),
      onRefetch: async () => ({ error, isSuccess: false }),
    })

    expect(result).toEqual({ status: 'failed', error })
  })

  it('详情刷新直接抛错时转换为可处理的失败结果', async () => {
    const error = new Error('request rejected')

    const result = await refreshBackRecordBeforeSubmit({
      expectedVersion: 14,
      onConflictReloaded: vi.fn(),
      onRefetch: async () => { throw error },
    })

    expect(result).toEqual({ status: 'failed', error })
  })

  it('刷新服务端版本前先持久化当前表单', async () => {
    const events: string[] = []

    await refreshBackRecordBeforeSubmit({
      expectedVersion: 14,
      onBeforeRefetch: () => events.push('persist'),
      onConflictReloaded: vi.fn(),
      onRefetch: async () => {
        events.push('refetch')
        return { data: detail(14), isSuccess: true }
      },
    })

    expect(events).toEqual(['persist', 'refetch'])
  })

})

function detail(version: number): ProcessOrderDetailVO {
  return {
    order: { uuid: 'order-1', version },
    originalRolls: [],
    rolls: [],
    finishRolls: [],
    steps: [],
  }
}
