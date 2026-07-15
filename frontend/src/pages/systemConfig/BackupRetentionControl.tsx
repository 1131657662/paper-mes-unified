import { DeleteOutlined, SaveOutlined } from '@ant-design/icons'
import { Button, InputNumber, Popconfirm, Space, Typography } from 'antd'
import { useState } from 'react'

interface BackupRetentionControlProps {
  retentionDays: number
  busy: boolean
  onCleanup: () => void
  onSave: (days: number) => void
}

export default function BackupRetentionControl({ retentionDays, busy, onCleanup, onSave }: BackupRetentionControlProps) {
  const [days, setDays] = useState(retentionDays)
  return (
    <div className="data-backup-retention">
      <Space wrap>
        <Typography.Text strong>本地保留策略</Typography.Text>
        <InputNumber
          aria-label="本地备份保留天数"
          min={7}
          max={3650}
          value={days}
          onChange={(value) => setDays(value ?? retentionDays)}
        />
        <Typography.Text type="secondary">天</Typography.Text>
        <Button icon={<SaveOutlined />} disabled={busy || days === retentionDays} onClick={() => onSave(days)}>保存策略</Button>
      </Space>
      <Popconfirm title="立即按当前保留策略清理？" description="会保留最新有效备份，删除后不可恢复。" onConfirm={onCleanup}>
        <Button danger icon={<DeleteOutlined />} disabled={busy}>清理过期备份</Button>
      </Popconfirm>
    </div>
  )
}
