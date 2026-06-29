import { useState } from 'react'
import { Empty } from 'antd'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import BackRecordActivePanel from './BackRecordActivePanel'
import BackRecordClosurePanel from './BackRecordClosurePanel'
import BackRecordRollNavigator from './BackRecordRollNavigator'
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
  const goNext = () => {
    const next = workbench.items[activeIndex + 1] ?? workbench.items[0]
    setActiveKey(next.key)
  }

  return (
    <div className="back-record-workbench">
      <BackRecordRollNavigator
        items={workbench.items}
        activeKey={activeItem.key}
        values={values}
        onSelect={setActiveKey}
      />
      <BackRecordActivePanel item={activeItem} onNext={goNext} onProcessChange={onProcessChange} />
      <BackRecordClosurePanel item={activeItem} items={workbench.items} values={values} />
    </div>
  )
}
