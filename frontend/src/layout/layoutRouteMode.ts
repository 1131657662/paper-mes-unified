const PROFILE_MODULES = ['customers', 'papers', 'machines', 'warehouses', 'users']

export function isEdgeScrollRoute(pathname: string): boolean {
  return pathname === '/dashboard'
    || pathname.startsWith('/reports/')
    || ['/system-config', '/operation-logs', '/profile', '/process-orders/create'].includes(pathname)
    || isProcessOrderRoute(pathname)
    || isDocumentRoute(pathname, 'delivery-orders')
    || isDocumentRoute(pathname, 'settle-orders')
    || isProfileRoute(pathname)
}

function isProcessOrderRoute(pathname: string): boolean {
  return /^\/process-orders\/[^/]+$/.test(pathname)
    || /^\/process-orders\/[^/]+\/(?:back-record|config-finish)$/.test(pathname)
    || /^\/process-orders\/create\/[^/]+\/routes\/[^/]+$/.test(pathname)
}

function isDocumentRoute(pathname: string, modulePath: string): boolean {
  return pathname === `/${modulePath}/create` || new RegExp(`^/${modulePath}/[^/]+$`).test(pathname)
}

function isProfileRoute(pathname: string): boolean {
  return PROFILE_MODULES.some((modulePath) => (
    pathname === `/${modulePath}/create`
    || new RegExp(`^/${modulePath}/[^/]+(?:/edit)?$`).test(pathname)
  ))
}
