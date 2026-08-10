import type { NavigateFunction } from 'react-router'
import { DEFAULT_PAGE_TAB_PATH, getNextActivePath, type PageTabItem } from './pageTabModel'

export type TabActionKey = 'refresh' | 'close-current' | 'close-left' | 'close-right' | 'close-other' | 'close-all'

export interface PageTabActionOptions {
  activePath: string
  closeAllTabs: () => void
  closeLeftTabs: (path: string) => void
  closeOtherTabs: (path: string) => void
  closeRightTabs: (path: string) => void
  closeTab: (path: string) => void
  navigate: NavigateFunction
  tabs: PageTabItem[]
}

export interface PageTabActions {
  closeCurrent: (path?: string) => void
  handleMenuAction: (key: TabActionKey, targetPath?: string) => void
}

export function createPageTabActions(options: PageTabActionOptions): PageTabActions {
  const { activePath, closeAllTabs, closeLeftTabs, closeOtherTabs, closeRightTabs, closeTab, navigate, tabs } = options

  const closeCurrent = (path = activePath) => {
    const nextPath = getNextActivePath(tabs, path, activePath)
    closeTab(path)
    if (path === activePath) navigate(nextPath)
  }

  const closeSide = (targetPath: string, direction: 'left' | 'right') => {
    const closeTabs = direction === 'left' ? closeLeftTabs : closeRightTabs
    closeTabs(targetPath)
    if (!willKeepPathAfterSideClose({ activePath, direction, targetPath, tabs })) navigate(targetPath)
  }

  const closeOther = (targetPath: string) => {
    closeOtherTabs(targetPath)
    if (targetPath !== activePath) navigate(targetPath)
  }

  const closeAll = () => {
    closeAllTabs()
    navigate(DEFAULT_PAGE_TAB_PATH)
  }

  const handleMenuAction = (key: TabActionKey, targetPath = activePath) => {
    if (key === 'refresh') navigate(0)
    if (key === 'close-current') closeCurrent(targetPath)
    if (key === 'close-left') closeSide(targetPath, 'left')
    if (key === 'close-right') closeSide(targetPath, 'right')
    if (key === 'close-other') closeOther(targetPath)
    if (key === 'close-all') closeAll()
  }

  return { closeCurrent, handleMenuAction }
}

interface SideCloseOptions {
  activePath: string
  direction: 'left' | 'right'
  targetPath: string
  tabs: PageTabItem[]
}

function willKeepPathAfterSideClose({ activePath, direction, targetPath, tabs }: SideCloseOptions) {
  const activeIndex = tabs.findIndex((tab) => tab.path === activePath)
  const targetIndex = tabs.findIndex((tab) => tab.path === targetPath)
  const activeTab = tabs[activeIndex]

  if (!activeTab?.closable) return true
  if (activeIndex === -1 || targetIndex === -1) return false
  return direction === 'left' ? activeIndex >= targetIndex : activeIndex <= targetIndex
}
