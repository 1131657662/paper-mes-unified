import { PERMISSIONS, type PermissionCode } from './permissions'
import type { UserRoleCode } from '../types/user'

export interface PermissionItem {
  code: PermissionCode
  label: string
  description: string
}

export interface PermissionGroup {
  key: string
  title: string
  description: string
  permissions: PermissionCode[]
}

export interface RoleProfile {
  code: UserRoleCode
  label: string
  tone: string
  summary: string
  description: string
  permissions: PermissionCode[]
}

export const PERMISSION_ITEMS: PermissionItem[] = [
  { code: PERMISSIONS.baseView, label: '基础档案查看', description: '查看客户、纸张、机台、仓库档案' },
  { code: PERMISSIONS.baseManage, label: '基础档案维护', description: '新增、编辑、删除基础档案' },
  { code: PERMISSIONS.orderView, label: '加工单查看', description: '查看加工单列表、详情、打印快照' },
  { code: PERMISSIONS.orderCreate, label: '新建加工单', description: '创建草稿、录入母卷、配置加工方案' },
  { code: PERMISSIONS.orderManage, label: '加工单下发维护', description: '打印下发、维护工序和成品卷号' },
  { code: PERMISSIONS.orderBackRecord, label: '生产回录', description: '填写实际成品、异常和回录结果' },
  { code: PERMISSIONS.deliveryView, label: '出库查看', description: '查看出库单、打印和导出出库单' },
  { code: PERMISSIONS.deliveryManage, label: '出库办理', description: '新建出库、确认签收、回退改单' },
  { code: PERMISSIONS.settleView, label: '结算查看', description: '查看结算单、打印和导出结算单' },
  { code: PERMISSIONS.settleManage, label: '结算办理', description: '生成结算单、作废结算单' },
  { code: PERMISSIONS.settleReceive, label: '收款登记', description: '登记收款、取消收款记录' },
  { code: PERMISSIONS.reportView, label: '经营报表', description: '查看仪表盘和统计报表' },
  { code: PERMISSIONS.systemAudit, label: '操作日志', description: '查看系统操作留痕和审计记录' },
  { code: PERMISSIONS.userManage, label: '用户权限维护', description: '维护系统账号、角色、状态和密码' },
]

export const PERMISSION_GROUPS: PermissionGroup[] = [
  {
    key: 'base',
    title: '基础资料',
    description: '客户、纸张、机台、仓库等档案',
    permissions: [PERMISSIONS.baseView, PERMISSIONS.baseManage],
  },
  {
    key: 'order',
    title: '加工生产',
    description: '新建加工单、下发、打印、回录',
    permissions: [PERMISSIONS.orderView, PERMISSIONS.orderCreate, PERMISSIONS.orderManage, PERMISSIONS.orderBackRecord],
  },
  {
    key: 'delivery',
    title: '出库管理',
    description: '出库单查看、新建、确认和回退',
    permissions: [PERMISSIONS.deliveryView, PERMISSIONS.deliveryManage],
  },
  {
    key: 'settle',
    title: '结算收款',
    description: '结算单查看、生成、收款登记',
    permissions: [PERMISSIONS.settleView, PERMISSIONS.settleManage, PERMISSIONS.settleReceive],
  },
  {
    key: 'report',
    title: '经营分析',
    description: '仪表盘、统计报表和经营汇总',
    permissions: [PERMISSIONS.reportView],
  },
  {
    key: 'system',
    title: '系统管理',
    description: '用户权限和操作审计',
    permissions: [PERMISSIONS.userManage, PERMISSIONS.systemAudit],
  },
]

export const ROLE_PROFILES: Record<UserRoleCode, RoleProfile> = {
  admin: {
    code: 'admin',
    label: '管理员',
    tone: 'blue',
    summary: '全系统维护',
    description: '可维护全部业务数据、用户权限、操作日志和系统配置。',
    permissions: [PERMISSIONS.all],
  },
  operator: {
    code: 'operator',
    label: '录单员',
    tone: 'cyan',
    summary: '加工单与回录',
    description: '负责新建加工单、配置加工方案、查看基础档案并完成生产回录。',
    permissions: [PERMISSIONS.baseView, PERMISSIONS.orderView, PERMISSIONS.orderCreate, PERMISSIONS.orderBackRecord, PERMISSIONS.reportView],
  },
  finance: {
    code: 'finance',
    label: '财务',
    tone: 'purple',
    summary: '结算、收款与报表',
    description: '负责查看出库、办理结算、登记收款并查看经营报表。',
    permissions: [
      PERMISSIONS.baseView,
      PERMISSIONS.orderView,
      PERMISSIONS.deliveryView,
      PERMISSIONS.settleView,
      PERMISSIONS.settleManage,
      PERMISSIONS.settleReceive,
      PERMISSIONS.reportView,
    ],
  },
  warehouse: {
    code: 'warehouse',
    label: '仓库',
    tone: 'green',
    summary: '出库办理',
    description: '负责查看加工单、办理出库、确认签收和出库回退。',
    permissions: [PERMISSIONS.baseView, PERMISSIONS.orderView, PERMISSIONS.deliveryView, PERMISSIONS.deliveryManage, PERMISSIONS.reportView],
  },
}

export function getRoleProfile(roleCode?: string) {
  if (roleCode && roleCode in ROLE_PROFILES) {
    return ROLE_PROFILES[roleCode as UserRoleCode]
  }
  return undefined
}

export function getPermissionLabel(code: string) {
  if (code === PERMISSIONS.all) return '全部权限'
  return PERMISSION_ITEMS.find((item) => item.code === code)?.label ?? code
}

export function getRolePermissions(roleCode?: string) {
  const profile = getRoleProfile(roleCode)
  if (!profile) return []
  if (profile.permissions.includes(PERMISSIONS.all)) {
    return PERMISSION_ITEMS.map((item) => item.code)
  }
  return profile.permissions
}

export function roleHasPermission(roleCode: string | undefined, code: PermissionCode) {
  const profile = getRoleProfile(roleCode)
  if (!profile) return false
  return profile.permissions.includes(PERMISSIONS.all) || profile.permissions.includes(code)
}

export function getRoleModuleNames(roleCode?: string) {
  return PERMISSION_GROUPS
    .filter((group) => group.permissions.some((code) => roleHasPermission(roleCode, code)))
    .map((group) => group.title)
}
