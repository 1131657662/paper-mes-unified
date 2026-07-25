import { describe, expect, it } from 'vitest'
import { ORDER_STATUS } from '../../../constants/processOrder'

describe('最新动态状态颜色', () => {
  it('加工中和已结算使用不同颜色', () => {
    expect(ORDER_STATUS[2]?.timelineColor).toBe('blue')
    expect(ORDER_STATUS[5]?.timelineColor).toBe('purple')
  })
})
