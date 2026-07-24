import {
  AccountBookOutlined,
  AlertOutlined,
  AppstoreOutlined,
  BarChartOutlined,
  ContainerOutlined,
  ControlOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  DollarOutlined,
  ExportOutlined,
  FileDoneOutlined,
  FileOutlined,
  FileTextOutlined,
  FundOutlined,
  InboxOutlined,
  LineChartOutlined,
  ProfileOutlined,
  SettingOutlined,
  SlidersOutlined,
  TeamOutlined,
  ToolOutlined,
  UsergroupAddOutlined,
} from '@ant-design/icons'
import type { MenuProps } from 'antd'
import type { ReactNode } from 'react'
import { PERMISSIONS } from '../constants/permissions'
import { hasAnyPermission } from '../utils/permission'

type MenuItem = Required<MenuProps>['items'][number]

export function buildMenuItems(permissions?: string[]): MenuProps['items'] {
  const can = (items: string[]) => hasAnyPermission(permissions, items)
  const baseChildren = baseMenuItems(can)
  const systemChildren = systemMenuItems(can)
  const result = [
    can([PERMISSIONS.reportView]) && menu('/dashboard', <DashboardOutlined />, '仪表盘'),
    can([PERMISSIONS.orderView]) && menu('/process-orders', <ContainerOutlined />, '加工单'),
    can([PERMISSIONS.deliveryView]) && deliveryMenu(),
    can([PERMISSIONS.settleView]) && menu('/settle-orders', <AccountBookOutlined />, '结算管理'),
    can([PERMISSIONS.reportView]) && reportMenu(),
    baseChildren.length > 0 && group('base', <ProfileOutlined />, '基础档案', baseChildren),
    systemChildren.length > 0 && group('system', <SettingOutlined />, '系统管理', systemChildren),
  ]
  return result.filter(Boolean) as MenuProps['items']
}

function reportMenu(): MenuItem {
  return group('reports', <BarChartOutlined />, '统计报表', [
    menu('/reports/overview', <AppstoreOutlined />, '经营总览'),
    menu('/reports/production', <FundOutlined />, '生产分析'),
    menu('/reports/quality-loss', <AlertOutlined />, '质量与损耗'),
    menu('/reports/settlement', <DollarOutlined />, '结算与应收'),
    menu('/reports/collection', <LineChartOutlined />, '回款分析'),
    menu('/reports/inventory', <DatabaseOutlined />, '库存流转'),
    menu('/reports/delivery', <ExportOutlined />, '出库分析'),
    menu('/reports/explorer', <SlidersOutlined />, '多维分析'),
    group('report-management', <SettingOutlined />, '报表管理', [
      menu('/reports/management/views', <FileTextOutlined />, '保存视图'),
      menu('/reports/management/subscriptions', <AlertOutlined />, '订阅与预警'),
      menu('/reports/management/metrics', <ControlOutlined />, '指标口径'),
    ]),
  ])
}

function deliveryMenu(): MenuItem {
  return group('delivery', <ExportOutlined />, '出库管理', [
    menu('/delivery-orders', <FileDoneOutlined />, '出库单'),
    menu('/delivery-orders/inventory', <InboxOutlined />, '成品库存'),
  ])
}

function baseMenuItems(can: (permissions: string[]) => boolean): MenuItem[] {
  return [
    can([PERMISSIONS.baseView]) && menu('/customers', <UsergroupAddOutlined />, '客户管理'),
    can([PERMISSIONS.baseView]) && menu('/papers', <FileOutlined />, '纸张档案'),
    can([PERMISSIONS.baseView]) && menu('/machines', <ToolOutlined />, '机台与工位'),
    can([PERMISSIONS.baseView]) && menu('/warehouses', <InboxOutlined />, '仓库档案'),
  ].filter(Boolean) as MenuItem[]
}

function systemMenuItems(can: (permissions: string[]) => boolean): MenuItem[] {
  return [
    can([PERMISSIONS.userManage]) && menu('/users', <TeamOutlined />, '用户权限'),
    can([PERMISSIONS.systemConfig, PERMISSIONS.dataBackup, PERMISSIONS.dataHealth])
      && menu('/system-config', <ControlOutlined />, '系统配置'),
    can([PERMISSIONS.systemAudit]) && menu('/operation-logs', <FileTextOutlined />, '操作日志'),
  ].filter(Boolean) as MenuItem[]
}

function menu(key: string, icon: ReactNode, label: string): MenuItem {
  return { key, icon, label } as MenuItem
}

function group(key: string, icon: ReactNode, label: string, children: MenuItem[]): MenuItem {
  return { key, icon, label, children } as MenuItem
}
