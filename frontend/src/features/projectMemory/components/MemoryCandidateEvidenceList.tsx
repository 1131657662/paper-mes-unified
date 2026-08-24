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
    <Descriptions.Item label="脱敏短语">{item.phrase || '-'}</Descriptions.Item>
    <Descriptions.Item label="AI建议">
      <JsonValue value={item.proposedValue} empty="该证据来自人工最终配置" />
    </Descriptions.Item>
    <Descriptions.Item label="最终有效配置">
      <JsonValue value={item.finalValue} />
    </Descriptions.Item>
    <Descriptions.Item label="确认差异">
      <JsonValue value={item.difference} />
    </Descriptions.Item>
  </Descriptions>
}

function JsonValue({ value, empty = '-' }: { value: unknown; empty?: string }) {
  if (value == null) return <Typography.Text type="secondary">{empty}</Typography.Text>
  return <pre className="memory-evidence__json">{JSON.stringify(value, null, 2)}</pre>
}
