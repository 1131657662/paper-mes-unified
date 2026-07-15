import { describe, expect, it } from 'vitest'
import { PERMISSION_ITEMS, getRoleProfile, roleHasPermission } from './permissionMeta'
import { PERMISSIONS } from './permissions'

describe('岗位权限矩阵', () => {
  it('管理员拥有界面中声明的全部权限', () => {
    expect(PERMISSION_ITEMS.every(({ code }) => roleHasPermission('admin', code))).toBe(true)
  })

  it('制单员可以制单和下发但不能生产回录', () => {
    expect(roleHasPermission('order_clerk', PERMISSIONS.orderCreate)).toBe(true)
    expect(roleHasPermission('order_clerk', PERMISSIONS.orderManage)).toBe(true)
    expect(roleHasPermission('order_clerk', PERMISSIONS.orderBackRecord)).toBe(false)
  })

  it('回录员只能执行生产回录相关操作', () => {
    expect(roleHasPermission('recorder', PERMISSIONS.orderBackRecord)).toBe(true)
    expect(roleHasPermission('recorder', PERMISSIONS.orderCreate)).toBe(false)
    expect(roleHasPermission('recorder', PERMISSIONS.orderManage)).toBe(false)
  })

  it('只读人员可以查看主要业务模块但没有办理权限', () => {
    expect(roleHasPermission('viewer', PERMISSIONS.orderView)).toBe(true)
    expect(roleHasPermission('viewer', PERMISSIONS.deliveryView)).toBe(true)
    expect(roleHasPermission('viewer', PERMISSIONS.settleView)).toBe(true)
    expect(roleHasPermission('viewer', PERMISSIONS.deliveryManage)).toBe(false)
    expect(roleHasPermission('viewer', PERMISSIONS.settleManage)).toBe(false)
  })

  it('财务可以结算收款但不能办理出库或生产回录', () => {
    expect(roleHasPermission('finance', PERMISSIONS.settleManage)).toBe(true)
    expect(roleHasPermission('finance', PERMISSIONS.settleReceive)).toBe(true)
    expect(roleHasPermission('finance', PERMISSIONS.deliveryManage)).toBe(false)
    expect(roleHasPermission('finance', PERMISSIONS.orderBackRecord)).toBe(false)
  })

  it('出库员可以办理出库但不能结算收款', () => {
    expect(getRoleProfile('warehouse')?.label).toBe('出库员')
    expect(roleHasPermission('warehouse', PERMISSIONS.deliveryManage)).toBe(true)
    expect(roleHasPermission('warehouse', PERMISSIONS.settleManage)).toBe(false)
    expect(roleHasPermission('warehouse', PERMISSIONS.settleReceive)).toBe(false)
  })
})
