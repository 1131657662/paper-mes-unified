import { Badge, Button, Checkbox, List, Space, Tag, Typography } from 'antd'
import { useEffect, useRef } from 'react'
import { PROCESS_MODE, STEP_TYPE, processModeRequiresMain } from '../../../constants/processOrder'
import type { Machine } from '../../../types/machine'
import { formatKg, formatMm } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'
import '../../../components/processOrder/ProcessOrderShared.css'

interface Props {
  disabled?: boolean
  machines: Machine[]
  rolls: RollDraft[]
  selectedId?: string
  configuredIds?: string[]
  batchSelection?: RollBatchSelection
  onSelect: (localId: string) => void
}

export interface RollBatchSelection {
  checkedIds: string[]
  onClear: () => void
  onSelectAll: () => void
  onToggle: (localId: string, checked: boolean) => void
}

export default function RollSelectorPanel({ disabled = false, machines, rolls, selectedId, configuredIds = [], batchSelection, onSelect }: Props) {
  const listRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const selected = [...(listRef.current?.querySelectorAll<HTMLElement>('[data-roll-id]') ?? [])]
      .find((item) => item.dataset.rollId === selectedId)
    selected?.scrollIntoView({ block: 'nearest' })
  }, [selectedId])

  return (
    <div ref={listRef} className="roll-selector-panel">
      {batchSelection && <BatchSelectionToolbar disabled={disabled} selection={batchSelection} />}
      <List
        size="small"
        dataSource={rolls}
        renderItem={(roll, index) => (
          <RollSelectorItem
            actions={{ onSelect, onToggle: batchSelection?.onToggle }}
            state={{
              checked: batchSelection?.checkedIds.includes(roll.localId) ?? false,
              configured: configuredIds.includes(roll.localId),
              disabled,
              index,
              machines,
              roll,
              selectable: Boolean(roll.uuid),
              selected: selectedId === roll.localId,
            }}
          />
        )}
      />
    </div>
  )
}

function BatchSelectionToolbar({ disabled, selection }: { disabled: boolean; selection: RollBatchSelection }) {
  return (
    <div className="roll-selector-panel__toolbar">
      <Typography.Text type="secondary">已选 {selection.checkedIds.length} 卷</Typography.Text>
      <Space size="small">
        <Button size="small" disabled={disabled} onClick={selection.onSelectAll}>全选</Button>
        <Button size="small" disabled={disabled || !selection.checkedIds.length} onClick={selection.onClear}>全不选</Button>
      </Space>
    </div>
  )
}

interface RollSelectorItemState {
  checked: boolean
  configured: boolean
  disabled: boolean
  index: number
  machines: Machine[]
  roll: RollDraft
  selectable: boolean
  selected: boolean
}

interface RollSelectorItemActions {
  onSelect: (localId: string) => void
  onToggle?: (localId: string, checked: boolean) => void
}

function RollSelectorItem({ actions, state }: { actions: RollSelectorItemActions; state: RollSelectorItemState }) {
  const className = `process-roll-option${state.selected ? ' process-roll-option--selected' : ''}`
  const select = () => {
    if (!state.disabled) actions.onSelect(state.roll.localId)
  }
  return (
    <List.Item
      aria-current={state.selected ? 'true' : undefined}
      aria-disabled={state.disabled}
      className={className}
      data-roll-id={state.roll.localId}
      role="button"
      tabIndex={state.disabled ? -1 : 0}
      onClick={select}
      onKeyDown={(event) => {
        if (event.key !== 'Enter' && event.key !== ' ') return
        event.preventDefault()
        select()
      }}
    >
      <div className="process-roll-option__content">
        <Space align="start">
          {actions.onToggle && <Checkbox aria-label={`选择母卷 ${state.index + 1}`}
            checked={state.selectable && state.checked} disabled={state.disabled || !state.selectable}
            onClick={(event) => event.stopPropagation()}
            onKeyDown={(event) => event.stopPropagation()}
            onChange={(event) => actions.onToggle?.(state.roll.localId, event.target.checked)} />}
          <Typography.Text strong>原纸 {state.index + 1}</Typography.Text>
        </Space>
        {state.configured && <Badge className="process-roll-option__status" status="success" text="已配置" />}
        <div className="process-roll-option__meta">
          {state.roll.paperName || '-'} / {formatMm(state.roll.originalWidth)} / {formatKg(state.roll.rollWeight)}
        </div>
        <div className="process-roll-option__tags">
          <Tag color={state.roll.processMode === 3 ? 'default' : 'blue'}>{PROCESS_MODE[state.roll.processMode ?? 1]}</Tag>
          {processModeRequiresMain(state.roll.processMode) && <Tag color="green">{STEP_TYPE[state.roll.mainStepType ?? 2]}</Tag>}
          {processModeRequiresMain(state.roll.processMode) && <Tag color={state.roll.machineUuid ? 'cyan' : 'default'}>{machineName(state.roll.machineUuid, state.machines)}</Tag>}
        </div>
      </div>
    </List.Item>
  )
}

function machineName(machineUuid: string | undefined, machines: Machine[]) {
  if (!machineUuid) return '未选机台'
  return machines.find((machine) => machine.uuid === machineUuid)?.machineName ?? '未知机台'
}
