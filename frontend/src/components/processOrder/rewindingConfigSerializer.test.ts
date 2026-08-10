import { describe, expect, it } from 'vitest'
import { buildDefaultSegments } from './rewindingConfigModel'
import { toOnSiteRewindingConfig, toRewindingConfig } from './rewindingConfigSerializer'

describe('rewinding config serialization', () => {
  it('serializes standard mode segments and preview finishes', () => {
    const segments = buildDefaultSegments(1200, 'roll-1', 2)

    const config = toRewindingConfig({
      nextPreview: { finishes: [{ finishWidth: 600, estimateWeight: 100 }] },
      nextRewindMode: 2,
      nextSpareCount: 1,
      nextSegments: segments,
      nextUnitPrice: 25,
      processMode: 1,
    })

    expect(config.finishSpecs).toEqual([{ count: 1, finishWidth: 600, estimateWeight: 100 }])
    expect(config.rewindSegments).toHaveLength(1)
    expect(config.spareCount).toBe(1)
  })

  it('serializes an on-site finish count without standard segments', () => {
    const config = toOnSiteRewindingConfig({
      count: 3,
      processMode: 2,
      rewindMode: 2,
      spareCount: 1,
      unitPrice: 20,
    })

    expect(config.finishSpecs).toEqual([{
      count: 3,
      estimateWeight: 0,
      finishCoreDiameter: 0,
      finishDiameter: 0,
      finishWidth: 0,
    }])
    expect(config.rewindSegments).toBeUndefined()
  })
})
