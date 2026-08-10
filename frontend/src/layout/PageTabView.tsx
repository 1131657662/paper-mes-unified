import type { MenuProps } from 'antd'
import { Button, Dropdown } from 'antd'
import { MoreOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons'
import type { TabActionKey } from './pageTabActions'

export interface TabScrollButtonProps {
  disabled: boolean
  direction: 'left' | 'right'
  onScroll: () => void
}

export interface PageTabToolsProps {
  canScroll: boolean
  canScrollRight: boolean
  items: MenuProps['items']
  onAction: (key: TabActionKey) => void
  onScrollRight: () => void
}

export interface PageTabLabelProps {
  label: string
  menuItems: MenuProps['items']
  onAction: (key: TabActionKey) => void
}

interface PageTabMenuProps {
  items: MenuProps['items']
  onAction: (key: TabActionKey) => void
}

export function TabScrollButton({ direction, disabled, onScroll }: TabScrollButtonProps) {
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

export function PageTabTools({ canScroll, canScrollRight, items, onAction, onScrollRight }: PageTabToolsProps) {
  return (
    <div className="app-shell__tabs-tools">
      {canScroll && <TabScrollButton disabled={!canScrollRight} direction="right" onScroll={onScrollRight} />}
      <PageTabMenu items={items} onAction={onAction} />
    </div>
  )
}

function PageTabMenu({ items, onAction }: PageTabMenuProps) {
  return (
    <Dropdown menu={{ items, onClick: ({ key }) => onAction(key as TabActionKey) }} trigger={['click']}>
      <Button aria-label="标签操作" className="app-shell__tabs-more" icon={<MoreOutlined />} size="small" type="text" />
    </Dropdown>
  )
}

export function PageTabLabel({ label, menuItems, onAction }: PageTabLabelProps) {
  return (
    <Dropdown menu={{ items: menuItems, onClick: ({ key }) => onAction(key as TabActionKey) }} trigger={['contextMenu']}>
      <span className="app-shell__tab-label">{label}</span>
    </Dropdown>
  )
}
