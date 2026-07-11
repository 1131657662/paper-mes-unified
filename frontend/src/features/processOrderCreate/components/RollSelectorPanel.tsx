import { Badge, List, Tag, Typography } from 'antd'
import { PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import type { Machine } from '../../../types/machine'
import { formatKg, formatMm } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'

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
      renderItem={(roll, index) => {
        const selected = selectedId === roll.localId
        return (
          <List.Item
            onClick={() => onSelect(roll.localId)}
            style={{
              cursor: 'pointer',
              border: selected ? '1px solid #1677ff' : '1px solid #f0f0f0',
              borderRadius: 6,
              marginBottom: 8,
              padding: 10,
              background: selected ? '#e6f4ff' : '#fff',
            }}
          >
            <div style={{ width: '100%' }}>
              <Typography.Text strong>原纸 {index + 1}</Typography.Text>
              {configuredIds.includes(roll.localId) && <Badge status="success" text="已配置" style={{ marginLeft: 8 }} />}
              <div style={{ color: '#666', fontSize: 12, marginTop: 4 }}>
                {roll.paperName || '-'} / {formatMm(roll.originalWidth)} / {formatKg(roll.rollWeight)}
              </div>
              <div style={{ marginTop: 6 }}>
                <Tag color={roll.processMode === 3 ? 'default' : 'blue'}>{PROCESS_MODE[roll.processMode ?? 1]}</Tag>
                {roll.processMode !== 3 && <Tag color="green">{STEP_TYPE[roll.mainStepType ?? 2]}</Tag>}
                {roll.processMode !== 3 && <Tag color={roll.machineUuid ? 'cyan' : 'default'}>{machineName(roll.machineUuid, machines)}</Tag>}
              </div>
            </div>
          </List.Item>
        )
      }}
    />
  )
}

function machineName(machineUuid: string | undefined, machines: Machine[]) {
  if (!machineUuid) return '未选机台'
  return machines.find((machine) => machine.uuid === machineUuid)?.machineName ?? '未知机台'
}
