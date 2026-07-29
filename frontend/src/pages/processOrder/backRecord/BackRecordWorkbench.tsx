import { Empty } from 'antd'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import BackRecordActivePanel from './BackRecordActivePanel'
import BackRecordClosurePanel from './BackRecordClosurePanel'
import BackRecordRollNavigator from './BackRecordRollNavigator'
import { buildBackRecordWorkbench } from './backRecordWorkbenchUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'
import { useBackRecordWorkbenchNavigation } from './useBackRecordWorkbenchNavigation'
import { buildBackRecordSourceOptions } from './backRecordRollOptions'

interface Props {
  detail: ProcessOrderDetailVO
  onDirty?: () => void
  values: BackRecordFormValues
  onProcessChange: (item: BackRecordWorkItem) => void
  onReopen: (item: BackRecordWorkItem) => void
  onClearSelection: () => void
  onSelectAll: () => void
  onSelectOnly: (key: string) => void
  onToggleSelection: (key: string, checked: boolean) => void
  selectedKeys: Set<string>
  reopening: boolean
}

export default function BackRecordWorkbench({
  detail,
  onClearSelection,
  onDirty,
  onProcessChange,
  onReopen,
  onSelectAll,
  onSelectOnly,
  onToggleSelection,
  selectedKeys,
  reopening,
  values,
}: Props) {
  const workbench = buildBackRecordWorkbench(detail)
  const navigation = useBackRecordWorkbenchNavigation(workbench.items)
  const sourceOptions = buildBackRecordSourceOptions(detail.originalRolls)
  const activeItem = navigation.activeItem

  if (!activeItem) return <Empty description="暂无可回录母卷" />

  return (
    <div className="back-record-workbench" onKeyDown={navigation.onKeyDown}>
      <BackRecordRollNavigator
        items={workbench.items}
        activeKey={activeItem.key}
        values={values}
        onClear={onClearSelection}
        onSelect={navigation.selectKey}
        onSelectAll={onSelectAll}
        onSelectOnly={() => onSelectOnly(activeItem.key)}
        onReopen={onReopen}
        onToggle={onToggleSelection}
        selectedKeys={selectedKeys}
        reopening={reopening}
      />
      <BackRecordActivePanel key={activeItem.key} item={activeItem} onDirty={onDirty} sourceOptions={sourceOptions} onNext={navigation.next} onPrevious={navigation.previous} onProcessChange={onProcessChange} />
      <BackRecordClosurePanel item={activeItem} items={workbench.items} values={values} />
    </div>
  )
}
