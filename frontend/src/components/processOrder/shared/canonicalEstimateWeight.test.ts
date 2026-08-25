import { describe, expect, it } from 'vitest'
import type { RollProductionVO, StageOutputVO } from '../../../types/processOrder'
import { canonicalFinishEstimateWeights, canonicalStageOutputWeights } from './canonicalEstimateWeight'

function production(overrides: Partial<RollProductionVO> = {}): RollProductionVO {
  return {
    originalUuid: 'roll-1',
    actualWeight: 1000,
    originalWidth: 1000,
    mainStepType: 1,
    steps: [{ uuid: 'step-1', stageLevel: 1, stepType: 1, isMain: 1 }],
    ...overrides,
  }
}

function output(uuid: string, finishWidth: number, overrides: Partial<StageOutputVO> = {}): StageOutputVO {
  return {
    uuid,
    stageLevel: 1,
    outputSort: 1,
    sourceStepType: 1,
    finishWidth,
    estimateWeight: 1,
    ...overrides,
  }
}

describe('canonicalStageOutputWeights', () => {
  it('allocates a 2400mm source as integer 621, 621, 620kg', () => {
    const weights = canonicalStageOutputWeights(
      production({ actualWeight: 1862, originalWidth: 2400 }),
      [
        output('a', 800),
        output('b', 800, { outputSort: 2 }),
        output('c', 800, { outputSort: 3 }),
      ],
    )

    expect(['a', 'b', 'c'].map((key) => weights.get(key))).toEqual([621, 621, 620])
  })

  it('allocates an ALLOCATE width gap to products and keeps explicit trim weight', () => {
    const weights = canonicalStageOutputWeights(
      production({
        steps: [{ uuid: 'step-1', stageLevel: 1, stepType: 1, isMain: 1, widthDifferencePolicy: 'ALLOCATE' }],
      }),
      [
        output('product', 600),
        output('trim', 100, { isRemain: 1, outputSort: 2 }),
      ],
    )

    expect(weights.get('product')).toBe(900)
    expect(weights.get('trim')).toBe(100)
  })

  it('keeps saw ALLOCATE proportional to product widths', () => {
    const weights = canonicalStageOutputWeights(
      production({
        actualWeight: 100,
        steps: [{ uuid: 'step-1', stageLevel: 1, stepType: 1, isMain: 1, widthDifferencePolicy: 'ALLOCATE' }],
      }),
      [output('wide', 600), output('narrow', 300, { outputSort: 2 })],
    )

    expect(weights.get('wide')).toBe(67)
    expect(weights.get('narrow')).toBe(33)
  })

  it('removes LOSS from stage output weights', () => {
    const weights = canonicalStageOutputWeights(
      production({
        steps: [{ uuid: 'step-1', stageLevel: 1, stepType: 1, isMain: 1, widthDifferencePolicy: 'LOSS' }],
      }),
      [output('product', 600)],
    )

    expect(weights.get('product')).toBe(600)
  })

  it('honors a planned LOSS weight even when stage widths are unavailable', () => {
    const weights = canonicalStageOutputWeights(
      production({
        steps: [{ uuid: 'step-1', stageLevel: 1, stepType: 1, isMain: 1,
          widthDifferencePolicy: 'LOSS', plannedLossWeight: 125 }],
      }),
      [output('product', 0, { finishWidth: undefined })],
    )

    expect(weights.get('product')).toBe(875)
  })

  it('keeps REMAINDER as a separate trim output', () => {
    const weights = canonicalStageOutputWeights(
      production(),
      [
        output('product', 600),
        output('trim', 400, { isRemain: 1, outputSort: 2 }),
      ],
    )

    expect(weights.get('product')).toBe(600)
    expect(weights.get('trim')).toBe(400)
  })

  it('keeps saved stage plans instead of flattening repeated rewind layouts', () => {
    const weights = canonicalStageOutputWeights(
      production({ actualWeight: 2403, originalWidth: 1625 }),
      [
        output('product-a', 1550, { estimateWeight: 1146, weightStatus: 'ESTIMATED' }),
        output('trim-a', 75, { estimateWeight: 56, isRemain: 1, outputSort: 2, weightStatus: 'ESTIMATED' }),
        output('product-b', 1550, { estimateWeight: 1146, outputSort: 3, weightStatus: 'ESTIMATED' }),
        output('trim-b', 75, { estimateWeight: 55, isRemain: 1, outputSort: 4, weightStatus: 'ESTIMATED' }),
      ],
    )

    expect(['product-a', 'trim-a', 'product-b', 'trim-b'].map((key) => weights.get(key)))
      .toEqual([1146, 56, 1146, 55])
  })

  it('leaves an unlinked next stage unknown instead of inventing parent coverage', () => {
    const weights = canonicalStageOutputWeights(
      production({ actualWeight: 1200, originalWidth: 1200, steps: [
        { uuid: 'step-1', stageLevel: 1, stepType: 1, isMain: 1, widthDifferencePolicy: 'REMAINDER' },
        { uuid: 'step-2', stageLevel: 2, stepType: 1, isMain: 0, widthDifferencePolicy: 'REMAINDER' },
      ] }),
      [
        output('parent', 1200, { actualWeight: 900 }),
        output('child-a', 600, { stageLevel: 2, outputSort: 1 }),
        output('child-b', 600, { stageLevel: 2, outputSort: 2 }),
      ],
    )

    expect(weights.get('child-a')).toBeUndefined()
    expect(weights.get('child-b')).toBeUndefined()
  })

  it('leaves an unlinked next stage unknown when only a prior stage exists', () => {
    const weights = canonicalStageOutputWeights(
      production({ steps: [
        { uuid: 'step-1', stageLevel: 1, stepType: 1, isMain: 1, widthDifferencePolicy: 'REMAINDER' },
        { uuid: 'step-2', stageLevel: 2, stepType: 1, isMain: 0, widthDifferencePolicy: 'REMAINDER' },
      ] }),
      [
        output('parent-product', 900, { outputStatus: 2 }),
        output('parent-trim', 100, { isRemain: 1, outputSort: 2 }),
        output('child', 900, { stageLevel: 2 }),
      ],
    )

    expect(weights.get('parent-product')).toBe(900)
    expect(weights.get('parent-trim')).toBe(100)
    expect(weights.get('child')).toBeUndefined()
  })

  it('does not fabricate a weight when the source is unknown', () => {
    const weights = canonicalStageOutputWeights(
      production({ actualWeight: undefined, totalWeight: undefined, rollWeight: undefined, weightStatus: 'UNKNOWN' }),
      [output('product', 1000)],
    )

    expect(weights.get('product')).toBeUndefined()
  })

  it('keeps measured stage trim fixed and allocates only the remaining trim budget', () => {
    const weights = canonicalStageOutputWeights(
      production({ originalWidth: 1000 }),
      [
        output('product', 500),
        output('trim', 500, { isRemain: 1, outputSort: 2, actualWeight: 100 }),
      ],
    )

    expect(weights.get('product')).toBe(900)
    expect(weights.has('trim')).toBe(false)
  })

  it('keeps measured outputs fixed when stage widths are unavailable', () => {
    const weights = canonicalStageOutputWeights(
      production({ originalWidth: undefined, actualWidth: undefined }),
      [
        output('product', 0, { finishWidth: undefined }),
        output('trim', 0, { finishWidth: undefined, isRemain: 1, outputSort: 2, actualWeight: 100 }),
      ],
    )

    expect(weights.get('product')).toBe(900)
    expect(weights.has('trim')).toBe(false)
  })
})

describe('canonicalFinishEstimateWeights', () => {
  it('keeps saved plans for each repeated rewind layout', () => {
    const weights = canonicalFinishEstimateWeights({
      production: production({ actualWeight: 2403, originalWidth: 1625, mainStepType: 2 }),
      finishes: [
        { uuid: 'product-a', finishRollNo: 'F001', finishWidth: 1550, estimateWeight: 1146 },
        { uuid: 'trim-a', finishRollNo: 'F002', finishWidth: 75, estimateWeight: 56, isRemain: 1 },
        { uuid: 'product-b', finishRollNo: 'F003', finishWidth: 1550, estimateWeight: 1146 },
        { uuid: 'trim-b', finishRollNo: 'F004', finishWidth: 75, estimateWeight: 55, isRemain: 1 },
      ],
    })

    expect(['product-a', 'trim-a', 'product-b', 'trim-b'].map((key) => weights.get(key)))
      .toEqual([1146, 56, 1146, 55])
  })

  it('reserves measured trim weight before allocating formal products', () => {
    const weights = canonicalFinishEstimateWeights({
      production: production({ originalWidth: 1000, mainStepType: 1 }),
      finishes: [
        { uuid: 'product-a', finishWidth: 500 },
        { uuid: 'product-b', finishWidth: 400 },
        { uuid: 'trim', finishWidth: 100, isRemain: 1, actualWeight: 20 },
      ],
    })

    expect(weights.get('product-a')).toBe(544)
    expect(weights.get('product-b')).toBe(436)
    expect(weights.get('trim')).toBe(20)
  })

  it('locks measured products and allocates only the remaining product weight', () => {
    const weights = canonicalFinishEstimateWeights({
      production: production({ originalWidth: 1000, mainStepType: 1 }),
      finishes: [
        { uuid: 'measured', finishWidth: 500, actualWeight: 700 },
        { uuid: 'unknown', finishWidth: 500 },
      ],
    })

    expect(weights.get('measured')).toBe(700)
    expect(weights.get('unknown')).toBe(300)
  })

  it('locks measured trim when a stage has no formal product row', () => {
    const weights = canonicalFinishEstimateWeights({
      production: production({ originalWidth: 1000, mainStepType: 1 }),
      finishes: [
        { uuid: 'measured-trim', finishWidth: 500, isRemain: 1, actualWeight: 250 },
        { uuid: 'unknown-trim', finishWidth: 500, isRemain: 1 },
      ],
    })

    expect(weights.get('measured-trim')).toBe(250)
    expect(weights.get('unknown-trim')).toBe(750)
  })

  it('scales planned loss by a merged source consumption ratio', () => {
    const weights = canonicalFinishEstimateWeights({
      production: production({
        actualWeight: 1000,
        steps: [{ uuid: 'loss-step', originalUuid: 'roll-1', widthDifferencePolicy: 'LOSS', plannedLossWeight: 100 }],
      }),
      finishes: [{
        uuid: 'product',
        finishWidth: 1000,
        sources: [{ originalUuid: 'roll-1', consumeRatio: 50 }],
      }],
    })

    expect(weights.get('product')).toBe(450)
  })

  it('treats a legacy source relation without a ratio as full consumption', () => {
    const weights = canonicalFinishEstimateWeights({
      production: production({ actualWeight: 1000 }),
      finishes: [
        { uuid: 'partial', finishWidth: 500, sources: [{ originalUuid: 'roll-1', consumeRatio: 50 }] },
        { uuid: 'legacy', finishWidth: 500, sources: [{ originalUuid: 'roll-1' }] },
      ],
    })

    expect(weights.get('partial')).toBe(500)
    expect(weights.get('legacy')).toBe(500)
  })

  it('allocates each finish source combination from its own consumed source budget', () => {
    const first = production({ originalUuid: 'source-a', actualWeight: 1000 })
    const second = production({ originalUuid: 'source-b', actualWeight: 1000 })
    const partialSource = { originalUuid: 'source-a', consumeRatio: 50 }
    const remainingSource = { originalUuid: 'source-a' }
    const secondSource = { originalUuid: 'source-b', consumeRatio: 100 }
    const finishes = [
      { uuid: 'partial', finishWidth: 500, sources: [partialSource] },
      { uuid: 'merged', finishWidth: 500, sources: [remainingSource, secondSource] },
    ]

    const weights = canonicalFinishEstimateWeights({
      production: first,
      finishes,
      sourceProductions: [first, second],
    })

    expect(weights.get('partial')).toBe(500)
    expect(weights.get('merged')).toBe(1500)
  })

  it('resolves legacy ratios from the complete production chain when rendering one group', () => {
    const first = production({ originalUuid: 'source-a', actualWeight: 1000 })
    const second = production({ originalUuid: 'source-b', actualWeight: 1000 })
    const partial = {
      uuid: 'partial', finishWidth: 500,
      sources: [{ originalUuid: 'source-a', consumeRatio: 50 }],
    }
    const merged = {
      uuid: 'merged', finishWidth: 500,
      sources: [{ originalUuid: 'source-a' }, { originalUuid: 'source-b', consumeRatio: 100 }],
    }
    first.finishes = [partial, merged]

    const weights = canonicalFinishEstimateWeights({
      production: first,
      finishes: [merged],
      sourceProductions: [first, second],
    })

    expect(weights.get('merged')).toBe(1500)
  })

  it('rejects explicit source consumption above one hundred percent', () => {
    const first = production({ originalUuid: 'source-a', actualWeight: 1000 })
    const finishes = [
      { uuid: 'first', finishWidth: 500, sources: [{ originalUuid: 'source-a', consumeRatio: 60 }] },
      { uuid: 'second', finishWidth: 500, sources: [{ originalUuid: 'source-a', consumeRatio: 60 }] },
    ]

    expect(() => canonicalFinishEstimateWeights({ production: first, finishes }))
      .toThrow('来源消耗比例合计不能超过100%')
  })

  it('derives trim budget from width before allocating product remainder', () => {
    const weights = canonicalFinishEstimateWeights({
      production: production({ originalWidth: 1000, mainStepType: 1 }),
      finishes: [
        { uuid: 'product', finishWidth: 900 },
        { uuid: 'trim', finishWidth: 100, isRemain: 1 },
      ],
    })

    expect(weights.get('product')).toBe(900)
    expect(weights.get('trim')).toBe(100)
  })

  it('includes an unlisted remainder width in the trim budget', () => {
    const weights = canonicalFinishEstimateWeights({
      production: production({ originalWidth: 1000, mainStepType: 1 }),
      finishes: [
        { uuid: 'product', finishWidth: 600 },
        { uuid: 'trim', finishWidth: 100, isRemain: 1 },
      ],
    })

    expect(weights.get('product')).toBe(600)
    expect(weights.get('trim')).toBe(400)
  })

  it('ignores a stale unmeasured trim estimate under ALLOCATE policy', () => {
    const weights = canonicalFinishEstimateWeights({
      production: production({
        originalWidth: 1000,
        mainStepType: 1,
        steps: [{ uuid: 'saw', stepType: 1, isMain: 1, widthDifferencePolicy: 'ALLOCATE' }],
      }),
      finishes: [
        { uuid: 'product', finishWidth: 600 },
        { uuid: 'trim', finishWidth: 100, isRemain: 1, estimateWeight: 400 },
      ],
    })

    expect(weights.get('product')).toBe(900)
    expect(weights.get('trim')).toBe(100)
  })

  it('does not fabricate product weight when trim has no measurable basis', () => {
    const weights = canonicalFinishEstimateWeights({
      production: production({ originalWidth: 1000, mainStepType: 1 }),
      finishes: [
        { uuid: 'product', finishWidth: 1000 },
        { uuid: 'trim', isRemain: 1 },
      ],
    })

    expect(weights.get('product')).toBeUndefined()
    expect(weights.get('trim')).toBeUndefined()
  })

  it('does not round a fractional measured remainder into a false product estimate', () => {
    const weights = canonicalFinishEstimateWeights({
      production: production({ actualWeight: 1000, originalWidth: 1000, mainStepType: 1 }),
      finishes: [
        { uuid: 'measured', finishWidth: 500, actualWeight: 333.4 },
        { uuid: 'unknown', finishWidth: 500 },
      ],
    })

    expect(weights.get('measured')).toBe(333.4)
    expect(weights.get('unknown')).toBeUndefined()
  })

  it('does not round a fractional measured stage remainder into a false estimate', () => {
    const weights = canonicalStageOutputWeights(
      production({ actualWeight: 1000, originalWidth: 1000 }),
      [
        output('measured', 500, { actualWeight: 333.4 }),
        output('unknown', 500, { outputSort: 2 }),
      ],
    )

    expect(weights.get('unknown')).toBeUndefined()
  })
})
