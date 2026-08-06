import { findRouteMeta } from '../router/routeMeta'

export interface PageTabItem {
  closable: boolean
  label: string
  path: string
}

export const DEFAULT_PAGE_TAB_PATH = '/dashboard'
const MAX_PAGE_TABS = 12

export function createTab(pathname: string): PageTabItem {
  const path = normalizePageTabPath(pathname)
  const meta = findRouteMeta(routePath(path))
  return {
    closable: meta?.closable !== false,
    label: meta?.label ?? '页面',
    path,
  }
}

export function ensurePageTabs(tabs: PageTabItem[], fallbackPath = DEFAULT_PAGE_TAB_PATH) {
  const normalizedTabs = removeStaleAppendTabs(tabs.reduce<PageTabItem[]>((result, tab) => {
    const nextTab = createTab(tab.path)
    return [...result.filter((item) => item.path !== nextTab.path), nextTab]
  }, []))
  if (normalizedTabs.length === 0) return [createTab(fallbackPath)]
  const pinnedTabs = normalizedTabs.filter((tab) => !tab.closable)
  const recentTabs = normalizedTabs.filter((tab) => tab.closable).slice(-(MAX_PAGE_TABS - pinnedTabs.length))
  return [...pinnedTabs, ...recentTabs]
}

export function normalizePageTabPath(pathname: string) {
  const { pathOnly, search, hash } = splitLocation(pathname)
  const cleanedPath = pathOnly.trim()
  const withoutTrailingSlash = cleanedPath.length > 1 ? cleanedPath.replace(/\/+$/, '') : cleanedPath
  if (!withoutTrailingSlash || withoutTrailingSlash === '/') return DEFAULT_PAGE_TAB_PATH
  const normalizedPath = withoutTrailingSlash === '/reports' ? '/reports/overview' : withoutTrailingSlash
  if (!isAppendRoute(normalizedPath)) return normalizedPath
  const session = new URLSearchParams(search).get('session')
  return session ? `${normalizedPath}?session=${encodeURIComponent(session)}${hash}` : normalizedPath
}

function splitLocation(value: string) {
  const hashIndex = value.indexOf('#')
  const withoutHash = hashIndex >= 0 ? value.slice(0, hashIndex) : value
  const hash = hashIndex >= 0 ? value.slice(hashIndex) : ''
  const queryIndex = withoutHash.indexOf('?')
  return {
    pathOnly: queryIndex >= 0 ? withoutHash.slice(0, queryIndex) : withoutHash,
    search: queryIndex >= 0 ? withoutHash.slice(queryIndex + 1) : '',
    hash,
  }
}

function routePath(path: string) {
  return splitLocation(path).pathOnly
}

function isAppendRoute(path: string) {
  return /^\/process-orders\/[^/]+\/append$/.test(path)
}

function hasAppendSession(path: string) {
  return isAppendRoute(routePath(path)) && new URLSearchParams(splitLocation(path).search).has('session')
}

function removeStaleAppendTabs(tabs: PageTabItem[]) {
  const hasSessionTab = tabs.some((tab) => hasAppendSession(tab.path))
  if (!hasSessionTab) return tabs
  return tabs.filter((tab) => !isAppendRoute(routePath(tab.path)) || hasAppendSession(tab.path))
}

export function getNextActivePath(
  tabs: PageTabItem[],
  removedPath: string,
  activePath: string,
) {
  const remaining = tabs.filter((tab) => tab.path !== removedPath || !tab.closable)
  if (remaining.some((tab) => tab.path === activePath)) return activePath

  const removedIndex = tabs.findIndex((tab) => tab.path === removedPath)
  const nextIndex = Math.min(Math.max(removedIndex, 0), remaining.length - 1)
  return remaining[nextIndex]?.path ?? DEFAULT_PAGE_TAB_PATH
}
