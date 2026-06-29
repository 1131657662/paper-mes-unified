import { createTab, type PageTabItem } from './pageTabModel'

const PAGE_TABS_STORAGE_KEY = 'paper-mes:page-tabs:v1'
const MAX_PAGE_TABS = 18

export function createInitialPageTabs(currentPath: string) {
  const paths = [...readCachedPaths(), currentPath]
  return uniquePaths(paths).slice(-MAX_PAGE_TABS).map(createTab)
}

export function cachePageTabs(tabs: PageTabItem[]) {
  safeLocalStorage(() => {
    localStorage.setItem(PAGE_TABS_STORAGE_KEY, JSON.stringify(tabs.map((tab) => tab.path)))
  })
}

function readCachedPaths() {
  return safeLocalStorage(() => {
    const rawValue = localStorage.getItem(PAGE_TABS_STORAGE_KEY)
    const parsedValue: unknown = rawValue ? JSON.parse(rawValue) : []
    if (!Array.isArray(parsedValue)) return []
    return parsedValue.filter(isUsablePath)
  }) ?? []
}

function safeLocalStorage<T>(operation: () => T) {
  try {
    return operation()
  } catch {
    return undefined
  }
}

function uniquePaths(paths: string[]) {
  return paths.reduce<string[]>((result, path) => {
    return [...result.filter((item) => item !== path), path]
  }, [])
}

function isUsablePath(value: unknown): value is string {
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('/login')
}
