import { useState, type Dispatch, type KeyboardEvent, type SetStateAction } from 'react'
import { message } from 'antd'
import { focusFirstBackRecordField } from './backRecordKeyboard'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface MoveOptions {
  activeIndex: number
  direction: -1 | 1
  items: BackRecordWorkItem[]
  selectKey: (key: string) => void
}

interface SelectOptions {
  key: string
  setActiveKey: Dispatch<SetStateAction<string>>
}

interface NavigationState {
  activeItem?: BackRecordWorkItem
  next: () => void
  onKeyDown: (event: KeyboardEvent<HTMLDivElement>) => void
  previous: () => void
  selectKey: (key: string) => void
}

export function useBackRecordWorkbenchNavigation(items: BackRecordWorkItem[]): NavigationState {
  const [activeKey, setActiveKey] = useState(items[0]?.key ?? '')
  const activeItem = items.find((item) => item.key === activeKey) ?? items[0]
  const activeIndex = activeItem ? items.findIndex((item) => item.key === activeItem.key) : -1
  const selectKey = (key: string) => selectWorkItem({ key, setActiveKey })
  const previous = () => moveWorkItem({ activeIndex, direction: -1, items, selectKey })
  const next = () => moveWorkItem({ activeIndex, direction: 1, items, selectKey })
  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => handleArrowKey(event, previous, next)

  return { activeItem, next, onKeyDown, previous, selectKey }
}

function selectWorkItem({ key, setActiveKey }: SelectOptions) {
  setActiveKey(key)
  window.setTimeout(focusFirstBackRecordField, 0)
}

function moveWorkItem({ activeIndex, direction, items, selectKey }: MoveOptions) {
  const target = items[activeIndex + direction]
  if (target) {
    selectKey(target.key)
    return
  }
  message.info(direction < 0 ? '已经是第一项' : '已经是最后一项')
}

function handleArrowKey(
  event: KeyboardEvent<HTMLDivElement>,
  previous: () => void,
  next: () => void,
) {
  if (!event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return
  if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return
  event.preventDefault()
  if (event.key === 'ArrowUp') previous()
  else next()
}
