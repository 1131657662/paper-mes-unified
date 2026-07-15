import { describe, expect, it } from 'vitest'
import { preventUnsavedUnload, shouldBlockUnsavedNavigation } from './useUnsavedChangesGuard'

const formLocation = { hash: '', pathname: '/papers/create', search: '' }

describe('未保存表单离开保护', () => {
  it('仅在表单已修改且目标地址变化时阻止站内导航', () => {
    expect(shouldBlockUnsavedNavigation(false, formLocation, { ...formLocation, pathname: '/papers' })).toBe(false)
    expect(shouldBlockUnsavedNavigation(true, formLocation, formLocation)).toBe(false)
    expect(shouldBlockUnsavedNavigation(true, formLocation, { ...formLocation, pathname: '/papers' })).toBe(true)
    expect(shouldBlockUnsavedNavigation(true, formLocation, { ...formLocation, search: '?source=menu' })).toBe(true)
  })

  it('刷新或关闭页面时触发浏览器原生确认机制', () => {
    let prevented = false
    const event = {
      preventDefault: () => { prevented = true },
      returnValue: undefined,
    } as unknown as BeforeUnloadEvent

    preventUnsavedUnload(event)

    expect(prevented).toBe(true)
    expect(event.returnValue).toBe('')
  })
})
