import { describe, expect, it } from 'vitest'
import {
  normalizeRewindPlan,
  planWithRewindMode,
  sameSpecRewindError,
  sameSpecRewindPlan,
  segmentRatioPercent,
} from './rewindLayerPlanUtils'
import type { RollDraft } from './types'

describe('sameSpecRewindPlan', () => {
  it('creates exactly one unchanged finish from the source roll', () => {
    const roll = {
      localId: 'local-1',
      uuid: 'roll-1',
      paperName: '牛卡纸',
      gramWeight: 265,
      originalWidth: 1702,
      originalDiameter: 48,
      coreDiameter: 6,
      rollWeight: 850,
      pieceNum: 1,
      processMode: 1,
      mainStepType: 2,
    } satisfies RollDraft

    const result = sameSpecRewindPlan({ processMode: 1, rewindMode: 3, segments: [] }, roll)

    expect(result.rewindMode).toBe(6)
    expect(result.segments).toEqual([{
      segmentSort: 1,
      segmentRatio: 1,
      targetDiameter: 48,
      finishCoreDiameter: 6,
      repeatCount: 1,
      sources: [{ originalUuid: 'roll-1', shareRatio: 100, consumeRatio: 100, sourceSort: 1 }],
      layoutItems: [{ width: 1702, quantity: 1, itemType: 'FINISH' }],
    }])
  })

  it('uses the measured width when rebuilding a same-spec plan', () => {
    const roll = {
      localId: 'local-1', uuid: 'roll-1', paperName: '牛卡纸', gramWeight: 265,
      originalWidth: 1702, actualWidth: 1680, originalDiameter: 48, coreDiameter: 6,
      rollWeight: 850, pieceNum: 1, processMode: 1, mainStepType: 2,
    } satisfies RollDraft

    const result = sameSpecRewindPlan({ processMode: 1 }, roll)

    expect(result.segments?.[0]?.layoutItems?.[0]?.width).toBe(1680)
  })

  it('uses the measured width for a diameter-only plan', () => {
    const roll = {
      localId: 'local-1', uuid: 'roll-1', paperName: '牛卡纸', gramWeight: 265,
      originalWidth: 1702, actualWidth: 1680, originalDiameter: 48, coreDiameter: 6,
      rollWeight: 850, pieceNum: 1, processMode: 1, mainStepType: 2,
    } satisfies RollDraft

    const result = planWithRewindMode({ processMode: 1 }, roll, 2)

    expect(result.segments?.[0]?.layoutItems?.[0]?.width).toBe(1680)
  })

  it('blocks same-spec mode when a source specification is missing', () => {
    expect(sameSpecRewindError({ originalDiameter: 48, coreDiameter: undefined })).toContain('纸芯')
    expect(sameSpecRewindError({ originalDiameter: undefined, coreDiameter: 6 })).toContain('直径')
    expect(sameSpecRewindError({ originalDiameter: 48, coreDiameter: 6 })).toBeUndefined()
  })
})

describe('复卷模式方案归一化', () => {
  const source = roll()

  it('分层模式把旧分段卷径迁入分层并清除冲突字段', () => {
    const result = planWithRewindMode(plan(), source, 4)
    const segment = result.segments![0]!

    expect(segment?.targetDiameter).toBeUndefined()
    expect(segment?.finishCoreDiameter).toBeUndefined()
    expect(segment.layoutItems?.[0]?.layers).toEqual([{ outDiameter: 1200, coreDiameter: 3 }])
  })

  it('改直径模式保留客户销售规格并固定物理门幅', () => {
    const result = planWithRewindMode(plan(), source, 2)
    const item = result.segments![0]!.layoutItems![0]!

    expect(item).toMatchObject({
      width: 1702,
      quantity: 1,
      customerGramWeight: 275,
      customerSpecOverrideReason: '合同规格',
    })
  })

  it.each([2, 3])('分层模式切换到模式 %s 时迁回外径和纸芯', (mode) => {
    const layered = planWithRewindMode(plan(), source, 4)
    layered.segments![0]!.layoutItems![0]!.layers = [
      { outDiameter: 900, coreDiameter: 76 },
      { outDiameter: 1100, coreDiameter: 152 },
    ]

    const segment = planWithRewindMode(layered, source, mode).segments![0]!

    expect(segment.targetDiameter).toBe(1100)
    expect(segment.finishCoreDiameter).toBe(76)
    expect(segment.layoutItems?.[0]?.layers).toBeUndefined()
  })

  it('改门幅模式清除不再生效的卷径和分层参数', () => {
    const result = normalizeRewindPlan({ ...plan(), rewindMode: 1 }, source)
    const segment = result.segments![0]!

    expect(segment?.targetDiameter).toBeUndefined()
    expect(segment?.finishCoreDiameter).toBeUndefined()
    expect(segment.layoutItems?.[0]?.layers).toBeUndefined()
  })

  it('分段输入按权重计算百分比，单段恒为100%', () => {
    const segments = [{ segmentRatio: 1 }, { segmentRatio: 3 }]

    expect(segmentRatioPercent(segments[0]!, segments)).toBe(25)
    expect(segmentRatioPercent(segments[1]!, segments)).toBe(75)
    expect(segmentRatioPercent({ segmentRatio: 8 }, [{ segmentRatio: 8 }])).toBe(100)
  })

  it('删除后重新新增分段时按当前列表顺序重排编号', () => {
    const base = plan().segments[0]!
    const result = normalizeRewindPlan({
      ...plan(),
      rewindMode: 1,
      segments: [{ ...base, segmentSort: 3 }, { ...base, segmentSort: 3 }],
    }, source)

    expect(result.segments?.map((segment) => segment.segmentSort)).toEqual([1, 2])
  })
})

function roll(): RollDraft {
  return {
    localId: 'local-1', uuid: 'roll-1', paperName: '牛卡纸', gramWeight: 265,
    originalWidth: 1702, originalDiameter: 1300, coreDiameter: 76,
    rollWeight: 850, pieceNum: 1, processMode: 1, mainStepType: 2,
  }
}

function plan() {
  return {
    processMode: 1,
    mainStepType: 2,
    rewindMode: 3,
    segments: [{
      segmentRatio: 1,
      targetDiameter: 1200,
      finishCoreDiameter: 3,
      repeatCount: 1,
      layoutItems: [{
        width: 800,
        quantity: 2,
        itemType: 'FINISH' as const,
        customerGramWeight: 275,
        customerSpecOverrideReason: '合同规格',
      }],
    }],
  }
}
