import { findRouteMeta } from '../router/routeMeta'

export interface PageTabItem {
  closable: boolean
  label: string
  path: string
}

export const DEFAULT_PAGE_TAB_PATH = '/dashboard'

export function createTab(pathname: string): PageTabItem {
  const path = normalizePageTabPath(pathname)
  const meta = findRouteMeta(path)
  return {
    closable: meta?.closable !== false,
    label: meta?.label ?? '页面',
    path,
  }
}

export function ensurePageTabs(tabs: PageTabItem[], fallbackPath = DEFAULT_PAGE_TAB_PATH) {
  const normalizedTabs = tabs.reduce<PageTabItem[]>((result, tab) => {
    const nextTab = createTab(tab.path)
    return [...result.filter((item) => item.path !== nextTab.path), nextTab]
  }, [])
  return normalizedTabs.length > 0 ? normalizedTabs : [createTab(fallbackPath)]
}

export function normalizePageTabPath(pathname: string) {
  const [pathOnly = ''] = pathname.split(/[?#]/)
  const cleanedPath = pathOnly.trim()
  const withoutTrailingSlash = cleanedPath.length > 1 ? cleanedPath.replace(/\/+$/, '') : cleanedPath
  if (!withoutTrailingSlash || withoutTrailingSlash === '/') return DEFAULT_PAGE_TAB_PATH
  return withoutTrailingSlash
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
