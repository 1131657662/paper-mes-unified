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
  { code: PERMISSIONS.orderPricing, label: '加工费计价调整', description: '调整待回录或已完成加工单的工序计价' },
  { code: PERMISSIONS.orderPricingApprove, label: '大额计价优惠审批', description: '批准超过免审额度的负向计价调整' },
  { code: PERMISSIONS.deliveryView, label: '出库查看', description: '查看出库单、打印和导出出库单' },
  { code: PERMISSIONS.deliveryManage, label: '出库办理', description: '新建出库、确认签收、回退改单' },
  { code: PERMISSIONS.settleView, label: '结算查看', description: '查看结算单、打印和导出结算单' },
  { code: PERMISSIONS.settleManage, label: '结算办理', description: '生成结算单、作废结算单' },
  { code: PERMISSIONS.settleDiscount, label: '结算优惠核销', description: '登记结算收款中的客户优惠' },
  { code: PERMISSIONS.settleDiscountApprove, label: '结算优惠审批', description: '审批超过免审额度的结算优惠' },
  { code: PERMISSIONS.settleReceive, label: '收款登记', description: '登记收款、取消收款记录' },
  { code: PERMISSIONS.reportView, label: '经营报表', description: '查看仪表盘和统计报表' },
  { code: PERMISSIONS.exportTaskView, label: '导出任务中心', description: '查看本人导出任务和下载结果文件' },
  { code: PERMISSIONS.systemConfig, label: '系统配置', description: '维护字典、系统参数和单号规则' },
  { code: PERMISSIONS.dataBackup, label: '数据备份', description: '执行备份、恢复演练和保留策略' },
  { code: PERMISSIONS.dataHealth, label: '数据巡检', description: '查看并处理业务数据一致性异常' },
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
    permissions: [
      PERMISSIONS.orderView,
      PERMISSIONS.orderCreate,
      PERMISSIONS.orderManage,
      PERMISSIONS.orderBackRecord,
      PERMISSIONS.orderPricing,
      PERMISSIONS.orderPricingApprove,
    ],
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
    permissions: [
      PERMISSIONS.settleView,
      PERMISSIONS.settleManage,
      PERMISSIONS.settleDiscount,
      PERMISSIONS.settleDiscountApprove,
      PERMISSIONS.settleReceive,
    ],
  },
  {
    key: 'report',
    title: '经营分析',
    description: '仪表盘、统计报表和经营汇总',
    permissions: [PERMISSIONS.reportView, PERMISSIONS.exportTaskView],
  },
  {
    key: 'system',
    title: '系统管理',
    description: '用户、配置、数据安全和操作审计',
    permissions: [
      PERMISSIONS.userManage,
      PERMISSIONS.systemConfig,
      PERMISSIONS.dataBackup,
      PERMISSIONS.dataHealth,
      PERMISSIONS.systemAudit,
    ],
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
  order_clerk: {
    code: 'order_clerk',
    label: '制单员',
    tone: 'cyan',
    summary: '制单、下发与改单',
    description: '负责新建加工单、配置方案、打印下发、工序维护和无后续业务时的回退改单。',
    permissions: [
      PERMISSIONS.baseView,
      PERMISSIONS.orderView,
      PERMISSIONS.orderCreate,
      PERMISSIONS.orderManage,
      PERMISSIONS.orderPricing,
      PERMISSIONS.reportView,
      PERMISSIONS.exportTaskView,
    ],
  },
  recorder: {
    code: 'recorder',
    label: '回录员',
    tone: 'geekblue',
    summary: '生产数据回录',
    description: '负责查看加工单并填写母卷、成品、切边、异常和实际加工结果。',
    permissions: [PERMISSIONS.baseView, PERMISSIONS.orderView, PERMISSIONS.orderBackRecord, PERMISSIONS.reportView,
      PERMISSIONS.exportTaskView],
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
      PERMISSIONS.settleDiscount,
      PERMISSIONS.settleDiscountApprove,
      PERMISSIONS.settleReceive,
      PERMISSIONS.orderPricing,
      PERMISSIONS.orderPricingApprove,
      PERMISSIONS.reportView,
      PERMISSIONS.exportTaskView,
    ],
  },
  warehouse: {
    code: 'warehouse',
    label: '出库员',
    tone: 'green',
    summary: '出库办理',
    description: '负责查看加工单、办理出库、确认签收和出库回退。',
    permissions: [PERMISSIONS.baseView, PERMISSIONS.orderView, PERMISSIONS.deliveryView, PERMISSIONS.deliveryManage,
      PERMISSIONS.reportView, PERMISSIONS.exportTaskView],
  },
  viewer: {
    code: 'viewer',
    label: '只读人员',
    tone: 'default',
    summary: '业务只读查看',
    description: '可查看基础档案、加工单、出库单、结算单和经营报表，不可执行新增、修改或状态操作。',
    permissions: [PERMISSIONS.baseView, PERMISSIONS.orderView, PERMISSIONS.deliveryView, PERMISSIONS.settleView,
      PERMISSIONS.reportView, PERMISSIONS.exportTaskView],
  },
  operator: {
    code: 'operator',
    label: '录单员（兼容）',
    tone: 'default',
    summary: '历史兼容角色',
    description: '保留现有账号原权限，不再作为新岗位的推荐选择。',
    permissions: [PERMISSIONS.baseView, PERMISSIONS.orderView, PERMISSIONS.orderCreate, PERMISSIONS.orderBackRecord,
      PERMISSIONS.reportView, PERMISSIONS.exportTaskView],
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
