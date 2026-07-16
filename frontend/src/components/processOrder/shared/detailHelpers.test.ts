import { describe, expect, it } from 'vitest'
import { groupFinishes } from './detailHelpers'

describe('母卷成品规格分组排序', () => {
  it('按成品门幅从小到大排列', () => {
    const groups = groupFinishes([
      { uuid: 'finish-wide', finishWidth: 992, estimateWeight: 500 },
      { uuid: 'finish-narrow', finishWidth: 878, estimateWeight: 400 },
      { uuid: 'finish-middle', finishWidth: 900, estimateWeight: 450 },
    ])

    expect(groups.map((group) => group.width)).toEqual([878, 900, 992])
  })
})
