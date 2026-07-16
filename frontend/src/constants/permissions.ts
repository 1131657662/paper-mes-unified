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
  settleDiscount: 'settle:discount',
  settleDiscountApprove: 'settle:discount-approve',
  settleReceive: 'settle:receive',
  settleView: 'settle:view',
  dataBackup: 'system:data-backup',
  dataHealth: 'system:data-health',
  systemAudit: 'system:audit',
  systemConfig: 'system:config',
  userManage: 'user:manage',
} as const

export type PermissionCode = (typeof PERMISSIONS)[keyof typeof PERMISSIONS]
