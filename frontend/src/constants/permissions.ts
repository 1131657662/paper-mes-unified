export const PERMISSIONS = {
  all: '*',
  baseManage: 'base:manage',
  baseView: 'base:view',
  deliveryManage: 'delivery:manage',
  deliveryView: 'delivery:view',
  orderBackRecord: 'order:back-record',
  orderCreate: 'order:create',
  orderManage: 'order:manage',
  orderView: 'order:view',
  reportView: 'report:view',
  settleManage: 'settle:manage',
  settleReceive: 'settle:receive',
  settleView: 'settle:view',
  systemAudit: 'system:audit',
  userManage: 'user:manage',
} as const

export type PermissionCode = (typeof PERMISSIONS)[keyof typeof PERMISSIONS]
