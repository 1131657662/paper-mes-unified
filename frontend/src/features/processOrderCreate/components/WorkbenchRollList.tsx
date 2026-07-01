import { Button, Checkbox, List, Space, Tag, Typography } from 'antd'
import { PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import type { Machine } from '../../../types/machine'
import type { PlanPreviewVO } from '../../../types/processOrder'
import { rollPreviewStatus } from '../previewStatusUtils'
import type { MergedSourceLock } from '../rewindConsumptionUtils'
import type { RollDraft } from '../types'

interface Props {
  machines: Machine[]
  rolls: RollDraft[]
  selectedId?: string
  checkedIds: string[]
  previews: Record<string, PlanPreviewVO>
  lockedRolls?: Record<string, MergedSourceLock>
  onClearSelection: () => void
  onSelect: (localId: string) => void
  onLockedSelect?: (roll: RollDraft, lock: MergedSourceLock) => void
  onToggle: (localId: string, checked: boolean) => void
  onSelectSameSpec: () => void
}

export default function WorkbenchRollList({
  machines,
  rolls,
  selectedId,
  checkedIds,
  previews,
  lockedRolls = {},
  onClearSelection,
  onSelect,
  onLockedSelect,
  onToggle,
  onSelectSameSpec,
}: Props) {
  return (
    <div>
      <Space wrap style={{ marginBottom: 8 }}>
        <Button size="small" onClick={onSelectSameSpec}>全选同规格</Button>
        <Button size="small" disabled={!checkedIds.length} onClick={onClearSelection}>全不选</Button>
      </Space>
      <List
        size="small"
        dataSource={rolls}
        renderItem={(roll, index) => {
          const lock = lockedRolls[roll.localId]
          const disabled = Boolean(lock)
          const status = rollPreviewStatus({ roll, preview: previews[roll.localId], lock })
          return (
            <List.Item
              onClick={() => disabled && lock ? onLockedSelect?.(roll, lock) : onSelect(roll.localId)}
              style={{
                cursor: disabled ? 'not-allowed' : 'pointer',
                border: selectedId === roll.localId ? '1px solid #1677ff' : '1px solid #f0f0f0',
                borderRadius: 6,
                marginBottom: 8,
                padding: 10,
                background: disabled ? '#f5f5f5' : selectedId === roll.localId ? '#e6f4ff' : '#fff',
                opacity: disabled ? 0.78 : 1,
              }}
            >
              <div style={{ width: '100%' }}>
                <Space align="start">
                  <Checkbox
                    checked={!disabled && checkedIds.includes(roll.localId)}
                    disabled={disabled}
                    onClick={(event) => event.stopPropagation()}
                    onChange={(event) => onToggle(roll.localId, event.target.checked)}
                  />
                  <div>
                    <Typography.Text strong>母卷 {index + 1}</Typography.Text>
                    <div style={{ color: '#666', fontSize: 12 }}>
                      {roll.rollNo || roll.paperName || '-'} / {roll.gramWeight}g / {roll.originalWidth}mm
                    </div>
                  </div>
                </Space>
                <div style={{ marginTop: 6 }}>
                  <Tag color={roll.processMode === 3 ? 'default' : 'blue'}>
                    {PROCESS_MODE[roll.processMode ?? 1]}
                  </Tag>
                  {roll.processMode !== 3 && <Tag color="green">{STEP_TYPE[roll.mainStepType ?? 2]}</Tag>}
                  {roll.processMode !== 3 && <Tag color={roll.machineUuid ? 'cyan' : 'default'}>{machineName(roll.machineUuid, machines)}</Tag>}
                  <Tag color={status.color}>{status.label}</Tag>
                </div>
              </div>
            </List.Item>
          )
        }}
      />
    </div>
  )
}

function machineName(machineUuid: string | undefined, machines: Machine[]) {
  if (!machineUuid) return '未选机台'
  return machines.find((machine) => machine.uuid === machineUuid)?.machineName ?? '未知机台'
}
