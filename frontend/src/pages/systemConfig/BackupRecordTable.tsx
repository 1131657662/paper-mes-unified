import { SafetyCertificateOutlined } from '@ant-design/icons'
import { Button, Popconfirm, Table, Tag, Tooltip } from 'antd'
import { DISPLAY_TERMS } from '../../constants/displayTerms'
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
      scroll={{ x: 1060 }}
    />
  )
}

function createColumns(props: BackupRecordTableProps): ColumnsType<BackupRecord> {
  return [
    { title: '备份编号', dataIndex: 'id', width: 160 },
    { title: '创建时间', dataIndex: 'createdAt', width: 165, render: formatDate },
    { title: '大小', dataIndex: 'sizeBytes', width: 95, render: (_, record) => formatBytes(record) },
    { title: '备份状态', dataIndex: 'integrityStatus', width: 110, render: renderIntegrity },
    { title: '备份内容', key: 'contents', width: 250, render: (_, record) => renderContents(record) },
    { title: '恢复验证', dataIndex: 'verificationStatus', width: 100, render: (_, record) => renderVerification(record) },
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
  const verifyDisabled = busy || record.integrityStatus !== 'COMPLETE'
    || !record.databaseArchive || !record.checksumAvailable
  return (
    <div className="mes-table-actions">
      <Popconfirm
        title="将在隔离测试库中恢复并校验，确认开始？"
        disabled={verifyDisabled}
        onConfirm={() => onVerify(record.id)}
      >
        <Tooltip title={verifyDisabled ? '仅完整且包含校验文件的备份可以验证' : undefined}>
          <span><Button type="link" size="small" icon={<SafetyCertificateOutlined />}
            disabled={verifyDisabled}>{DISPLAY_TERMS.isolatedRestoreVerification}</Button></span>
        </Tooltip>
      </Popconfirm>
      <Popconfirm title="确认永久删除该备份？" description="备份文件和校验报告都会删除，操作不可撤销。" onConfirm={() => onDelete(record.id)}>
        <Button danger type="link" size="small" disabled={busy || deleteProtected}>删除</Button>
      </Popconfirm>
    </div>
  )
}

function renderIntegrity(value: BackupRecord['integrityStatus']) {
  if (value === 'COMPLETE') return <Tag color="success">完整</Tag>
  if (value === 'REVIEW') return <Tag color="warning">待核查</Tag>
  return <Tag color="error">不完整</Tag>
}

function renderContents(record: BackupRecord) {
  if (record.integrityStatus === 'REVIEW') return <Tag color="warning">目录读取异常</Tag>
  if (record.missingItems.length > 0) {
    const reason = record.missingItems.map((item) => `缺少${item}`).join('、')
    return <Tooltip title={reason}><span className="backup-record__issue">{reason}</span></Tooltip>
  }
  return <span>数据库、校验文件{record.uploadIncluded ? '、附件' : ''}</span>
}

function renderVerification(record: BackupRecord) {
  if (record.integrityStatus !== 'COMPLETE') return <Tag>不可验证</Tag>
  return record.verificationStatus === 'VERIFIED'
    ? <Tag color="success">已验证</Tag> : <Tag color="warning">{DISPLAY_TERMS.unverified}</Tag>
}

function formatDate(value?: string) {
  return formatDateTime(value)
}

function formatBytes(record: BackupRecord) {
  if (record.integrityStatus !== 'COMPLETE' && record.sizeBytes === 0) return '-'
  const value = record.sizeBytes
  if (value < 1024) return `${value} B`
  if (value < 1024 ** 2) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 ** 2).toFixed(1)} MB`
}
