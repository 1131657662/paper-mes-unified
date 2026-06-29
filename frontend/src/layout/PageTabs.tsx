import { useEffect, useRef, useState } from 'react'
import type { MenuProps, TabsProps } from 'antd'
import { Button, Dropdown, Tabs } from 'antd'
import {
  LeftOutlined,
  CloseCircleOutlined,
  CloseOutlined,
  ColumnWidthOutlined,
  HomeOutlined,
  MoreOutlined,
  RightOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { useLocation, useNavigate } from 'react-router-dom'
import { DEFAULT_PAGE_TAB_PATH, getNextActivePath, type PageTabItem } from './pageTabModel'
import { usePageTabs } from './usePageTabs'

type TabActionKey = 'refresh' | 'close-current' | 'close-left' | 'close-right' | 'close-other' | 'close-all'

export default function PageTabs() {
  const location = useLocation()
  const navigate = useNavigate()
  const tabsRef = useRef<HTMLDivElement>(null)
  const [scrollState, setScrollState] = useState<TabScrollState>(emptyScrollState)
  const tabState = usePageTabs(location.pathname)
  const actions = createPageTabActions({ activePath: location.pathname, navigate, ...tabState })
  const activeMenuItems = createMenuItems({
    activePath: location.pathname,
    currentPath: location.pathname,
    tabs: tabState.tabs,
  })
  const shellClassName = scrollState.canScroll ? 'app-shell__tabs app-shell__tabs--scrollable' : 'app-shell__tabs'

  useEffect(() => {
    return watchTabScrollState(tabsRef.current, setScrollState)
  }, [location.pathname, tabState.tabs.length])

  const handleScroll = (deltaX: number) => {
    scrollTabs(tabsRef.current, deltaX)
    requestAnimationFrame(() => setScrollState(readTabScrollState(tabsRef.current)))
  }

  return (
    <div className={shellClassName} ref={tabsRef}>
      {scrollState.canScroll && (
        <TabScrollButton disabled={!scrollState.canScrollLeft} direction="left" onScroll={() => handleScroll(-220)} />
      )}
      <div className="app-shell__tabs-center">
        <Tabs
          activeKey={location.pathname}
          className="app-shell__tabs-control"
          hideAdd
          items={toTabItems({
            currentPath: location.pathname,
            onAction: actions.handleMenuAction,
            tabs: tabState.tabs,
          })}
          more={{ icon: null, trigger: 'hover' }}
          onChange={navigate}
          onEdit={(targetKey, action) => {
            if (action === 'remove' && typeof targetKey === 'string') actions.closeCurrent(targetKey)
          }}
          tabBarGutter={4}
          tabBarExtraContent={<PageTabMenu items={activeMenuItems} onAction={(key) => actions.handleMenuAction(key)} />}
          type="editable-card"
        />
      </div>
      {scrollState.canScroll && (
        <TabScrollButton disabled={!scrollState.canScrollRight} direction="right" onScroll={() => handleScroll(220)} />
      )}
    </div>
  )
}

function TabScrollButton({ direction, disabled, onScroll }: TabScrollButtonProps) {
  const icon = direction === 'left' ? <LeftOutlined /> : <RightOutlined />
  const label = direction === 'left' ? '向左滚动标签' : '向右滚动标签'

  return (
    <Button
      aria-label={label}
      className={`app-shell__tabs-arrow app-shell__tabs-arrow--${direction}`}
      disabled={disabled}
      icon={icon}
      onClick={onScroll}
      type="text"
    />
  )
}

function PageTabMenu({ items, onAction }: PageTabMenuProps) {
  return (
    <Dropdown menu={{ items, onClick: ({ key }) => onAction(key as TabActionKey) }} trigger={['click']}>
      <Button aria-label="标签操作" className="app-shell__tabs-more" icon={<MoreOutlined />} size="small" type="text" />
    </Dropdown>
  )
}

function PageTabLabel({ label, menuItems, onAction }: PageTabLabelProps) {
  return (
    <Dropdown menu={{ items: menuItems, onClick: ({ key }) => onAction(key as TabActionKey) }} trigger={['contextMenu']}>
      <span className="app-shell__tab-label">{label}</span>
    </Dropdown>
  )
}

function createPageTabActions(options: PageTabActionOptions) {
  const { activePath, closeAllTabs, closeLeftTabs, closeOtherTabs, closeRightTabs, closeTab, navigate, tabs } = options

  const closeCurrent = (path = activePath) => {
    const nextPath = getNextActivePath(tabs, path, activePath)
    closeTab(path)
    if (path === activePath) navigate(nextPath)
  }

  const closeLeft = (targetPath: string) => {
    closeLeftTabs(targetPath)
    if (!willKeepPathAfterSideClose({ activePath, direction: 'left', targetPath, tabs })) navigate(targetPath)
  }

  const closeRight = (targetPath: string) => {
    closeRightTabs(targetPath)
    if (!willKeepPathAfterSideClose({ activePath, direction: 'right', targetPath, tabs })) navigate(targetPath)
  }

  const closeOther = (targetPath: string) => {
    closeOtherTabs(targetPath)
    if (targetPath !== activePath) navigate(targetPath)
  }

  const closeAll = () => {
    closeAllTabs()
    navigate(DEFAULT_PAGE_TAB_PATH)
  }

  return {
    closeCurrent,
    handleMenuAction: (key: TabActionKey, targetPath = activePath) => {
      if (key === 'refresh') navigate(0)
      if (key === 'close-current') closeCurrent(targetPath)
      if (key === 'close-left') closeLeft(targetPath)
      if (key === 'close-right') closeRight(targetPath)
      if (key === 'close-other') closeOther(targetPath)
      if (key === 'close-all') closeAll()
    },
  }
}

function willKeepPathAfterSideClose(options: SideCloseOptions) {
  const { activePath, direction, targetPath, tabs } = options
  const activeIndex = tabs.findIndex((tab) => tab.path === activePath)
  const targetIndex = tabs.findIndex((tab) => tab.path === targetPath)
  const activeTab = tabs[activeIndex]

  if (!activeTab?.closable) return true
  if (activeIndex === -1 || targetIndex === -1) return false
  return direction === 'left' ? activeIndex >= targetIndex : activeIndex <= targetIndex
}

function scrollTabs(container: HTMLDivElement | null, deltaX: number) {
  const scroller = container?.querySelector('.ant-tabs-nav-wrap')
  scroller?.dispatchEvent(new WheelEvent('wheel', { bubbles: true, cancelable: true, deltaX, deltaY: 0 }))
}

function watchTabScrollState(container: HTMLDivElement | null, onChange: (state: TabScrollState) => void) {
  if (!container) return undefined

  const update = () => onChange(readTabScrollState(container))
  const resizeObserver = new ResizeObserver(update)
  const mutationObserver = new MutationObserver(update)
  const targets = getTabScrollTargets(container)

  targets.forEach((target) => resizeObserver.observe(target))
  targets.forEach((target) => mutationObserver.observe(target, { attributes: true, attributeFilter: ['class', 'style'] }))
  window.addEventListener('resize', update)

  const frame = requestAnimationFrame(update)

  return () => {
    cancelAnimationFrame(frame)
    resizeObserver.disconnect()
    mutationObserver.disconnect()
    window.removeEventListener('resize', update)
  }
}

function readTabScrollState(container: HTMLDivElement | null): TabScrollState {
  const wrap = container?.querySelector('.ant-tabs-nav-wrap')
  const list = container?.querySelector('.ant-tabs-nav-list')
  if (!(wrap instanceof HTMLElement) || !(list instanceof HTMLElement)) return emptyScrollState

  const canScrollLeft = wrap.classList.contains('ant-tabs-nav-wrap-ping-left')
  const canScrollRight = wrap.classList.contains('ant-tabs-nav-wrap-ping-right')
  const canScroll = canScrollLeft || canScrollRight || list.getBoundingClientRect().width > wrap.getBoundingClientRect().width + 1
  return { canScroll, canScrollLeft, canScrollRight }
}

function getTabScrollTargets(container: HTMLDivElement) {
  return [container, ...container.querySelectorAll('.ant-tabs-nav-wrap, .ant-tabs-nav-list')]
}

function toTabItems(options: ToTabItemsOptions): TabsProps['items'] {
  const { currentPath, onAction, tabs } = options
  const onlyOneTab = tabs.length <= 1
  return tabs.map((tab) => ({
    closable: tab.closable && !onlyOneTab,
    key: tab.path,
    label: (
      <PageTabLabel
        label={tab.label}
        menuItems={createMenuItems({ activePath: tab.path, currentPath, tabs })}
        onAction={(key) => onAction(key, tab.path)}
      />
    ),
  }))
}

function createMenuItems(options: CreateMenuItemsOptions): MenuProps['items'] {
  const { activePath, currentPath, tabs } = options
  const activeIndex = tabs.findIndex((tab) => tab.path === activePath)
  const closableTabs = tabs.filter((tab) => tab.closable)
  const hasLeftClosable = tabs.slice(0, Math.max(activeIndex, 0)).some((tab) => tab.closable)
  const hasRightClosable = tabs.slice(activeIndex + 1).some((tab) => tab.closable)

  return [
    { key: 'refresh', disabled: activePath !== currentPath, icon: <ReloadOutlined />, label: '刷新当前' },
    { key: 'close-current', disabled: tabs.length <= 1, icon: <CloseOutlined />, label: '关闭当前' },
    { key: 'close-left', disabled: !hasLeftClosable, icon: <ColumnWidthOutlined rotate={180} />, label: '关闭左侧' },
    { key: 'close-right', disabled: !hasRightClosable, icon: <ColumnWidthOutlined />, label: '关闭右侧' },
    { key: 'close-other', disabled: tabs.length <= 1, icon: <CloseCircleOutlined />, label: '关闭其他' },
    { key: 'close-all', disabled: tabs.length <= 1 || closableTabs.length === 0, icon: <HomeOutlined />, label: '关闭全部' },
  ]
}

interface PageTabMenuProps {
  items: MenuProps['items']
  onAction: (key: TabActionKey) => void
}

interface PageTabLabelProps {
  label: string
  menuItems: MenuProps['items']
  onAction: (key: TabActionKey) => void
}

interface ToTabItemsOptions {
  currentPath: string
  onAction: (key: TabActionKey, path: string) => void
  tabs: PageTabItem[]
}

interface SideCloseOptions {
  activePath: string
  direction: 'left' | 'right'
  targetPath: string
  tabs: PageTabItem[]
}

interface CreateMenuItemsOptions {
  activePath: string
  currentPath: string
  tabs: PageTabItem[]
}

interface TabScrollButtonProps {
  disabled: boolean
  direction: 'left' | 'right'
  onScroll: () => void
}

interface TabScrollState {
  canScroll: boolean
  canScrollLeft: boolean
  canScrollRight: boolean
}

const emptyScrollState: TabScrollState = {
  canScroll: false,
  canScrollLeft: false,
  canScrollRight: false,
}

interface PageTabActionOptions {
  activePath: string
  closeAllTabs: () => void
  closeLeftTabs: (path: string) => void
  closeOtherTabs: (path: string) => void
  closeRightTabs: (path: string) => void
  closeTab: (path: string) => void
  navigate: ReturnType<typeof useNavigate>
  tabs: PageTabItem[]
}
