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
  values: BackRecordFormValues
  onProcessChange: (item: BackRecordWorkItem) => void
}

export default function BackRecordWorkbench({ detail, values, onProcessChange }: Props) {
  const workbench = buildBackRecordWorkbench(detail)
  const navigation = useBackRecordWorkbenchNavigation(workbench.items)
  const sourceOptions = buildBackRecordSourceOptions(detail.originalRolls)

  if (!navigation.activeItem) return <Empty description="暂无可回录母卷" />

  return (
    <div className="back-record-workbench" onKeyDown={navigation.onKeyDown}>
      <BackRecordRollNavigator
        items={workbench.items}
        activeKey={navigation.activeItem.key}
        values={values}
        onSelect={navigation.selectKey}
      />
      <BackRecordActivePanel item={navigation.activeItem} sourceOptions={sourceOptions} onNext={navigation.next} onPrevious={navigation.previous} onProcessChange={onProcessChange} />
      <BackRecordClosurePanel item={navigation.activeItem} items={workbench.items} values={values} />
    </div>
  )
}
