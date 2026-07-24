const LEGACY_VIEW_PATHS = {
  inventory: '/reports/inventory',
  overview: '/reports/overview',
  production: '/reports/production',
  settlement: '/reports/settlement',
} as const

const LEGACY_QUERY_ALLOWLIST = [
  'dateFrom',
  'dateTo',
  'customerUuid',
  'customerUuids',
  'paperName',
  'paperNames',
  'mainStepType',
  'processMode',
  'machineUuid',
  'settleType',
  'isInvoice',
  'orderStatus',
  'metricReleaseUuid',
] as const

export interface ReportLegacyLocation {
  pathname: string
  search: string
}

export function resolveLegacyReportLocation(search: string): ReportLegacyLocation {
  const source = new URLSearchParams(search)
  const alertEvent = source.get('alertEvent')
  if (alertEvent) return alertEventLocation(alertEvent)

  const target = new URLSearchParams()
  LEGACY_QUERY_ALLOWLIST.forEach((key) => copyValue(source, target, key))
  return {
    pathname: legacyViewPath(source.get('view')),
    search: target.size > 0 ? `?${target.toString()}` : '',
  }
}

function alertEventLocation(eventId: string): ReportLegacyLocation {
  const target = new URLSearchParams({ eventId })
  return {
    pathname: '/reports/management/subscriptions',
    search: `?${target.toString()}`,
  }
}

function copyValue(source: URLSearchParams, target: URLSearchParams, key: string) {
  source.getAll(key).forEach((value) => {
    if (value) target.append(key, value)
  })
}

function legacyViewPath(view: string | null): string {
  if (!view || !(view in LEGACY_VIEW_PATHS)) return '/reports/overview'
  return LEGACY_VIEW_PATHS[view as keyof typeof LEGACY_VIEW_PATHS]
}
