import { useState } from 'react'
import {
  Alert, Button, Descriptions, Drawer, Input, Popconfirm, Skeleton, Space, Tabs, Tag, Typography,
} from 'antd'
import { CheckOutlined, CloseOutlined } from '@ant-design/icons'
import { useApproveProjectMemoryCandidate } from '../hooks/useApproveProjectMemoryCandidate'
import { useProjectMemoryCandidate } from '../hooks/useProjectMemoryCandidate'
import { useRejectProjectMemoryCandidate } from '../hooks/useRejectProjectMemoryCandidate'
import type { ProjectMemoryCandidate, ProjectMemoryCandidateDocument } from '../types'
import MemoryCandidateEvidenceList from './MemoryCandidateEvidenceList'

interface Props {
  currentMemoryVersion: string
  onClose: () => void
  uuid?: string
}

export default function MemoryCandidateDetailDrawer(props: Props) {
  const detail = useProjectMemoryCandidate(props.uuid)
  return <Drawer open={Boolean(props.uuid)} width="min(780px, calc(100vw - 24px))"
    title="候选记忆审核" destroyOnHidden onClose={props.onClose}>
    {detail.isLoading && <Skeleton active paragraph={{ rows: 10 }} />}
    {detail.isError && <Alert type="error" showIcon message="候选详情加载失败"
      action={<Button size="small" onClick={() => void detail.refetch()}>重试</Button>} />}
    {detail.data && <CandidateDetail key={detail.data.candidate.uuid}
      candidate={detail.data.candidate} evidence={detail.data.evidence}
      currentMemoryVersion={props.currentMemoryVersion} onReviewed={props.onClose} />}
  </Drawer>
}

function CandidateDetail(props: {
  candidate: ProjectMemoryCandidate
  currentMemoryVersion: string
  evidence: Parameters<typeof MemoryCandidateEvidenceList>[0]['items']
  onReviewed: () => void
}) {
  const candidate = props.candidate
  return <div className="memory-candidate-detail">
    <Descriptions size="small" bordered column={2}>
      <Descriptions.Item label="状态"><Tag>{statusLabel(candidate.status)}</Tag></Descriptions.Item>
      <Descriptions.Item label="类型">{typeLabel(candidate.candidateType)}</Descriptions.Item>
      <Descriptions.Item label="出现订单">{candidate.distinctOrderCount}</Descriptions.Item>
      <Descriptions.Item label="记忆编号">{candidate.memoryId}</Descriptions.Item>
    </Descriptions>
    <Tabs items={[
      { key: 'review', label: '审核内容', children: <CandidateReviewForm {...props} /> },
      { key: 'evidence', label: `证据记录 (${props.evidence.length})`,
        children: <MemoryCandidateEvidenceList items={props.evidence} /> },
    ]} />
  </div>
}

function CandidateReviewForm(props: {
  candidate: ProjectMemoryCandidate
  currentMemoryVersion: string
  onReviewed: () => void
}) {
  const [document, setDocument] = useState<ProjectMemoryCandidateDocument>(
    () => structuredClone(props.candidate.candidate))
  const [expected, setExpected] = useState(() => JSON.stringify(document.expected ?? {}, null, 2) ?? '{}')
  const [reason, setReason] = useState('')
  const [parseError, setParseError] = useState<string>()
  const approve = useApproveProjectMemoryCandidate()
  const reject = useRejectProjectMemoryCandidate()
  const canApprove = props.candidate.status === 'READY'
  const canReject = ['CANDIDATE', 'READY', 'CONFLICT'].includes(props.candidate.status)

  const update = (field: string, value: unknown) => setDocument((current) => ({ ...current, [field]: value }))
  const approveCandidate = () => {
    let edited = document
    if (document.type === 'EXAMPLE') {
      try {
        edited = { ...document, expected: JSON.parse(expected) as unknown }
        setParseError(undefined)
      } catch {
        setParseError('最终配置 JSON 格式不正确')
        return
      }
    }
    approve.mutate({ uuid: props.candidate.uuid,
      expectedMemoryVersion: props.currentMemoryVersion, idempotencyKey: crypto.randomUUID(),
      reason: reason.trim(), candidate: edited }, { onSuccess: props.onReviewed })
  }
  const rejectCandidate = () => reject.mutate({ uuid: props.candidate.uuid,
    reason: reason.trim() }, { onSuccess: props.onReviewed })

  return <Space direction="vertical" size="middle" className="memory-candidate-review-form">
    <CandidateFields document={document} expected={expected} onExpected={setExpected} onUpdate={update} />
    {parseError && <Alert type="error" showIcon message={parseError} />}
    <Input.TextArea aria-label="审核原因" rows={3} maxLength={500} showCount value={reason}
      placeholder="填写批准或拒绝原因" onChange={(event) => setReason(event.target.value)} />
    <Space>
      {canApprove && <Popconfirm title="批准后将生成新的正式项目记忆版本，是否继续？"
        okText="批准" cancelText="取消" onConfirm={approveCandidate}>
        <Button type="primary" icon={<CheckOutlined />} loading={approve.isPending}
          disabled={!reason.trim()}>编辑并批准</Button>
      </Popconfirm>}
      {canReject && <Popconfirm title="确认拒绝这条候选记忆？" okText="拒绝" cancelText="取消"
        onConfirm={rejectCandidate}>
        <Button danger icon={<CloseOutlined />} loading={reject.isPending}
          disabled={!reason.trim()}>拒绝</Button>
      </Popconfirm>}
      {!canApprove && <Typography.Text type="secondary">
        {props.candidate.status === 'CANDIDATE' ? '达到不同订单阈值后可批准' : '当前状态不可批准'}
      </Typography.Text>}
    </Space>
  </Space>
}

function CandidateFields({ document, expected, onExpected, onUpdate }: {
  document: ProjectMemoryCandidateDocument
  expected: string
  onExpected: (value: string) => void
  onUpdate: (field: string, value: unknown) => void
}) {
  const input = document.type === 'EXAMPLE' ? document.input : document.phrase
  return <Space direction="vertical" size="small" className="memory-candidate-review-form">
    <label>客户表达</label>
    <Input.TextArea aria-label="客户表达" rows={3} maxLength={2000} value={input}
      onChange={(event) => onUpdate(document.type === 'EXAMPLE' ? 'input' : 'phrase', event.target.value)} />
    <label>适用范围</label>
    <Input aria-label="适用范围" maxLength={80} value={document.scope}
      onChange={(event) => onUpdate('scope', event.target.value)} />
    {document.type === 'TERM' && <>
      <label>归一化意图</label>
      <Input aria-label="归一化意图" maxLength={120} value={document.intent}
        onChange={(event) => onUpdate('intent', event.target.value)} />
      <label>业务含义</label>
      <Input.TextArea aria-label="业务含义" rows={2} maxLength={500} value={document.meaning}
        onChange={(event) => onUpdate('meaning', event.target.value)} />
      <label>别名（逗号分隔）</label>
      <Input aria-label="别名" value={(document.aliases ?? []).join(',')}
        onChange={(event) => onUpdate('aliases', event.target.value.split(',').map((item) => item.trim()).filter(Boolean))} />
    </>}
    {document.type === 'EXAMPLE' && <>
      <label>最终有效配置</label>
      <Input.TextArea aria-label="最终有效配置" className="memory-candidate-review-form__json" rows={12}
        value={expected} onChange={(event) => onExpected(event.target.value)} />
    </>}
  </Space>
}

function statusLabel(status: ProjectMemoryCandidate['status']) {
  return { CANDIDATE: '收集中', READY: '待审核', ACTIVE: '已批准', CONFLICT: '有冲突',
    REJECTED: '已拒绝', EXPIRED: '已过期' }[status]
}

function typeLabel(type: ProjectMemoryCandidate['candidateType']) {
  return { TERM: '术语', EXAMPLE: '案例', RULE: '规则', EXTERNAL_FACT: '外部事实', EPISODE: '事件' }[type]
}
