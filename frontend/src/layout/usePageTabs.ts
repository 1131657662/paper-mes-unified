import { useEffect, useState } from 'react'
import { createTab, ensurePageTabs, type PageTabItem } from './pageTabModel'
import { cachePageTabs, createInitialPageTabs } from './pageTabStorage'

export function usePageTabs(pathname: string) {
  const [tabs, setTabs] = useState<PageTabItem[]>(() => createInitialPageTabs(pathname))

  useEffect(() => {
    setTabs((current) => {
      if (current.some((tab) => tab.path === pathname)) return current
      return [...current, createTab(pathname)]
    })
  }, [pathname])

  useEffect(() => {
    cachePageTabs(tabs)
  }, [tabs])

  const closeTab = (path: string) => {
    setTabs((current) => ensurePageTabs(current.filter((tab) => tab.path !== path || !tab.closable)))
  }

  const closeLeftTabs = (path: string) => {
    setTabs((current) => {
      const index = current.findIndex((tab) => tab.path === path)
      if (index <= 0) return current
      return ensurePageTabs(current.filter((tab, tabIndex) => tabIndex >= index || !tab.closable))
    })
  }

  const closeRightTabs = (path: string) => {
    setTabs((current) => {
      const index = current.findIndex((tab) => tab.path === path)
      if (index === -1 || index === current.length - 1) return current
      return ensurePageTabs(current.filter((tab, tabIndex) => tabIndex <= index || !tab.closable))
    })
  }

  const closeOtherTabs = (path: string) => {
    setTabs((current) => ensurePageTabs(current.filter((tab) => tab.path === path || !tab.closable)))
  }

  const closeAllTabs = () => {
    setTabs((current) => ensurePageTabs(current.filter((tab) => !tab.closable)))
  }

  return { closeAllTabs, closeLeftTabs, closeOtherTabs, closeRightTabs, closeTab, tabs }
}
