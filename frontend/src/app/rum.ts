import { onCLS, onFCP, onINP, onLCP, onTTFB, type Metric } from 'web-vitals'
import { findRouteMeta } from '../router/routeMeta'

type Browser = 'chrome' | 'edge' | 'firefox' | 'safari' | 'other'
type DeviceTier = 'low' | 'mid' | 'high' | 'unknown'
type NetworkType = 'slow-2g' | '2g' | '3g' | '4g' | 'unknown'

export interface RumPayload {
  name: string
  value: number
  rating: string
  route: string
  browser: Browser
  browserVersion: string
  deviceTier: DeviceTier
  networkType: NetworkType
}

interface RumContext {
  route: string
  browser: Browser
  browserVersion: string
  deviceTier: DeviceTier
  networkType: NetworkType
}

interface NavigatorWithSignals extends Navigator {
  connection?: { effectiveType?: string }
  deviceMemory?: number
}

const endpoint = '/api/rum'
let installed = false

export function installRum() {
  if (installed || !rumEnabled() || typeof window === 'undefined') return
  installed = true
  const context = createContext()
  const seen = new Set<string>()
  const report = (metric: Metric) => {
    if (seen.has(metric.name)) return
    seen.add(metric.name)
    sendMetric(createPayload(metric, context))
  }
  onCLS(report)
  onFCP(report)
  onINP(report)
  onLCP(report)
  onTTFB(report)
}

export function createPayload(
  metric: Pick<Metric, 'name' | 'value' | 'rating'>,
  context: RumContext,
): RumPayload {
  return {
    name: metric.name,
    value: metric.value,
    rating: metric.rating,
    ...context,
  }
}

export function resolveRouteTemplate(pathname: string): string {
  const safePathname = pathname.split(/[?#]/, 1)[0] || '/'
  return findRouteMeta(safePathname)?.path ?? '*'
}

function createContext(): RumContext {
  const browser = detectBrowser(navigator.userAgent)
  return {
    route: resolveRouteTemplate(window.location.pathname),
    browser,
    browserVersion: detectBrowserVersion(navigator.userAgent, browser),
    deviceTier: detectDeviceTier(navigator),
    networkType: detectNetworkType(navigator),
  }
}

function detectBrowser(userAgent: string): Browser {
  if (/Edg\//i.test(userAgent)) return 'edge'
  if (/Chrome\//i.test(userAgent)) return 'chrome'
  if (/Firefox\//i.test(userAgent)) return 'firefox'
  if (/Safari\//i.test(userAgent) && /Version\//i.test(userAgent))
    return 'safari'
  return 'other'
}

function detectBrowserVersion(userAgent: string, browser: Browser): string {
  const token =
    browser === 'edge'
      ? 'Edg'
      : browser === 'chrome'
        ? 'Chrome'
        : browser === 'firefox'
          ? 'Firefox'
          : 'Version'
  return userAgent.match(new RegExp(`${token}/(\\d+)`, 'i'))?.[1] ?? 'unknown'
}

function detectDeviceTier(currentNavigator: Navigator): DeviceTier {
  const signals = currentNavigator as NavigatorWithSignals
  const cores = signals.hardwareConcurrency ?? 4
  const memory = signals.deviceMemory
  if (cores <= 2 || (memory !== undefined && memory <= 2)) return 'low'
  if (cores >= 8 && (memory === undefined || memory >= 8)) return 'high'
  return 'mid'
}

function detectNetworkType(currentNavigator: Navigator): NetworkType {
  const type = (currentNavigator as NavigatorWithSignals).connection
    ?.effectiveType
  return type === 'slow-2g' || type === '2g' || type === '3g' || type === '4g'
    ? type
    : 'unknown'
}

function rumEnabled() {
  return import.meta.env.VITE_RUM_ENABLED === 'true'
}

function sendMetric(payload: RumPayload) {
  const body = JSON.stringify(payload)
  const blob = new Blob([body], { type: 'application/json' })
  try {
    if (navigator.sendBeacon?.(endpoint, blob)) return
  } catch {
    // Telemetry must never affect the application path.
  }
  void fetch(endpoint, {
    method: 'POST',
    body,
    credentials: 'omit',
    headers: { 'Content-Type': 'application/json' },
    keepalive: true,
  }).catch(() => undefined)
}
