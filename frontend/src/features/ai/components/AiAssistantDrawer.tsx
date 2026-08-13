import {
  Alert,
  Button,
  Drawer,
  Empty,
  Input,
  List,
  Space,
} from 'antd'
import { CloseOutlined, SendOutlined } from '@ant-design/icons'
import { useState } from 'react'
import { useAiAssist } from '../hooks/useAiAssist'
import { useAiStatus } from '../hooks/useAiStatus'
import AiAnswerItem from './AiAnswerItem'
import type { AiAssistResponse } from '../types'

interface Props {
  open: boolean
  onClose: () => void
  pageTemplate: string
  contextEpoch: string
}

export default function AiAssistantDrawer({ open, onClose, pageTemplate, contextEpoch }: Props) {
  const [question, setQuestion] = useState('')
  const [history, setHistory] = useState<AiAssistResponse[]>([])
  const mutation = useAiAssist(contextEpoch)
  const { data: status, isError: isStatusError } = useAiStatus(open)
  const isBusy = mutation.isPending
  const isAvailable = status?.enabled === true
    && status.rulesReady
    && status.dataMode === 'FAQ_ONLY'

  function submit() {
    const trimmed = question.trim()
    if (!trimmed || isBusy || !isAvailable) return
    setQuestion('')
    mutation.mutate({ question: trimmed, pageTemplate, contextEpoch }, {
      onSuccess: (response) => setHistory((items) => [response, ...items].slice(0, 5)),
    })
  }

  return (
    <Drawer
      title="智能助手"
      width={480}
      open={open}
      destroyOnHidden
      closable={false}
      extra={(
        <Button
          type="text"
          icon={<CloseOutlined />}
          aria-label="关闭智能助手"
          onClick={onClose}
        />
      )}
      onClose={onClose}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          type={isAvailable ? 'info' : 'warning'}
          showIcon
          message={isAvailable
            ? status.provider === 'ZHIPU'
              ? '当前由已审核规则约束，智谱仅改写规则答案'
              : '当前仅使用已审核本地规则'
            : isStatusError ? '无法确认智能助手状态' : '智能助手当前未启用'}
          description="不读取订单、客户、价格或日志明细，不执行任何业务操作。"
        />
        {history.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="输入错误码或业务规则问题" />}
        <List
          dataSource={history}
          locale={{ emptyText: null }}
          renderItem={(item) => <AiAnswerItem response={item} />}
        />
        {mutation.isError && <Alert type="error" showIcon message="智能助手暂时不可用" description="请稍后重试，MES 主业务不受影响。" />}
        <Input.TextArea
          value={question}
          disabled={!isAvailable}
          maxLength={1000}
          showCount
          autoSize={{ minRows: 3, maxRows: 6 }}
          placeholder="例如：E001 为什么不能操作？"
          onChange={(event) => setQuestion(event.target.value)}
          onPressEnter={(event) => {
            if (!event.shiftKey) {
              event.preventDefault()
              submit()
            }
          }}
        />
        <Button
          type="primary"
          icon={<SendOutlined />}
          loading={isBusy}
          disabled={!isAvailable}
          onClick={submit}
          block
        >
          发送问题
        </Button>
      </Space>
    </Drawer>
  )
}
