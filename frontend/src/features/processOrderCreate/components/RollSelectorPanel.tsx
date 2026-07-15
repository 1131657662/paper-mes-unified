import { Badge, List, Tag, Typography } from 'antd'
import { PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import type { Machine } from '../../../types/machine'
import { formatKg, formatMm } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'
import '../../../components/processOrder/ProcessOrderShared.css'

interface Props {
  machines: Machine[]
  rolls: RollDraft[]
  selectedId?: string
  configuredIds?: string[]
  onSelect: (localId: string) => void
}

export default function RollSelectorPanel({ machines, rolls, selectedId, configuredIds = [], onSelect }: Props) {
  return (
    <List
      size="small"
      dataSource={rolls}
      renderItem={(roll, index) => (
        <RollSelectorItem
          configured={configuredIds.includes(roll.localId)}
          index={index}
          machines={machines}
          roll={roll}
          selected={selectedId === roll.localId}
          onSelect={onSelect}
        />
      )}
    />
  )
}

interface RollSelectorItemProps {
  configured: boolean
  index: number
  machines: Machine[]
  roll: RollDraft
  selected: boolean
  onSelect: (localId: string) => void
}

function RollSelectorItem({ configured, index, machines, roll, selected, onSelect }: RollSelectorItemProps) {
  const className = `process-roll-option${selected ? ' process-roll-option--selected' : ''}`
  return (
    <List.Item className={className} onClick={() => onSelect(roll.localId)}>
      <div className="process-roll-option__content">
        <Typography.Text strong>原纸 {index + 1}</Typography.Text>
        {configured && <Badge className="process-roll-option__status" status="success" text="已配置" />}
        <div className="process-roll-option__meta">
          {roll.paperName || '-'} / {formatMm(roll.originalWidth)} / {formatKg(roll.rollWeight)}
        </div>
        <div className="process-roll-option__tags">
          <Tag color={roll.processMode === 3 ? 'default' : 'blue'}>{PROCESS_MODE[roll.processMode ?? 1]}</Tag>
          {roll.processMode !== 3 && <Tag color="green">{STEP_TYPE[roll.mainStepType ?? 2]}</Tag>}
          {roll.processMode !== 3 && <Tag color={roll.machineUuid ? 'cyan' : 'default'}>{machineName(roll.machineUuid, machines)}</Tag>}
        </div>
      </div>
    </List.Item>
  )
}

function machineName(machineUuid: string | undefined, machines: Machine[]) {
  if (!machineUuid) return '未选机台'
  return machines.find((machine) => machine.uuid === machineUuid)?.machineName ?? '未知机台'
}
