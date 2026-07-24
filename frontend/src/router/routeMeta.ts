export interface AppRouteMeta {
  closable?: boolean
  group?: string
  label: string
  menuKey?: string
  parentLabel?: string
  path: string
}

export const routeMeta: AppRouteMeta[] = [
  { path: '/dashboard', label: '仪表盘', closable: false },
  { path: '/process-orders', label: '加工单' },
  { path: '/delivery-orders', label: '出库单', parentLabel: '出库管理' },
  { path: '/delivery-orders/inventory', label: '成品库存', parentLabel: '出库管理' },
  { path: '/delivery-orders/inventory/finishes', label: '成品库存', parentLabel: '出库管理', menuKey: '/delivery-orders/inventory' },
  { path: '/delivery-orders/inventory/customers/:customerUuid', label: '客户库存明细', parentLabel: '成品库存', menuKey: '/delivery-orders/inventory' },
  { path: '/settle-orders', label: '结算管理' },
  { path: '/reports', label: '统计报表' },
  { path: '/reports/overview', label: '经营总览', parentLabel: '统计报表', menuKey: '/reports/overview' },
  { path: '/reports/production', label: '生产分析', parentLabel: '统计报表', menuKey: '/reports/production' },
  { path: '/reports/quality-loss', label: '质量与损耗', parentLabel: '统计报表', menuKey: '/reports/quality-loss' },
  { path: '/reports/settlement', label: '结算与应收', parentLabel: '统计报表', menuKey: '/reports/settlement' },
  { path: '/reports/collection', label: '回款分析', parentLabel: '统计报表', menuKey: '/reports/collection' },
  { path: '/reports/inventory', label: '库存流转', parentLabel: '统计报表', menuKey: '/reports/inventory' },
  { path: '/reports/delivery', label: '出库分析', parentLabel: '统计报表', menuKey: '/reports/delivery' },
  { path: '/reports/explorer', label: '多维分析', parentLabel: '统计报表', menuKey: '/reports/explorer' },
  { path: '/reports/management/views', label: '保存视图', parentLabel: '报表管理', menuKey: '/reports/management/views' },
  { path: '/reports/management/subscriptions', label: '订阅与预警', parentLabel: '报表管理', menuKey: '/reports/management/subscriptions' },
  { path: '/reports/management/metrics', label: '指标口径', parentLabel: '报表管理', menuKey: '/reports/management/metrics' },
  { path: '/profile', label: '个人中心' },
  { path: '/customers', label: '客户管理', parentLabel: '基础档案', group: 'base' },
  { path: '/customers/create', label: '新增客户', parentLabel: '客户管理', menuKey: '/customers' },
  { path: '/customers/:uuid', label: '客户详情', parentLabel: '客户管理', menuKey: '/customers' },
  { path: '/customers/:uuid/edit', label: '编辑客户', parentLabel: '客户管理', menuKey: '/customers' },
  { path: '/papers', label: '纸张档案', parentLabel: '基础档案', group: 'base' },
  { path: '/papers/create', label: '新增纸张', parentLabel: '纸张档案', menuKey: '/papers' },
  { path: '/papers/:uuid', label: '纸张详情', parentLabel: '纸张档案', menuKey: '/papers' },
  { path: '/papers/:uuid/edit', label: '编辑纸张', parentLabel: '纸张档案', menuKey: '/papers' },
  { path: '/machines', label: '机台与工位', parentLabel: '基础档案', group: 'base' },
  { path: '/machines/create', label: '新增生产资源', parentLabel: '机台与工位', menuKey: '/machines' },
  { path: '/machines/:uuid', label: '资源详情', parentLabel: '机台与工位', menuKey: '/machines' },
  { path: '/machines/:uuid/edit', label: '编辑生产资源', parentLabel: '机台与工位', menuKey: '/machines' },
  { path: '/warehouses', label: '仓库档案', parentLabel: '基础档案', group: 'base' },
  { path: '/warehouses/create', label: '新增仓库', parentLabel: '仓库档案', menuKey: '/warehouses' },
  { path: '/warehouses/:uuid', label: '仓库详情', parentLabel: '仓库档案', menuKey: '/warehouses' },
  { path: '/warehouses/:uuid/edit', label: '编辑仓库', parentLabel: '仓库档案', menuKey: '/warehouses' },
  { path: '/users', label: '用户权限', parentLabel: '系统管理', group: 'system' },
  { path: '/users/create', label: '新增用户', parentLabel: '用户权限', menuKey: '/users' },
  { path: '/users/:uuid', label: '用户详情', parentLabel: '用户权限', menuKey: '/users' },
  { path: '/users/:uuid/edit', label: '编辑用户', parentLabel: '用户权限', menuKey: '/users' },
  { path: '/system-config', label: '系统配置', parentLabel: '系统管理', group: 'system' },
  { path: '/process-orders/create', label: '新建加工单', parentLabel: '加工单', menuKey: '/process-orders' },
  { path: '/process-orders/create/:uuid/routes/:rollUuid', label: '链式工艺设计', parentLabel: '加工单', menuKey: '/process-orders' },
  { path: '/process-orders/:uuid', label: '加工单详情', parentLabel: '加工单', menuKey: '/process-orders' },
  { path: '/process-orders/:uuid/back-record', label: '回录工作台', parentLabel: '加工单', menuKey: '/process-orders' },
  { path: '/process-orders/:uuid/config-finish', label: '成品配置', parentLabel: '加工单', menuKey: '/process-orders' },
  { path: '/delivery-orders/create', label: '新建出库单', parentLabel: '出库管理', menuKey: '/delivery-orders' },
  { path: '/delivery-orders/:uuid', label: '出库单详情', parentLabel: '出库管理', menuKey: '/delivery-orders' },
  { path: '/settle-orders/create', label: '新建结算单', parentLabel: '结算管理', menuKey: '/settle-orders' },
  { path: '/settle-orders/:uuid', label: '结算单详情', parentLabel: '结算管理', menuKey: '/settle-orders' },
  { path: '/operation-logs', label: '操作日志', parentLabel: '系统管理', group: 'system' },
  { path: '*', label: '页面不存在' },
]

export function findRouteMeta(pathname: string) {
  return routeMeta.find((item) => matchPath(item.path, pathname))
}

export function selectedMenuKey(pathname: string) {
  const meta = findRouteMeta(pathname)
  if (meta?.path === '*') return pathname
  return meta?.menuKey ?? meta?.path ?? pathname
}

function matchPath(pattern: string, pathname: string) {
  if (pattern === '*') return true
  if (pattern === pathname) return true
  const patternParts = pattern.split('/').filter(Boolean)
  const pathParts = pathname.split('/').filter(Boolean)
  if (patternParts.length !== pathParts.length) return false
  return patternParts.every((part, index) => part.startsWith(':') || part === pathParts[index])
}
