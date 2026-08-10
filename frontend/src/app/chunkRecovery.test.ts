import { describe, expect, it } from 'vitest'
import { shouldReloadAfterPreloadError } from './chunkRecovery'

describe('懒加载 Chunk 恢复', () => {
  it('没有重试标记时允许刷新', () => {
    expect(shouldReloadAfterPreloadError(null)).toBe(true)
  })

  it('已有重试标记时阻止再次刷新', () => {
    expect(shouldReloadAfterPreloadError('1')).toBe(false)
  })
})
