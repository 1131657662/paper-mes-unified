const BASE_PATHS = ['/customers', '/papers', '/machines', '/warehouses']
const SYSTEM_PATHS = ['/users', '/system-config', '/operation-logs']

export function defaultOpenMenuKeys(pathname: string): string[] {
  if (pathname.startsWith('/reports/management/')) return ['reports', 'report-management']
  if (pathname.startsWith('/reports/')) return ['reports']
  if (pathname.startsWith('/delivery-orders')) return ['delivery']
  if (BASE_PATHS.some((path) => pathname.startsWith(path))) return ['base']
  if (SYSTEM_PATHS.some((path) => pathname.startsWith(path))) return ['system']
  return []
}
