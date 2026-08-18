import { Collapse, Descriptions, Empty, Space, Tag, Typography } from 'antd'
import dayjs from 'dayjs'
import type { ProjectMemoryCandidateEvidence } from '../types'

interface Props {
  items: ProjectMemoryCandidateEvidence[]
}

export default function MemoryCandidateEvidenceList({ items }: Props) {
  if (items.length === 0) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE}
    description="暂无证据记录" />
  return <Collapse items={items.map((item) => ({
    key: item.uuid,
    label: <EvidenceTitle item={item} />,
    children: <EvidenceBody item={item} />,
  }))} />
}

function EvidenceTitle({ item }: { item: ProjectMemoryCandidateEvidence }) {
  return <Space wrap>
    <strong>{item.orderNo || item.orderUuid}</strong>
    <Tag color={item.sourceType === 'AI_CONFIRMED' ? 'blue' : 'gold'}>
      {item.sourceType === 'AI_CONFIRMED' ? 'AI确认应用' : '人工最终配置'}
    </Tag>
    {item.previewReady != null && <Tag color={item.previewReady ? 'success' : 'error'}>
      {item.previewReady ? '工艺校验通过' : '工艺校验未通过'}
    </Tag>}
    <Typography.Text type="secondary">
      {dayjs(item.createdAt).format('YYYY-MM-DD HH:mm')}
    </Typography.Text>
  </Space>
}

function EvidenceBody({ item }: { item: ProjectMemoryCandidateEvidence }) {
  return <Descriptions size="small" bordered column={1} styles={{ label: { width: 120 } }}>
    <Descriptions.Item label="客户原话">
      <Typography.Paragraph className="memory-evidence__phrase" copyable>
        {item.phrase || contextRequirement(item.context) || '-'}
      </Typography.Paragraph>
    </Descriptions.Item>
    <Descriptions.Item label="母卷与基线">
      <JsonValue value={item.context} />
    </Descriptions.Item>
    <Descriptions.Item label="AI建议">
      <JsonValue value={item.proposedValue} empty="该证据来自人工最终配置" />
    </Descriptions.Item>
    <Descriptions.Item label="最终有效配置">
      <JsonValue value={item.finalValue} />
    </Descriptions.Item>
    <Descriptions.Item label="确认差异">
      <JsonValue value={item.difference} />
    </Descriptions.Item>
    <Descriptions.Item label="记录人">{item.createdBy || '-'}</Descriptions.Item>
  </Descriptions>
}

function JsonValue({ value, empty = '-' }: { value: unknown; empty?: string }) {
  if (value == null) return <Typography.Text type="secondary">{empty}</Typography.Text>
  return <pre className="memory-evidence__json">{JSON.stringify(value, null, 2)}</pre>
}

function contextRequirement(value: unknown) {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return undefined
  const requirement = (value as Record<string, unknown>).customerRequirement
  return typeof requirement === 'string' ? requirement : undefined
}
