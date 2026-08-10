import { useEffect, useRef, useState } from 'react'
import { Tabs } from 'antd'
import { MoreOutlined } from '@ant-design/icons'
import { useLocation, useNavigate } from 'react-router'
import { createPageTabActions } from './pageTabActions'
import { PageTabTools, TabScrollButton } from './PageTabView'
import { createMenuItems, toTabItems } from './pageTabMenuModel'
import {
  emptyScrollState,
  readTabScrollState,
  scrollTabs,
  watchTabScrollState,
  type TabScrollState,
} from './pageTabScroll'
import { usePageTabs } from './usePageTabs'

export default function PageTabs() {
  const location = useLocation()
  const navigate = useNavigate()
  const tabsRef = useRef<HTMLDivElement>(null)
  const [scrollState, setScrollState] = useState<TabScrollState>(emptyScrollState)
  const tabState = usePageTabs(`${location.pathname}${location.search}${location.hash}`)
  const { activePath, ...pageTabState } = tabState
  const actions = createPageTabActions({ activePath, navigate, ...pageTabState })
  const activeMenuItems = createMenuItems({ activePath, currentPath: activePath, tabs: tabState.tabs })
  const shellClassName = scrollState.canScroll ? 'app-shell__tabs app-shell__tabs--scrollable' : 'app-shell__tabs'

  useEffect(() => watchTabScrollState(tabsRef.current, setScrollState), [activePath, tabState.tabs.length])

  const handleScroll = (deltaX: number) => {
    scrollTabs(tabsRef.current, deltaX, () => setScrollState(readTabScrollState(tabsRef.current)))
  }

  return (
    <div className={shellClassName} ref={tabsRef}>
      {scrollState.canScroll && <TabScrollButton disabled={!scrollState.canScrollLeft} direction="left" onScroll={() => handleScroll(-220)} />}
      <div className="app-shell__tabs-center">
        <Tabs
          activeKey={activePath}
          className="app-shell__tabs-control"
          hideAdd
          items={toTabItems({ currentPath: activePath, onAction: actions.handleMenuAction, tabs: tabState.tabs })}
          more={{ icon: <MoreOutlined aria-label="更多标签" />, trigger: 'hover' }}
          onChange={navigate}
          onEdit={(targetKey, action) => {
            if (action === 'remove' && typeof targetKey === 'string') actions.closeCurrent(targetKey)
          }}
          tabBarGutter={4}
          type="editable-card"
        />
      </div>
      <PageTabTools
        canScroll={scrollState.canScroll}
        canScrollRight={scrollState.canScrollRight}
        items={activeMenuItems}
        onAction={actions.handleMenuAction}
        onScrollRight={() => handleScroll(220)}
      />
    </div>
  )
}
