import type { MenuProps, TabsProps } from 'antd'
import {
  CloseCircleOutlined,
  CloseOutlined,
  ColumnWidthOutlined,
  HomeOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import type { TabActionKey } from './pageTabActions'
import type { PageTabItem } from './pageTabModel'
import { PageTabLabel } from './PageTabView'

interface ToTabItemsOptions {
  currentPath: string
  onAction: (key: TabActionKey, path: string) => void
  tabs: PageTabItem[]
}

export function toTabItems({ currentPath, onAction, tabs }: ToTabItemsOptions): TabsProps['items'] {
  return tabs.map((tab) => ({
    // rc-tabs renders its editable-card close control as role="tab".
    // Keep closing available through the accessible tab-actions menu instead.
    closable: false,
    key: tab.path,
    label: <PageTabLabel label={tab.label} menuItems={createMenuItems({ activePath: tab.path, currentPath, tabs })} onAction={(key) => onAction(key, tab.path)} />,
  }))
}

export function createMenuItems({ activePath, currentPath, tabs }: CreateMenuItemsOptions): MenuProps['items'] {
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

interface CreateMenuItemsOptions {
  activePath: string
  currentPath: string
  tabs: PageTabItem[]
}
