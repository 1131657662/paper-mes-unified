import { Button, Checkbox, Popconfirm, Space, Tag, Typography } from 'antd'
import { UndoOutlined } from '@ant-design/icons'
import { PROCESS_MODE } from '../../../constants/processOrder'
import { formatKg, formatProductionEstimateKg } from '../../../features/processOrderDetail/orderDetailUtils'
import { isRollWeightKnown, rollTotalWeight } from '../../../features/processOrderDetail/routeConfigSource'
import { formatGram, formatMm, formatOptionalKg } from '../../../utils/numberFormatters'
import type { OriginalRoll } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import { buildWorkItemMetrics, workItemRecorded, workItemStatus } from './backRecordWorkbenchUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface Props {
  items: BackRecordWorkItem[]
  activeKey: string
  values: BackRecordFormValues
  onClear: () => void
  onSelect: (key: string) => void
  onSelectAll: () => void
  onSelectOnly: () => void
  onReopen: (item: BackRecordWorkItem) => void
  onToggle: (key: string, checked: boolean) => void
  selectedKeys: Set<string>
  reopening: boolean
}

export default function BackRecordRollNavigator({
  activeKey,
  items,
  onClear,
  onSelect,
  onSelectAll,
  onSelectOnly,
  onReopen,
  onToggle,
  selectedKeys,
  reopening,
  values,
}: Props) {
  const selectable = items.filter((item) => item.kind === 'roll' && !workItemRecorded(item))
  return (
    <aside className="back-record-nav">
      <div className="back-record-nav__head">
        <div className="back-record-nav__title-row">
          <Typography.Text strong>母卷回录</Typography.Text>
          <Tag>{selectedKeys.size} / {selectable.length} 已选</Tag>
        </div>
        <Space className="back-record-nav__selection-actions" size={2}>
          <Button type="link" size="small" onClick={onSelectAll}>全选</Button>
          <Button type="link" size="small" onClick={onSelectOnly}>仅当前</Button>
          <Button type="link" size="small" onClick={onClear}>清空</Button>
        </Space>
      </div>
      <div className="back-record-nav__list">
        {items.map((item) => (
          <RollNavItem
            key={item.key}
            item={item}
            active={item.key === activeKey}
            values={values}
            onSelect={onSelect}
            onReopen={onReopen}
            onToggle={onToggle}
            selected={selectedKeys.has(item.key)}
            reopening={reopening}
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
  onReopen,
  onToggle,
  selected,
  reopening,
}: {
  item: BackRecordWorkItem
  active: boolean
  values: BackRecordFormValues
  onSelect: (key: string) => void
  onReopen: (item: BackRecordWorkItem) => void
  onToggle: (key: string, checked: boolean) => void
  selected: boolean
  reopening: boolean
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
        {item.roll && <span>{rollWeightText(item.roll)}</span>}
        <span>{item.finishes.filter(({ finish }) => finish.isSpare !== 1 && finish.isRemain !== 1).length} 件成品</span>
        {shouldShowDiff && metrics.diff != null && <span>差 {formatOptionalKg(metrics.diff)}</span>}
      </span>
      </Button>
      {recorded && item.kind === 'roll' && (
        <div className="back-record-nav-item__reopen">
          <Popconfirm
            title="撤回本批回录？"
            description="相关成品会先撤出库存，已录实重会保留；当前未保存输入将被刷新。"
            okText="撤回并修改"
            cancelText="取消"
            onConfirm={() => onReopen(item)}
          >
            <Button danger type="link" size="small" icon={<UndoOutlined />} loading={reopening}>
              撤回修改
            </Button>
          </Popconfirm>
        </div>
      )}
    </div>
  )
}

function rollSpec(roll: OriginalRoll) {
  const paper = roll.paperName || '-'
  const gram = formatGram(roll.gramWeight)
  const width = formatMm(roll.originalWidth)
  return `${paper} / ${gram} / ${width}`
}

function rollWeightText(roll: OriginalRoll) {
  if (roll.actualWeight != null && roll.actualWeight > 0) return formatKg(roll.actualWeight)
  return isRollWeightKnown(roll) ? formatProductionEstimateKg(rollTotalWeight(roll)) : '来料重量待称重'
}
