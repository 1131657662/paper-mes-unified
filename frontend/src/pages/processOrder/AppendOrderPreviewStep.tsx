import { Button, Card, Descriptions, Space, Table, Tag, Typography } from 'antd'
import { PROCESS_MODE, STEP_TYPE } from '../../constants/processOrder'
import type { PlanPreviewVO } from '../../types/processOrder'
import { formatKg } from '../../utils/numberFormatters'
import { totalWeight } from '../../features/processOrderCreate/draftMappers'
import type { RollDraft } from '../../features/processOrderCreate/types'

interface Props {
  rolls: RollDraft[]
  configuredIds: string[]
  previews: Record<string, PlanPreviewVO>
  submitting: boolean
  onPrev: () => void
  onSubmit: () => void
}

export default function AppendOrderPreviewStep(props: Props) {
  const incomplete = props.rolls.filter((roll) => roll.processMode !== 3
    && !props.configuredIds.includes(roll.localId))
  return (
    <Card title="追加内容确认">
      <Descriptions bordered size="small" column={3} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="新增母卷">{props.rolls.length} 条</Descriptions.Item>
        <Descriptions.Item label="新增件数">
          {props.rolls.reduce((sum, roll) => sum + Number(roll.pieceNum ?? 1), 0)} 件
        </Descriptions.Item>
        <Descriptions.Item label="新增重量">{formatKg(totalWeight(props.rolls))}</Descriptions.Item>
      </Descriptions>
      <Table
        rowKey="localId"
        pagination={false}
        size="small"
        dataSource={props.rolls}
        columns={[
          { title: '母卷', render: (_, roll) => roll.rollNo || roll.paperName },
          { title: '件数', dataIndex: 'pieceNum', width: 90 },
          { title: '加工方式', render: (_, roll) => <Tag>{PROCESS_MODE[roll.processMode ?? 1]}</Tag> },
          { title: '主工艺', render: (_, roll) => STEP_TYPE[roll.mainStepType ?? 0] ?? '-' },
          {
            title: '配置状态',
            render: (_, roll) => roll.processMode === 3
              ? <Tag>无需配置</Tag>
              : <Tag color={props.configuredIds.includes(roll.localId) ? 'success' : 'error'}>
                {props.configuredIds.includes(roll.localId) ? '已确认' : '未完成'}
              </Tag>,
          },
          { title: '预计成品', render: (_, roll) => props.previews[roll.localId]?.finishCount ?? '-' },
        ]}
      />
      {incomplete.length > 0 && (
        <Typography.Paragraph type="danger" style={{ marginTop: 16 }}>
          还有 {incomplete.length} 条新增母卷未完成工艺确认，暂不能提交。
        </Typography.Paragraph>
      )}
      <div className="create-editor-footer">
        <Typography.Text type="secondary">提交后只新增本次母卷及其工艺，不改动原有成品号和生产数据。</Typography.Text>
        <Space>
          <Button onClick={props.onPrev}>上一步</Button>
          <Button type="primary" disabled={incomplete.length > 0} loading={props.submitting} onClick={props.onSubmit}>
            确认追加
          </Button>
        </Space>
      </div>
    </Card>
  )
}
