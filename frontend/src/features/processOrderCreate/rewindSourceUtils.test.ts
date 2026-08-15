import { describe, expect, it } from 'vitest'
import { sameSpecSourceIds, sourceOptionsFromRolls } from './rewindSourceUtils'
import type { RollDraft } from './types'

describe('复卷来源母卷筛选', () => {
  it('按实际门幅匹配同规格来源母卷', () => {
    const target = roll('target', 1200, 1180)
    const measuredMatch = roll('match', 1200, 1180)
    const originalWidthOnlyMatch = roll('original-only', 1200, undefined)

    expect(sameSpecSourceIds(target, [target, measuredMatch, originalWidthOnlyMatch]))
      .toEqual(['target', 'match'])
  })

  it('来源标签展示实际门幅', () => {
    const [option] = sourceOptionsFromRolls([roll('roll-1', 1200, 1180)])

    expect(option?.label).toContain('1180 mm')
    expect(option?.label).not.toContain('1200 mm')
  })
})

function roll(localId: string, originalWidth: number, actualWidth?: number): RollDraft {
  return {
    localId,
    uuid: localId,
    paperName: '牛卡纸',
    gramWeight: 265,
    originalWidth,
    actualWidth,
    originalDiameter: 48,
    coreDiameter: 6,
    rollWeight: 850,
    pieceNum: 1,
    processMode: 1,
    mainStepType: 2,
  }
}
