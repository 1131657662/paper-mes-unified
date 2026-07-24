import { Button, Checkbox, List, Space, Tag, Typography } from 'antd'
import { PROCESS_MODE, STEP_TYPE, processModeRequiresMain } from '../../../constants/processOrder'
import type { Machine } from '../../../types/machine'
import type { ProcessRoutePreviewVO } from '../../../types/processOrder'
import { formatGram, formatKg, formatMm } from '../../../utils/numberFormatters'
import type { MergedSourceLock } from '../rewindConsumptionUtils'
import type { RollDraft } from '../types'

interface Props {
  actions: WorkbenchRollItemActions
  state: WorkbenchRollItemState
}

export interface WorkbenchRollItemActions {
  onLockedSelect?: (roll: RollDraft, lock: MergedSourceLock) => void
  onOpenRouteDesigner?: (roll: RollDraft) => void
  onSelect: (localId: string) => void
  onToggle: (localId: string, checked: boolean) => void
}

export interface WorkbenchRollItemState {
  checked: boolean
  index: number
  lock?: MergedSourceLock
  machines: Machine[]
  previewStatus: { color: string; label: string }
  roll: RollDraft
  routePreview?: ProcessRoutePreviewVO
  selected: boolean
}

export default function WorkbenchRollItem({ actions, state }: Props) {
  const disabled = Boolean(state.lock)
  const className = rollItemClassName(state.selected, disabled)
  const select = () => state.lock
    ? actions.onLockedSelect?.(state.roll, state.lock)
    : actions.onSelect(state.roll.localId)

  return (
    <List.Item className={className} onClick={select}>
      <div className="process-roll-option__content">
        <RollHeading actions={actions} disabled={disabled} state={state} />
        <RollTags state={state} />
        {processModeRequiresMain(state.roll.processMode) && <RouteButton actions={actions} state={state} />}
      </div>
    </List.Item>
  )
}

function RollHeading({ actions, disabled, state }: { actions: WorkbenchRollItemActions; disabled: boolean; state: WorkbenchRollItemState }) {
  return (
    <Space align="start">
      <Checkbox
        aria-label={`选择母卷 ${state.index + 1}`}
        checked={!disabled && state.checked}
        disabled={disabled}
        onClick={(event) => event.stopPropagation()}
        onChange={(event) => actions.onToggle(state.roll.localId, event.target.checked)}
      />
      <div>
        <Typography.Text strong>母卷 {state.index + 1}</Typography.Text>
        <div className="process-roll-option__identity">
          卷号：{state.roll.rollNo || '-'} / 编号：{state.roll.extraNo || '-'}
        </div>
        <div
          className="process-roll-option__spec"
          aria-label={`${state.roll.paperName || '-'} / ${formatGram(state.roll.gramWeight).replace(/\s+/g, '')} / ${formatMm(state.roll.originalWidth).replace(/\s+/g, '')} / ${formatKg(rollWeight(state.roll)).replace(/\s+/g, '')}`}
        >
          <span>{state.roll.paperName || '-'}</span>
          <span>{formatGram(state.roll.gramWeight).replace(/\s+/g, '')}</span>
          <span>{formatMm(state.roll.originalWidth).replace(/\s+/g, '')}</span>
          <span>{formatKg(rollWeight(state.roll)).replace(/\s+/g, '')}</span>
        </div>
      </div>
    </Space>
  )
}

function RollTags({ state }: { state: WorkbenchRollItemState }) {
  const roll = state.roll
  return (
    <div className="process-roll-option__tags">
      <Tag color={roll.processMode === 3 ? 'default' : 'blue'}>{PROCESS_MODE[roll.processMode ?? 1]}</Tag>
      {processModeRequiresMain(roll.processMode) && <Tag color="green">{STEP_TYPE[roll.mainStepType ?? 2]}</Tag>}
      {processModeRequiresMain(roll.processMode) && <Tag color={roll.machineUuid ? 'cyan' : 'default'}>{machineName(roll.machineUuid, state.machines)}</Tag>}
      {state.routePreview ? <Tag color="blue">链式 {state.routePreview.stages?.length ?? 0} 道</Tag> : <Tag color={state.previewStatus.color}>{state.previewStatus.label}</Tag>}
    </div>
  )
}

function RouteButton({ actions, state }: { actions: WorkbenchRollItemActions; state: WorkbenchRollItemState }) {
  return (
    <Button
      className="process-roll-option__route"
      size="small"
      type={state.routePreview ? 'primary' : 'default'}
      onClick={(event) => {
        event.stopPropagation()
        actions.onOpenRouteDesigner?.(state.roll)
      }}
    >
      {state.routePreview ? '编辑链式工艺' : '链式工艺'}
    </Button>
  )
}

function rollItemClassName(selected: boolean, disabled: boolean) {
  return [
    'process-roll-option',
    selected && 'process-roll-option--selected',
    disabled && 'process-roll-option--locked',
  ].filter(Boolean).join(' ')
}

function machineName(machineUuid: string | undefined, machines: Machine[]) {
  if (!machineUuid) return '未选机台'
  return machines.find((machine) => machine.uuid === machineUuid)?.machineName ?? '未知机台'
}

function rollWeight(roll: RollDraft) {
  return Number(roll.rollWeight ?? 0) * (roll.pieceNum ?? 1)
}
