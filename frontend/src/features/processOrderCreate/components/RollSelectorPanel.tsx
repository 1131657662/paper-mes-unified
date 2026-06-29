import { Badge, List, Tag, Typography } from 'antd'
import { PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import type { RollDraft } from '../types'

interface Props {
  rolls: RollDraft[]
  selectedId?: string
  configuredIds?: string[]
  onSelect: (localId: string) => void
}

export default function RollSelectorPanel({ rolls, selectedId, configuredIds = [], onSelect }: Props) {
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
                {roll.paperName || '-'} / {roll.originalWidth}mm / {roll.rollWeight}kg
              </div>
              <div style={{ marginTop: 6 }}>
                <Tag color={roll.processMode === 3 ? 'default' : 'blue'}>{PROCESS_MODE[roll.processMode ?? 1]}</Tag>
                {roll.processMode !== 3 && <Tag color="green">{STEP_TYPE[roll.mainStepType ?? 2]}</Tag>}
              </div>
            </div>
          </List.Item>
        )
      }}
    />
  )
}
