import { useState, type KeyboardEvent } from 'react'
import { Empty, message } from 'antd'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import BackRecordActivePanel from './BackRecordActivePanel'
import BackRecordClosurePanel from './BackRecordClosurePanel'
import BackRecordRollNavigator from './BackRecordRollNavigator'
import { focusFirstBackRecordField } from './backRecordKeyboard'
import { buildBackRecordWorkbench } from './backRecordWorkbenchUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface Props {
  detail: ProcessOrderDetailVO
  values: BackRecordFormValues
  onProcessChange: (item: BackRecordWorkItem) => void
}

export default function BackRecordWorkbench({ detail, values, onProcessChange }: Props) {
  const workbench = buildBackRecordWorkbench(detail)
  const [activeKey, setActiveKey] = useState(workbench.items[0]?.key ?? '')
  const activeItem = workbench.items.find((item) => item.key === activeKey) ?? workbench.items[0]

  if (!activeItem) return <Empty description="暂无可回录母卷" />

  const activeIndex = workbench.items.findIndex((item) => item.key === activeItem.key)
  const selectIndex = (index: number) => {
    const target = workbench.items[index]
    if (!target) return
    setActiveKey(target.key)
    window.setTimeout(focusFirstBackRecordField, 0)
  }
  const goPrevious = () => {
    if (activeIndex <= 0) {
      message.info('已经是第一项')
      return
    }
    selectIndex(activeIndex - 1)
  }
  const goNext = () => {
    if (activeIndex >= workbench.items.length - 1) {
      message.info('已经是最后一项')
      return
    }
    selectIndex(activeIndex + 1)
  }
  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (!event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      goPrevious()
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      goNext()
    }
  }

  return (
    <div className="back-record-workbench" onKeyDown={handleKeyDown}>
      <BackRecordRollNavigator
        items={workbench.items}
        activeKey={activeItem.key}
        values={values}
        onSelect={setActiveKey}
      />
      <BackRecordActivePanel item={activeItem} onNext={goNext} onPrevious={goPrevious} onProcessChange={onProcessChange} />
      <BackRecordClosurePanel item={activeItem} items={workbench.items} values={values} />
    </div>
  )
}
