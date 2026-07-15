import { describe, expect, it } from 'vitest'
import { isRouteNodeActivationKey } from './routeDraftFlowKeyboard'

describe('链式工艺节点键盘操作', () => {
  it('仅使用 Enter 或空格键激活节点', () => {
    expect(isRouteNodeActivationKey('Enter')).toBe(true)
    expect(isRouteNodeActivationKey(' ')).toBe(true)
    expect(isRouteNodeActivationKey('Tab')).toBe(false)
    expect(isRouteNodeActivationKey('ArrowRight')).toBe(false)
  })
})
