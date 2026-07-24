import { Button, Checkbox, Tag, Typography } from 'antd'
import { PROCESS_MODE } from '../../../constants/processOrder'
import { formatKg } from '../../../features/processOrderDetail/orderDetailUtils'
import { formatGram, formatMm, formatOptionalKg } from '../../../utils/numberFormatters'
import type { OriginalRoll } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import { buildWorkItemMetrics, workItemRecorded, workItemStatus } from './backRecordWorkbenchUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface Props {
  items: BackRecordWorkItem[]
  activeKey: string
  values: BackRecordFormValues
  onSelect: (key: string) => void
  onToggle: (key: string, checked: boolean) => void
  selectedKeys: Set<string>
}

export default function BackRecordRollNavigator({ items, activeKey, values, onSelect, onToggle, selectedKeys }: Props) {
  const selectable = items.filter((item) => item.kind === 'roll' && !workItemRecorded(item))
  return (
    <aside className="back-record-nav">
      <div className="back-record-nav__head">
        <Typography.Text strong>母卷回录</Typography.Text>
        <Tag>{selectedKeys.size} / {selectable.length} 已选</Tag>
      </div>
      <div className="back-record-nav__list">
        {items.map((item) => (
          <RollNavItem
            key={item.key}
            item={item}
            active={item.key === activeKey}
            values={values}
            onSelect={onSelect}
            onToggle={onToggle}
            selected={selectedKeys.has(item.key)}
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
  onToggle,
  selected,
}: {
  item: BackRecordWorkItem
  active: boolean
  values: BackRecordFormValues
  onSelect: (key: string) => void
  onToggle: (key: string, checked: boolean) => void
  selected: boolean
}) {
  const status = workItemStatus(item, values)
  const metrics = buildWorkItemMetrics(item, values)
  const mode = item.roll?.processMode ? PROCESS_MODE[item.roll.processMode] : '成品池'
  const shouldShowDiff = item.roll?.processMode !== 3
    && item.finishes.some(({ finish }) => finish.isSpare !== 1)
  const recorded = workItemRecorded(item)

  return (
    <div className="back-record-nav-item-shell">
      {item.kind === 'roll' && (
        <Checkbox
          aria-label={`选择${item.title}`}
          checked={selected}
          className="back-record-nav-item__selector"
          disabled={recorded}
          onChange={(event) => onToggle(item.key, event.target.checked)}
        />
      )}
      <Button
        className={`back-record-nav-item${active ? ' back-record-nav-item--active' : ''}${item.kind === 'roll' ? ' back-record-nav-item--selectable' : ''}`}
        type="text"
        onClick={() => onSelect(item.key)}
      >
      <span className="back-record-nav-item__main">
        <span className="back-record-nav-item__title">{item.title}</span>
        <Tag color={status.color}>{status.text}</Tag>
      </span>
      {item.roll && (
        <span className="back-record-nav-item__identity">
          <span>{`\u5377\u53f7\uff1a${item.roll.rollNo || '-'}`}</span>
          <span>{`\u7f16\u53f7\uff1a${item.roll.extraNo || '-'}`}</span>
          {item.roll.batchNo && <span>{`\u6279\u6b21\uff1a${item.roll.batchNo}`}</span>}
          <span>{`\u4ef6\u6570\uff1a${item.roll.pieceNum ?? 1}\u4ef6`}</span>
        </span>
      )}
      <span className="back-record-nav-item__sub">{item.roll ? rollSpec(item.roll) : item.subtitle || mode}</span>
      <span className="back-record-nav-item__meta">
        <span>{mode}</span>
        {item.roll && <span>{formatKg((item.roll.rollWeight ?? 0) * (item.roll.pieceNum ?? 1))}</span>}
        <span>{item.finishes.filter(({ finish }) => finish.isSpare !== 1).length} 件成品</span>
        {shouldShowDiff && metrics.diff != null && <span>差 {formatOptionalKg(metrics.diff)}</span>}
      </span>
      </Button>
    </div>
  )
}

function rollSpec(roll: OriginalRoll) {
  const paper = roll.paperName || '-'
  const gram = formatGram(roll.gramWeight)
  const width = formatMm(roll.originalWidth)
  return `${paper} / ${gram} / ${width}`
}
