import { SafetyCertificateOutlined } from '@ant-design/icons'
import { Button, Popconfirm, Table, Tag } from 'antd'
import { formatDateTime } from '../../utils/dateTime'
import type { ColumnsType } from 'antd/es/table'
import type { BackupRecord } from '../../types/dataBackup'
import { isDeleteProtected } from './backupRecordProtection'

interface BackupRecordTableProps {
  records: BackupRecord[]
  busy: boolean
  loading: boolean
  onDelete: (backupId: string) => void
  onVerify: (backupId: string) => void
}

export default function BackupRecordTable(props: BackupRecordTableProps) {
  const columns = createColumns(props)
  return (
    <Table<BackupRecord>
      rowKey="id"
      size="small"
      loading={props.loading}
      columns={columns}
      dataSource={props.records}
      pagination={{ pageSize: 10, showSizeChanger: false }}
      scroll={{ x: 980 }}
    />
  )
}

function createColumns(props: BackupRecordTableProps): ColumnsType<BackupRecord> {
  return [
    { title: '备份编号', dataIndex: 'id', width: 160 },
    { title: '创建时间', dataIndex: 'createdAt', width: 165, render: formatDate },
    { title: '大小', dataIndex: 'sizeBytes', width: 95, render: formatBytes },
    { title: '数据库', dataIndex: 'databaseArchive', width: 80, render: renderAvailability },
    { title: '附件', dataIndex: 'uploadIncluded', width: 70, render: renderAvailability },
    { title: '校验', dataIndex: 'checksumAvailable', width: 70, render: renderAvailability },
    { title: '恢复演练', dataIndex: 'verificationStatus', width: 100, render: renderVerification },
    {
      title: '操作', key: 'action', fixed: 'right', width: 180,
      render: (_, record) => (
        <RecordActions
          record={record}
          busy={props.busy}
          deleteProtected={isDeleteProtected(record, props.records)}
          onDelete={props.onDelete}
          onVerify={props.onVerify}
        />
      ),
    },
  ]
}

interface RecordActionsProps {
  record: BackupRecord
  busy: boolean
  deleteProtected: boolean
  onDelete: (backupId: string) => void
  onVerify: (backupId: string) => void
}

function RecordActions({ record, busy, deleteProtected, onDelete, onVerify }: RecordActionsProps) {
  return (
    <div className="mes-table-actions">
      <Popconfirm title="将在隔离测试库中恢复并校验，确认开始？" onConfirm={() => onVerify(record.id)}>
        <Button type="link" size="small" icon={<SafetyCertificateOutlined />} disabled={busy || !record.checksumAvailable}>恢复演练</Button>
      </Popconfirm>
      <Popconfirm title="确认永久删除该备份？" description="备份文件和校验报告都会删除，操作不可撤销。" onConfirm={() => onDelete(record.id)}>
        <Button danger type="link" size="small" disabled={busy || deleteProtected}>删除</Button>
      </Popconfirm>
    </div>
  )
}

function renderAvailability(value: boolean) {
  return value ? <Tag color="success">完整</Tag> : <Tag>无</Tag>
}

function renderVerification(value: BackupRecord['verificationStatus']) {
  return value === 'VERIFIED' ? <Tag color="success">已通过</Tag> : <Tag color="warning">未演练</Tag>
}

function formatDate(value?: string) {
  return formatDateTime(value)
}

function formatBytes(value: number) {
  if (value < 1024) return `${value} B`
  if (value < 1024 ** 2) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 ** 2).toFixed(1)} MB`
}
