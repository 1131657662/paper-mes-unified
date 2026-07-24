export const REPORT_TOPIC_CODES = [
  'overview',
  'production',
  'quality-loss',
  'settlement',
  'collection',
  'inventory',
  'delivery',
  'explorer',
] as const

export const REPORT_MANAGEMENT_CODES = [
  'views',
  'subscriptions',
  'metrics',
] as const

export type ReportTopicCode = (typeof REPORT_TOPIC_CODES)[number]
export type ReportManagementCode = (typeof REPORT_MANAGEMENT_CODES)[number]

export interface ReportNavigationItem {
  code: ReportTopicCode | ReportManagementCode
  description: string
  label: string
  path: string
}

export const reportTopicNavigation = [
  item('overview', '经营总览', '跨生产、库存、结算与出库的经营指标总览'),
  item('production', '生产分析', '加工量、产出效率、工艺结构与设备负荷'),
  item('quality-loss', '质量与损耗', '损耗趋势、异常分布与阈值偏差'),
  item('settlement', '结算与应收', '结算进度、应收账龄与客户欠款'),
  item('collection', '回款分析', '现金、抵扣、优惠与回款周期'),
  item('inventory', '库存流转', '库存快照、锁定、入库与出库流转'),
  item('delivery', '出库分析', '出库趋势、仓库效率与履约时效'),
  item('explorer', '多维分析', '按授权维度组合指标并逐层钻取'),
] satisfies ReportNavigationItem[]

export const reportManagementNavigation = [
  managementItem('views', '保存视图', '管理个人和组织共享的查询视图'),
  managementItem('subscriptions', '订阅与预警', '管理定时报表、阈值规则与预警事件'),
  managementItem('metrics', '指标口径', '管理指标版本、发布包与物化状态'),
] satisfies ReportNavigationItem[]

const reportSourcePaths: readonly ReportSourcePath[] = reportTopicNavigation.map((entry) => entry.path)
  .filter((path): path is ReportSourcePath => path !== '/reports/management/views'
    && path !== '/reports/management/subscriptions' && path !== '/reports/management/metrics')

export function resolveReportSourcePath(path: string | undefined): ReportSourcePath {
  return reportSourcePaths.find((candidate) => candidate === path) ?? '/reports/overview'
}

export function findReportNavigation(path: string): ReportNavigationItem | undefined {
  return [...reportTopicNavigation, ...reportManagementNavigation]
    .find((entry) => entry.path === path)
}

function item(code: ReportTopicCode, label: string, description: string): ReportNavigationItem {
  return { code, description, label, path: `/reports/${code}` }
}

function managementItem(
  code: ReportManagementCode,
  label: string,
  description: string,
): ReportNavigationItem {
  return { code, description, label, path: `/reports/management/${code}` }
}
import type { ReportSourcePath } from '../../types/report'
