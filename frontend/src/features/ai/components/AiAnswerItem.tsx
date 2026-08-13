import { Divider, List, Space, Tag, Typography } from 'antd'
import type { AiAssistResponse } from '../types'

interface Props {
  response: AiAssistResponse
}

export default function AiAnswerItem({ response }: Props) {
  const color = response.decision === 'ANSWER'
    ? 'success'
    : response.decision === 'CLARIFY' ? 'warning' : 'error'

  return (
    <List.Item>
      <Space direction="vertical" size={8} style={{ width: '100%' }}>
        <Space wrap>
          <Tag color={color}>{response.decision}</Tag>
          <Tag>{response.confidence}</Tag>
          <Typography.Text type="secondary">{response.provider}</Typography.Text>
        </Space>
        <Typography.Paragraph style={{ marginBottom: 0 }}>{response.answer}</Typography.Paragraph>
        {response.safeNextSteps.length > 0 && (
          <>
            <Divider plain orientation="left">安全下一步</Divider>
            <List
              size="small"
              dataSource={response.safeNextSteps}
              renderItem={(step) => <List.Item>{step}</List.Item>}
            />
          </>
        )}
        {response.citations.length > 0 && (
          <Typography.Text type="secondary">
            依据：{response.citations.map((citation) => `${citation.ruleId} v${citation.version}`).join('、')}
          </Typography.Text>
        )}
      </Space>
    </List.Item>
  )
}
