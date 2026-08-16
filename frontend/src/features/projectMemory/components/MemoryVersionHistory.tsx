import { useState } from 'react'
import { Button, Form, Input, Modal, Table, Tag, Typography } from 'antd'
import { RollbackOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import type { ColumnsType } from 'antd/es/table'
import { useRollbackProjectMemory } from '../hooks/useRollbackProjectMemory'
import type { ProjectMemoryVersion } from '../types'

interface Props {
  activeVersion: string
  canManage: boolean
  items: ProjectMemoryVersion[]
  loading: boolean
}

export default function MemoryVersionHistory({ activeVersion, canManage, items, loading }: Props) {
  const [target, setTarget] = useState<ProjectMemoryVersion>()
  const [reason, setReason] = useState('')
  const [idempotencyKey, setIdempotencyKey] = useState('')
  const { mutate: rollbackMemory, isPending: isRollingBack } = useRollbackProjectMemory()
  const columns = versionColumns(canManage, openRollback)

  function openRollback(version: ProjectMemoryVersion) {
    setTarget(version)
    setReason('')
    setIdempotencyKey(crypto.randomUUID())
  }

  function confirmRollback() {
    if (!target || !reason.trim()) return
    rollbackMemory({ expectedMemoryVersion: activeVersion, targetMemoryVersion: target.memoryVersion,
      idempotencyKey, reason: reason.trim() }, { onSuccess: closeRollback })
  }

  function closeRollback() {
    setTarget(undefined)
    setReason('')
    setIdempotencyKey('')
  }

  return (
    <section className="project-memory-history">
      <Table<ProjectMemoryVersion> columns={columns} dataSource={items} loading={loading}
        pagination={{ pageSize: 10, showSizeChanger: false }} rowKey="memoryVersion" scroll={{ x: 900 }} size="small" />
      <Modal title={`回滚到 ${target?.memoryVersion ?? ''}`} open={Boolean(target)} confirmLoading={isRollingBack}
        okButtonProps={{ disabled: !reason.trim() }} okText="确认回滚" cancelText="取消"
        onCancel={closeRollback} onOk={confirmRollback}>
        <Form.Item htmlFor="project-memory-rollback-reason" label="回滚原因" required>
          <Input.TextArea id="project-memory-rollback-reason" aria-label="回滚原因" value={reason}
            maxLength={500} placeholder="请说明恢复到该版本的原因" rows={4} showCount
            onChange={(event) => setReason(event.target.value)} />
        </Form.Item>
      </Modal>
    </section>
  )
}

function versionColumns(canManage: boolean, onRollback: (version: ProjectMemoryVersion) => void) {
  const columns: ColumnsType<ProjectMemoryVersion> = [
    { title: '版本', dataIndex: 'memoryVersion', width: 110, render: versionCell },
    { title: '状态', dataIndex: 'status', width: 100, render: statusCell },
    { title: '变更原因', dataIndex: 'patchNotes', width: 230, ellipsis: true, render: textCell },
    { title: '创建人', dataIndex: 'createdBy', width: 110, render: textCell },
    { title: '批准人', dataIndex: 'approvedBy', width: 110, render: textCell },
    { title: '创建时间', dataIndex: 'createdAt', width: 160, render: timeCell },
    { title: 'Checksum', dataIndex: 'checksum', width: 190, ellipsis: true,
      render: (value: string) => <Typography.Text copyable ellipsis={{ tooltip: value }}>{value}</Typography.Text> },
  ]
  if (canManage) columns.push({ title: '操作', key: 'action', fixed: 'right', width: 90,
    render: (_, version) => version.status === 'ACTIVE' ? '-' : <Button type="link" size="small"
      icon={<RollbackOutlined />} onClick={() => onRollback(version)}>回滚</Button> })
  return columns
}

function versionCell(value: string, version: ProjectMemoryVersion) {
  return <Typography.Text strong={version.status === 'ACTIVE'}>{value}</Typography.Text>
}

function statusCell(status: ProjectMemoryVersion['status']) {
  if (status === 'ACTIVE') return <Tag color="success">生效中</Tag>
  if (status === 'DRAFT') return <Tag color="warning">草稿</Tag>
  return <Tag>历史</Tag>
}

function textCell(value?: string) { return value || '-' }
function timeCell(value: string) { return dayjs(value).format('YYYY-MM-DD HH:mm') }
