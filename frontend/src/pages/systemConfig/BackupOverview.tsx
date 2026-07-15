import { DatabaseOutlined, ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Descriptions, Popconfirm, Space, Switch, Tag, Typography } from 'antd'
import type { BackupStatus } from '../../types/dataBackup'
import BackupAutomaticControl from './BackupAutomaticControl'
import BackupRetentionControl from './BackupRetentionControl'

interface BackupOverviewProps {
  status?: BackupStatus
  busy: boolean
  loading: boolean
  unavailable: boolean
  actions: BackupOverviewActions
}

export interface BackupOverviewActions {
  backup: () => void
  cleanup: () => void
  refresh: () => void
  saveRetention: (days: number) => void
  saveAutomatic: (enabled: boolean, executionTime: string) => void
  toggle: (enabled: boolean) => void
}

export default function BackupOverview(props: BackupOverviewProps) {
  const { status, busy, loading, unavailable } = props
  return (
    <>
      <div className="data-backup-toolbar">
        <div>
          <Typography.Title level={5}>数据库与附件备份</Typography.Title>
          <Typography.Text type="secondary">生产库恢复仅允许通过服务器脚本和双重确认执行。</Typography.Text>
        </div>
        <Space wrap>
          <Typography.Text>管理端备份</Typography.Text>
          <Switch
            aria-label="启用管理端备份"
            checked={status?.enabled ?? true}
            disabled={busy}
            loading={loading}
            onChange={props.actions.toggle}
          />
          <Button icon={<ReloadOutlined />} onClick={props.actions.refresh}>刷新</Button>
          <Popconfirm title="确认立即创建完整备份？" onConfirm={props.actions.backup} okText="开始备份" cancelText="取消">
            <Button type="primary" icon={<DatabaseOutlined />} disabled={unavailable} loading={busy}>立即备份</Button>
          </Popconfirm>
        </Space>
      </div>
      <BackupNotice status={status} />
      <Descriptions size="small" column={3} className="data-backup-summary">
        <Descriptions.Item label="运行状态">{formatRunning(status)}</Descriptions.Item>
        <Descriptions.Item label="运行环境">{formatRuntime(status)}</Descriptions.Item>
        <Descriptions.Item label="备份数量">{status?.backupCount ?? 0} 份</Descriptions.Item>
        <Descriptions.Item label="最近备份">{formatDate(status?.latestBackupAt)}</Descriptions.Item>
        <Descriptions.Item label="最近演练">{formatDate(status?.latestVerifiedAt)}</Descriptions.Item>
        <Descriptions.Item label="磁盘空间">{formatDisk(status)}</Descriptions.Item>
        <Descriptions.Item label="异地同步">{formatOffsiteStatus(status)}</Descriptions.Item>
        <Descriptions.Item label="最近同步">{formatDate(status?.offsiteLastSyncAt)}</Descriptions.Item>
        <Descriptions.Item label="远端名称">{status?.offsiteRemoteName || '-'}</Descriptions.Item>
      </Descriptions>
      {status && (
        <BackupAutomaticControl
          key={`${status.automaticEnabled}-${status.automaticExecutionTime}`}
          status={status}
          busy={busy}
          onSave={props.actions.saveAutomatic}
        />
      )}
      {status && (
        <BackupRetentionControl
          key={status.retentionDays}
          retentionDays={status.retentionDays}
          busy={busy}
          onCleanup={props.actions.cleanup}
          onSave={props.actions.saveRetention}
        />
      )}
    </>
  )
}

function BackupNotice({ status }: { status?: BackupStatus }) {
  if (!status) return null
  if (!status.enabled) return <Alert type="warning" showIcon message="管理端备份已停用" description="管理员可通过右上角开关重新启用。" />
  if (!status.configured) return <Alert type="warning" showIcon message="管理端备份已启用，但运行环境不完整" description={`缺少：${status.missingComponents?.join('、') || '未知组件'}`} />
  if (status.usableSpaceBytes < 1024 ** 3) return <Alert type="error" showIcon message="备份磁盘可用空间不足 1 GB" />
  if (status.offsiteStatus === 'FAILED') return <Alert type="error" showIcon message="最近一次异地备份同步失败" description="请查看服务器异地同步日志并处理后重新执行。" />
  if (status.offsiteStatus === 'INVALID') return <Alert type="warning" showIcon message="异地备份状态文件不可用" description="本地备份不受影响，请检查同步脚本生成的状态文件。" />
  if ((status.latestBackupAgeHours ?? 0) > 48) return <Alert type="warning" showIcon message="最近一次备份已超过 48 小时" />
  return <Alert type="info" showIcon message={status.message || '备份服务可用'} />
}

function formatOffsiteStatus(status?: BackupStatus) {
  if (!status || status.offsiteStatus === 'NOT_CONFIGURED') return <Tag>未配置或尚未同步</Tag>
  if (status.offsiteStatus === 'SUCCESS') return <Tag color="success">成功</Tag>
  if (status.offsiteStatus === 'FAILED') return <Tag color="error">失败</Tag>
  return <Tag color="warning">状态不可用</Tag>
}

function formatRunning(status?: BackupStatus) {
  if (!status?.running) return '空闲'
  return status.runningOperation === 'VERIFY' ? '恢复演练中' : '备份中'
}

function formatRuntime(status?: BackupStatus) {
  if (!status) return '-'
  return `${status.platform === 'WINDOWS' ? 'Windows' : 'Linux'} / ${status.runner === 'POWERSHELL' ? 'PowerShell' : 'Bash'}`
}

function formatDate(value?: string) {
  return formatDateTime(value)
}

function formatDisk(status?: BackupStatus) {
  if (!status) return '-'
  return `${formatBytes(status.usableSpaceBytes)} 可用 / ${formatBytes(status.totalSpaceBytes)}`
}

function formatBytes(value: number) {
  if (value < 1024 ** 3) return `${(value / 1024 ** 2).toFixed(1)} MB`
  return `${(value / 1024 ** 3).toFixed(1)} GB`
}
import { formatDateTime } from '../../utils/dateTime'
