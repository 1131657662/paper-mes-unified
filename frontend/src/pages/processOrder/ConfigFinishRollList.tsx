import type { KeyboardEvent } from 'react'
import { Checkbox, Tag, Typography } from 'antd'
import { CheckCircleOutlined, MergeCellsOutlined } from '@ant-design/icons'
import { PROCESS_MODE, STEP_TYPE, processModeRequiresMain } from '../../constants/processOrder'
import type { OriginalRoll } from '../../types/processOrder'
import { formatGram, formatKg, formatMm } from '../../utils/numberFormatters'

interface RollSelection {
  checkedUuids: string[]
  selectedIndex: number
}

interface RollStatus {
  finishCounts: Map<string, number>
  sourceOnlyUuids: Set<string>
}

interface Props {
  onSelect: (index: number) => void
  onToggle: (uuid: string, checked: boolean) => void
  rolls: OriginalRoll[]
  selection: RollSelection
  status: RollStatus
}

export default function ConfigFinishRollList({ onSelect, onToggle, rolls, selection, status }: Props) {
  const configuredCount = rolls.filter((roll) => (status.finishCounts.get(roll.uuid) ?? 0) > 0).length
  return (
    <aside className="config-finish-sidebar">
      <div className="config-finish-sidebar__head">
        <div>
          <Typography.Text strong>母卷列表</Typography.Text>
          <span>{rolls.length} 卷</span>
        </div>
        <Typography.Text type="secondary">已配 {configuredCount}/{rolls.length}</Typography.Text>
      </div>
      <div aria-label="母卷列表" className="config-finish-roll-list" role="list">
        {rolls.map((roll, index) => (
          <RollItem
            key={roll.uuid}
            checked={selection.checkedUuids.includes(roll.uuid)}
            finishCount={status.finishCounts.get(roll.uuid) ?? 0}
            index={index}
            isSourceOnly={status.sourceOnlyUuids.has(roll.uuid)}
            onSelect={onSelect}
            onToggle={onToggle}
            roll={roll}
            selected={selection.selectedIndex === index}
          />
        ))}
      </div>
      <div className="config-finish-sidebar__foot">
        已选择 {selection.checkedUuids.length} 卷用于批量复制
      </div>
    </aside>
  )
}

interface RollItemProps {
  checked: boolean
  finishCount: number
  index: number
  isSourceOnly: boolean
  onSelect: (index: number) => void
  onToggle: (uuid: string, checked: boolean) => void
  roll: OriginalRoll
  selected: boolean
}

function RollItem(props: RollItemProps) {
  const { checked, finishCount, index, isSourceOnly, onSelect, onToggle, roll, selected } = props
  const className = `config-finish-roll${selected ? ' is-selected' : ''}${finishCount > 0 ? ' is-configured' : ''}`
  const selectOnKeyboard = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key !== 'Enter' && event.key !== ' ') return
    event.preventDefault()
    onSelect(index)
  }
  return (
    <div
      aria-current={selected ? 'true' : undefined}
      className={className}
      role="listitem"
      tabIndex={0}
      onClick={() => onSelect(index)}
      onKeyDown={selectOnKeyboard}
    >
      <div className="config-finish-roll__head">
        <Checkbox
          aria-label={`选择母卷 ${index + 1}`}
          checked={checked}
          onClick={(event) => event.stopPropagation()}
          onChange={(event) => onToggle(roll.uuid, event.target.checked)}
        />
        <strong>母卷 {index + 1}</strong>
        {finishCount > 0 && <Tag icon={<CheckCircleOutlined />} color="success">已配 {finishCount} 件</Tag>}
        {finishCount === 0 && isSourceOnly && <Tag icon={<MergeCellsOutlined />} color="processing">合并来源</Tag>}
      </div>
      <span className="config-finish-roll__paper">{roll.paperName || '-'}</span>
      <span>{formatGram(roll.gramWeight)} / {formatMm(roll.originalWidth)}</span>
      <span>{formatKg(roll.rollWeight)} × {roll.pieceNum || 1} 件</span>
      <span>
        {PROCESS_MODE[roll.processMode ?? 1] ?? '-'}
        {processModeRequiresMain(roll.processMode) ? ` / ${STEP_TYPE[roll.mainStepType ?? 0] ?? '-'}` : ''}
      </span>
    </div>
  )
}
