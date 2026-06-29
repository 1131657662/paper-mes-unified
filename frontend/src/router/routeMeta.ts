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
  { path: '/delivery-orders', label: '出库管理' },
  { path: '/settle-orders', label: '结算管理' },
  { path: '/reports', label: '统计报表' },
  { path: '/customers', label: '客户管理', parentLabel: '基础档案', group: 'base' },
  { path: '/papers', label: '纸张档案', parentLabel: '基础档案', group: 'base' },
  { path: '/machines', label: '机台档案', parentLabel: '基础档案', group: 'base' },
  { path: '/warehouses', label: '仓库档案', parentLabel: '基础档案', group: 'base' },
  { path: '/process-orders/create', label: '新建加工单', parentLabel: '加工单', menuKey: '/process-orders' },
  { path: '/process-orders/:uuid', label: '加工单详情', parentLabel: '加工单', menuKey: '/process-orders' },
  { path: '/process-orders/:uuid/back-record', label: '回录工作台', parentLabel: '加工单', menuKey: '/process-orders' },
  { path: '/process-orders/:uuid/config-finish', label: '成品配置', parentLabel: '加工单', menuKey: '/process-orders' },
  { path: '/operation-logs', label: '操作日志' },
]

export function findRouteMeta(pathname: string) {
  return routeMeta.find((item) => matchPath(item.path, pathname)) ?? routeMeta[0]
}

export function selectedMenuKey(pathname: string) {
  const meta = findRouteMeta(pathname)
  return meta?.menuKey ?? meta?.path ?? pathname
}

function matchPath(pattern: string, pathname: string) {
  if (pattern === pathname) return true
  const patternParts = pattern.split('/').filter(Boolean)
  const pathParts = pathname.split('/').filter(Boolean)
  if (patternParts.length !== pathParts.length) return false
  return patternParts.every((part, index) => part.startsWith(':') || part === pathParts[index])
}
