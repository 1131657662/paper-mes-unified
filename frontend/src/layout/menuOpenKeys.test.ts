import { describe, expect, it } from 'vitest'
import { defaultOpenMenuKeys } from './menuOpenKeys'

describe('default open menu keys', () => {
  it.each([
    ['/reports/inventory', ['reports']],
    ['/reports/management/metrics', ['reports', 'report-management']],
    ['/delivery-orders/inventory', ['delivery']],
    ['/customers/customer-1', ['base']],
    ['/operation-logs', ['system']],
    ['/dashboard', []],
  ])('opens only the group owning %s', (pathname, expected) => {
    expect(defaultOpenMenuKeys(pathname)).toEqual(expected)
  })
})
