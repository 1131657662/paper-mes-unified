import { Button, Checkbox, List, Space, Tag, Typography } from 'antd'
import { PROCESS_MODE, STEP_TYPE, processModeRequiresMain } from '../../../constants/processOrder'
import type { Machine } from '../../../types/machine'
import type { ProcessRoutePreviewVO } from '../../../types/processOrder'
import { formatGram, formatKg, formatMm } from '../../../utils/numberFormatters'
import type { MergedSourceLock } from '../rewindConsumptionUtils'
import type { RollDraft } from '../types'
import { rollTotalWeight } from '../../processOrderDetail/routeConfigSource'
import { supportsRouteDesigner } from '../configStepSelection'

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
  batchDisabledReason?: string
  index: number
  interactionDisabled: boolean
  machineUuid?: string
  lock?: MergedSourceLock
  machines: Machine[]
  previewStatus: { color: string; label: string }
  roll: RollDraft
  routePreview?: ProcessRoutePreviewVO
  selected: boolean
}

export default function WorkbenchRollItem({ actions, state }: Props) {
  const disabled = Boolean(state.lock) || state.interactionDisabled
  const className = rollItemClassName(state.selected, disabled)
  const select = () => {
    if (state.interactionDisabled) return
    if (state.lock) actions.onLockedSelect?.(state.roll, state.lock)
    else actions.onSelect(state.roll.localId)
  }

  return (
    <List.Item
      aria-current={state.selected ? 'true' : undefined}
      aria-disabled={disabled}
      className={className}
      data-roll-id={state.roll.localId}
      role="button"
      tabIndex={disabled ? -1 : 0}
      onClick={select}
      onKeyDown={(event) => {
        if (event.key !== 'Enter' && event.key !== ' ') return
        event.preventDefault()
        select()
      }}
    >
      <div className="process-roll-option__content">
        <RollHeading actions={actions} disabled={disabled} state={state} />
        <div className="process-roll-option__footer">
          <RollTags state={state} />
          {(supportsRouteDesigner(state.roll.processMode, state.roll.pieceNum) || state.routePreview) && (
            <RouteButton actions={actions} state={state} />
          )}
        </div>
      </div>
    </List.Item>
  )
}

function RollHeading({ actions, disabled, state }: { actions: WorkbenchRollItemActions; disabled: boolean; state: WorkbenchRollItemState }) {
  const checkboxDisabled = disabled || Boolean(state.batchDisabledReason)
  return (
    <Space align="start">
      {state.selected ? <Tag color="processing">当前</Tag> : <BatchCheckbox actions={actions}
        disabled={checkboxDisabled} reason={state.batchDisabledReason} state={state} />}
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

function BatchCheckbox({ actions, disabled, reason, state }: BatchCheckboxProps) {
  const checkbox = <Checkbox aria-label={`选择母卷 ${state.index + 1}`}
    checked={!state.lock && state.checked} disabled={disabled}
    onClick={(event) => event.stopPropagation()}
    onKeyDown={(event) => event.stopPropagation()}
    onChange={(event) => actions.onToggle(state.roll.localId, event.target.checked)} />
  if (!reason) return checkbox
  return <span title={reason} onClick={(event) => event.stopPropagation()}>{checkbox}</span>
}

function RollTags({ state }: { state: WorkbenchRollItemState }) {
  const roll = state.roll
  return (
    <div className="process-roll-option__tags">
      <Tag color={roll.processMode === 3 ? 'default' : 'blue'}>{PROCESS_MODE[roll.processMode ?? 1]}</Tag>
      {processModeRequiresMain(roll.processMode) && <Tag color="green">{STEP_TYPE[roll.mainStepType ?? 2]}</Tag>}
      {processModeRequiresMain(roll.processMode) && <Tag color={state.machineUuid ? 'cyan' : 'default'}>{machineName(state.machineUuid, state.machines)}</Tag>}
      {state.routePreview ? <Tag color="blue">链式 {state.routePreview.stages?.length ?? 0} 道</Tag> : <Tag color={state.previewStatus.color}>{state.previewStatus.label}</Tag>}
    </div>
  )
}

function RouteButton({ actions, state }: { actions: WorkbenchRollItemActions; state: WorkbenchRollItemState }) {
  const blocked = !supportsRouteDesigner(state.roll.processMode, state.roll.pieceNum)
  return (
    <Button
      className="process-roll-option__route"
      disabled={state.interactionDisabled || blocked}
      size="small"
      type={state.routePreview ? 'primary' : 'default'}
      title={blocked ? '件数大于1的母卷需拆分为单件后配置链式工艺' : undefined}
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
  return rollTotalWeight(roll)
}

interface BatchCheckboxProps {
  actions: WorkbenchRollItemActions
  disabled: boolean
  reason?: string
  state: WorkbenchRollItemState
}
