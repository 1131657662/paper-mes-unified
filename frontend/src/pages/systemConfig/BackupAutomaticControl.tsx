import { SaveOutlined } from '@ant-design/icons'
import { Button, Input, Space, Switch, Typography } from 'antd'
import { useState } from 'react'
import type { BackupStatus } from '../../types/dataBackup'

interface BackupAutomaticControlProps {
  status: BackupStatus
  busy: boolean
  onSave: (enabled: boolean, executionTime: string) => void
}

export default function BackupAutomaticControl({ status, busy, onSave }: BackupAutomaticControlProps) {
  const [enabled, setEnabled] = useState(status.automaticEnabled)
  const [executionTime, setExecutionTime] = useState(status.automaticExecutionTime)
  const changed = enabled !== status.automaticEnabled || executionTime !== status.automaticExecutionTime
  return (
    <div className="data-backup-automatic">
      <Space wrap>
        <Typography.Text strong>自动备份</Typography.Text>
        <Switch aria-label="启用自动备份" checked={enabled} disabled={busy} onChange={setEnabled} />
        <Input
          aria-label="自动备份执行时间"
          className="data-backup-time-input"
          type="time"
          value={executionTime}
          disabled={busy || !enabled}
          onChange={(event) => setExecutionTime(event.target.value)}
        />
        <Button
          icon={<SaveOutlined />}
          disabled={busy || !changed || !executionTime}
          onClick={() => onSave(enabled, executionTime)}
        >
          保存调度
        </Button>
      </Space>
      <Typography.Text type="secondary">{formatAutomaticSummary(status)}</Typography.Text>
    </div>
  )
}

function formatAutomaticSummary(status: BackupStatus) {
  if (!status.automaticEnabled) return '当前已停用'
  if (!status.enabled) return '等待管理端备份开启'
  if (!status.configured) return '等待备份运行环境就绪'
  const next = formatDate(status.nextAutomaticAt)
  const last = formatDate(status.lastAutomaticAt)
  const result = formatStatus(status.lastAutomaticStatus)
  const failures = status.automaticConsecutiveFailures
  return `下次 ${next} · 上次 ${last}（${result}） · 连续失败 ${failures} 次`
}

function formatStatus(value?: BackupStatus['lastAutomaticStatus']) {
  if (value === 'SUCCESS') return '成功'
  if (value === 'FAILED') return '失败'
  if (value === 'RUNNING') return '执行中'
  return '尚未执行'
}

function formatDate(value?: string) {
  return formatDateTime(value)
}
import { formatDateTime } from '../../utils/dateTime'
