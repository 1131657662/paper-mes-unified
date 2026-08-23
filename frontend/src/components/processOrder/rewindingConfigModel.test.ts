import { describe, expect, it } from 'vitest'
import type { FinishPreviewVO, OriginalRoll } from '../../types/processOrder'
import {
  buildDefaultSegments,
  buildInitialSegments,
  buildSameSpecSegments,
  equalizeSourceRatios,
  toCm,
  toFinishSpecs,
  toInch,
  toPreviewDto,
} from './rewindingConfigModel'

const roll = (overrides: Partial<OriginalRoll> = {}): OriginalRoll => ({
  uuid: 'roll-1',
  originalWidth: 1200,
  originalDiameter: 20,
  coreDiameter: 6,
  ...overrides,
})

describe('rewindingConfigModel', () => {
  it('converts stored inch diameters to form centimeters and back', () => {
    expect(toCm(20)).toBe(51)
    expect(toInch(51)).toBe(20)
    expect(toCm(undefined)).toBeUndefined()
  })

  it('builds a default segment using the original width for mode two', () => {
    const [segment] = buildDefaultSegments(1200, 'roll-1', 2)

    expect(segment?.layoutItems[0]?.width).toBe(1200)
    expect(segment?.sources).toEqual([{ originalUuid: 'roll-1', shareRatio: 100 }])
  })

  it('hydrates configured segments and converts their ratio to form percent', () => {
    const [segment] = buildInitialSegments(roll(), {
      processMode: 1,
      rewindMode: 3,
      rewindSegments: [{
        segmentSort: 2,
        segmentRatio: 0.5,
        targetDiameter: 10,
        layoutItems: [{ width: 400, itemType: 'TRIM' }],
      }],
    })

    expect(segment?.segmentSort).toBe(2)
    expect(segment?.segmentRatio).toBe(50)
    expect(segment?.targetDiameter).toBe(25)
    expect(segment?.layoutItems[0]?.itemType).toBe('TRIM')
  })

  it('creates a same-spec segment from the original roll', () => {
    const [segment] = buildSameSpecSegments(roll())

    expect(segment?.targetDiameter).toBe(51)
    expect(segment?.finishCoreDiameter).toBe(6)
    expect(segment?.layoutItems[0]?.width).toBe(1200)
  })

  it('preserves source consume ratios when editing and submitting a plan', () => {
    const [segment] = buildInitialSegments(roll(), {
      processMode: 1,
      rewindMode: 5,
      rewindSegments: [{
        segmentSort: 1,
        segmentRatio: 1,
        sources: [{ originalUuid: 'roll-1', shareRatio: 100, consumeRatio: 50 }],
        layoutItems: [{ width: 1200, itemType: 'FINISH' }],
      }],
    })

    expect(segment?.sources[0]?.consumeRatio).toBe(50)
    expect(toPreviewDto(5, 0, [segment!]).segments?.[0]?.sources?.[0]?.consumeRatio).toBe(50)
  })

  it('uses the measured width and does not invent a paper core for same-spec mode', () => {
    const [segment] = buildSameSpecSegments(roll({ actualWidth: 1180, coreDiameter: undefined }))

    expect(segment?.layoutItems[0]?.width).toBe(1180)
    expect(segment?.finishCoreDiameter).toBeUndefined()
  })

  it('rebuilds an existing same-spec config from the current source specification', () => {
    const [segment] = buildInitialSegments(roll({ actualWidth: 1180 }), {
      processMode: 1,
      rewindMode: 6,
      rewindSegments: [{ layoutItems: [{ width: 1200, itemType: 'FINISH' }] }],
    })

    expect(segment?.layoutItems[0]?.width).toBe(1180)
  })

  it('equalizes source ratios in one update while preserving a total of 100', () => {
    const sources = equalizeSourceRatios([
      { originalUuid: 'roll-1', shareRatio: 0 },
      { originalUuid: 'roll-2', shareRatio: 0 },
      { originalUuid: 'roll-3', shareRatio: 0 },
    ])

    expect(sources.map((source) => source.shareRatio)).toEqual([33.33, 33.33, 33.34])
    expect(sources.reduce((total, source) => total + source.shareRatio, 0)).toBe(100)
  })

  it('normalizes one segment ratio and injects layers for layered mode', () => {
    const [segment] = buildDefaultSegments(1200, 'roll-1', 4)
    const preview = toPreviewDto(4, 2, [{
      ...segment!,
      targetDiameter: 51,
      layoutItems: [{ ...segment!.layoutItems[0]!, itemType: 'FINISH' }],
    }])

    expect(preview.segments?.[0]?.segmentRatio).toBe(1)
    expect(preview.segments?.[0]?.targetDiameter).toBe(20)
    expect(preview.segments?.[0]?.layoutItems?.[0]?.layers).toEqual([{ outDiameter: 20, coreDiameter: 3 }])
  })

  it('generates fallback finish specs while excluding trim items', () => {
    const [segment] = buildDefaultSegments(1200, 'roll-1', 2)
    const specs = toFinishSpecs(null, [{
      ...segment!,
      targetDiameter: 51,
      repeatCount: 2,
      layoutItems: [
        { ...segment!.layoutItems[0]!, width: 400, quantity: 2, itemType: 'FINISH' },
        { ...segment!.layoutItems[0]!, width: 100, quantity: 1, itemType: 'TRIM' },
      ],
    }])

    expect(specs).toHaveLength(4)
    expect(specs.every((spec) => spec.finishWidth === 400)).toBe(true)
  })

  it('uses server preview finishes when available', () => {
    const preview: FinishPreviewVO = { finishes: [{ finishWidth: 300, estimateWeight: 12 }] }

    expect(toFinishSpecs(preview, [])).toEqual([{
      count: 1,
      finishWidth: 300,
      estimateWeight: 12,
    }])
  })
})
