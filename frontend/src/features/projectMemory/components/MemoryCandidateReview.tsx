import { useState } from 'react'
import { Button, Select, Space, Table, Tag } from 'antd'
import { EyeOutlined, ReloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import type { ColumnsType } from 'antd/es/table'
import { useProjectMemoryCandidates } from '../hooks/useProjectMemoryCandidates'
import type { ProjectMemoryCandidate, ProjectMemoryCandidateStatus } from '../types'
import MemoryCandidateDetailDrawer from './MemoryCandidateDetailDrawer'

interface Props {
  currentMemoryVersion: string
}

export default function MemoryCandidateReview({ currentMemoryVersion }: Props) {
  const [status, setStatus] = useState<ProjectMemoryCandidateStatus>()
  const [selectedUuid, setSelectedUuid] = useState<string>()
  const { data: candidates = [], isLoading, refetch } = useProjectMemoryCandidates(status)

  return <section className="project-memory-candidates">
    <div className="project-memory-section-head">
      <Space>
        <strong>候选记忆</strong>
        <Select allowClear value={status} placeholder="全部状态" options={statusOptions}
          onChange={(value) => setStatus(value)} />
      </Space>
      <Button icon={<ReloadOutlined />} onClick={() => void refetch()}>刷新</Button>
    </div>
    <Table<ProjectMemoryCandidate> rowKey="uuid" size="middle" loading={isLoading}
      columns={candidateColumns(setSelectedUuid)} dataSource={candidates}
      pagination={{ pageSize: 20, showSizeChanger: false }} scroll={{ x: 980 }} />
    <MemoryCandidateDetailDrawer uuid={selectedUuid} currentMemoryVersion={currentMemoryVersion}
      onClose={() => setSelectedUuid(undefined)} />
  </section>
}

const statusOptions = [
  { value: 'CANDIDATE', label: '收集中' },
  { value: 'READY', label: '待审核' },
  { value: 'CONFLICT', label: '有冲突' },
  { value: 'ACTIVE', label: '已批准' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'EXPIRED', label: '已过期' },
] satisfies { value: ProjectMemoryCandidateStatus; label: string }[]

function candidateColumns(open: (uuid: string) => void): ColumnsType<ProjectMemoryCandidate> {
  return [
    { title: '候选内容', width: 300, ellipsis: true,
      render: (_, item) => item.candidate.phrase || item.candidate.input || item.memoryId },
    { title: '类型', dataIndex: 'candidateType', width: 100, render: candidateType },
    { title: '范围', width: 150, render: (_, item) => item.candidate.scope || '-' },
    { title: '证据订单', dataIndex: 'distinctOrderCount', width: 100 },
    { title: '状态', dataIndex: 'status', width: 100, render: candidateStatus },
    { title: '最近出现', dataIndex: 'lastSeenAt', width: 155,
      render: (value: string) => dayjs(value).format('YYYY-MM-DD HH:mm') },
    { title: '审核备注', dataIndex: 'reviewNotes', width: 180, ellipsis: true,
      render: (value?: string) => value || '-' },
    { title: '操作', key: 'action', fixed: 'right', width: 100,
      render: (_, item) => <Button type="link" size="small" icon={<EyeOutlined />}
        onClick={() => open(item.uuid)}>查看</Button> },
  ]
}

function candidateType(type: ProjectMemoryCandidate['candidateType']) {
  return { TERM: '术语', EXAMPLE: '案例', RULE: '规则', EXTERNAL_FACT: '外部事实', EPISODE: '事件' }[type]
}

function candidateStatus(status: ProjectMemoryCandidateStatus) {
  const labels: Record<ProjectMemoryCandidateStatus, string> = {
    CANDIDATE: '收集中', READY: '待审核', ACTIVE: '已批准', CONFLICT: '有冲突',
    REJECTED: '已拒绝', EXPIRED: '已过期',
  }
  const color = status === 'READY' ? 'processing' : status === 'ACTIVE' ? 'success'
    : status === 'CONFLICT' ? 'error' : status === 'CANDIDATE' ? 'default' : 'warning'
  return <Tag color={color}>{labels[status]}</Tag>
}
