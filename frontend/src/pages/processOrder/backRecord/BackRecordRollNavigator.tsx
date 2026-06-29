import { Button, Tag, Typography } from 'antd'
import { PROCESS_MODE } from '../../../constants/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import { buildWorkItemMetrics, workItemStatus } from './backRecordWorkbenchUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface Props {
  items: BackRecordWorkItem[]
  activeKey: string
  values: BackRecordFormValues
  onSelect: (key: string) => void
}

export default function BackRecordRollNavigator({ items, activeKey, values, onSelect }: Props) {
  return (
    <aside className="back-record-nav">
      <div className="back-record-nav__head">
        <Typography.Text strong>母卷回录</Typography.Text>
        <Tag>{items.length}</Tag>
      </div>
      <div className="back-record-nav__list">
        {items.map((item) => (
          <RollNavItem
            key={item.key}
            item={item}
            active={item.key === activeKey}
            values={values}
            onSelect={onSelect}
          />
        ))}
      </div>
    </aside>
  )
}

function RollNavItem({
  item,
  active,
  values,
  onSelect,
}: {
  item: BackRecordWorkItem
  active: boolean
  values: BackRecordFormValues
  onSelect: (key: string) => void
}) {
  const status = workItemStatus(item, values)
  const metrics = buildWorkItemMetrics(item, values)
  const mode = item.roll?.processMode ? PROCESS_MODE[item.roll.processMode] : '成品池'

  return (
    <Button
      className={`back-record-nav-item${active ? ' back-record-nav-item--active' : ''}`}
      type="text"
      onClick={() => onSelect(item.key)}
    >
      <span className="back-record-nav-item__main">
        <span className="back-record-nav-item__title">{item.title}</span>
        <Tag color={status.color}>{status.text}</Tag>
      </span>
      <span className="back-record-nav-item__sub">{item.subtitle || mode}</span>
      <span className="back-record-nav-item__meta">
        <span>{mode}</span>
        <span>{item.finishes.filter(({ finish }) => finish.isSpare !== 1).length} 件成品</span>
        {metrics.diff != null && <span>差 {metrics.diff.toFixed(3)}kg</span>}
      </span>
    </Button>
  )
}
