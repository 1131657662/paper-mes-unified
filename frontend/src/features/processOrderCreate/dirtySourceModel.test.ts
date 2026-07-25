import { describe, expect, it } from 'vitest'
import { addDirtySource, removeDirtySource } from './dirtySourceModel'

describe('新建加工单 dirty 来源', () => {
  it.each(['draft', 'service'] as const)(
    '同时存在两种 dirty 时，清除 %s 不会误清另一来源',
    (source) => {
      let sources = addDirtySource(new Set(), 'draft')
      sources = addDirtySource(sources, 'service')

      const remaining = removeDirtySource(sources, source)

      expect(remaining.size).toBe(1)
      expect(remaining.has(source === 'draft' ? 'service' : 'draft')).toBe(true)
    },
  )
})
