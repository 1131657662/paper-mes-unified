import { useEffect, useRef } from 'react'
import { Alert, Empty, Spin } from 'antd'
import { RobotOutlined, UserOutlined } from '@ant-design/icons'
import type { ProcessAiMessage } from '../types'

interface Props {
  error?: string
  loading: boolean
  messages: ProcessAiMessage[]
  pendingUser?: string
  progress?: string
}

export default function ProcessAiMessageList(props: Props) {
  const endRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    endRef.current?.scrollIntoView({ block: 'end' })
  }, [props.messages.length, props.pendingUser, props.progress, props.error])

  if (props.loading) return <div className="process-ai-messages__loading"><Spin /></div>
  const isEmpty = props.messages.length === 0 && !props.pendingUser
  return <div className="process-ai-messages" aria-live="polite">
    {isEmpty && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无对话" />}
    {props.messages.map((item) => <MessageBubble key={item.sequenceNo} message={item} />)}
    {props.pendingUser && <LocalBubble role="USER" content={props.pendingUser} />}
    {props.progress && <LocalBubble role="ASSISTANT" content={props.progress} pending />}
    {props.error && <Alert type="error" showIcon message={props.error} />}
    <div ref={endRef} />
  </div>
}

function MessageBubble({ message }: { message: ProcessAiMessage }) {
  return <LocalBubble role={message.role} content={message.content}
    failed={message.status === 'FAILED'} pending={message.status === 'PARTIAL'} />
}

function LocalBubble({ role, content, failed, pending }: {
  role: ProcessAiMessage['role']; content: string; failed?: boolean; pending?: boolean
}) {
  return <div className={`process-ai-message process-ai-message--${role.toLowerCase()}`}>
    <div className="process-ai-message__avatar">
      {role === 'USER' ? <UserOutlined /> : <RobotOutlined spin={pending} />}
    </div>
    <div className={failed ? 'process-ai-message__body process-ai-message__body--failed'
      : 'process-ai-message__body'}>{content || (pending ? '正在处理…' : '')}</div>
  </div>
}
