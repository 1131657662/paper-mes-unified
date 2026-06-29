import { findRouteMeta } from '../router/routeMeta'

export interface PageTabItem {
  closable: boolean
  label: string
  path: string
}

export const DEFAULT_PAGE_TAB_PATH = '/dashboard'

export function createTab(pathname: string): PageTabItem {
  const meta = findRouteMeta(pathname)
  return {
    closable: meta?.closable !== false,
    label: meta?.label ?? '页面',
    path: pathname,
  }
}

export function ensurePageTabs(tabs: PageTabItem[], fallbackPath = DEFAULT_PAGE_TAB_PATH) {
  return tabs.length > 0 ? tabs : [createTab(fallbackPath)]
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
