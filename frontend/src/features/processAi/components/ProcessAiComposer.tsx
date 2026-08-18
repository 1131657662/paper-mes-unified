import { useState } from 'react'
import { Button, Input } from 'antd'
import { ReloadOutlined, SendOutlined, StopOutlined } from '@ant-design/icons'

interface Props {
  disabled?: boolean
  streaming: boolean
  onCancel: () => void
  onRetry: () => Promise<void>
  onSend: (message: string) => Promise<void>
  retryable: boolean
}

export default function ProcessAiComposer({ disabled, streaming, onCancel, onRetry, onSend, retryable }: Props) {
  const [message, setMessage] = useState('')
  const submit = () => {
    const value = message.trim()
    if (!value || disabled || streaming) return
    setMessage('')
    void onSend(value)
  }

  return <div className="process-ai-composer">
    <Input.TextArea value={message} maxLength={2000} showCount disabled={disabled || streaming}
      autoSize={{ minRows: 3, maxRows: 7 }} placeholder="粘贴客户原话，或补充上一轮问题"
      onChange={(event) => setMessage(event.target.value)}
      onPressEnter={(event) => {
        if (event.shiftKey) return
        event.preventDefault()
        submit()
      }} />
    <div className="process-ai-composer__actions">
      {retryable && !streaming && <Button icon={<ReloadOutlined />} disabled={disabled}
        onClick={() => void onRetry()}>重试</Button>}
      {streaming
        ? <Button icon={<StopOutlined />} onClick={onCancel}>停止</Button>
        : <Button type="primary" icon={<SendOutlined />} disabled={disabled || !message.trim()}
          onClick={submit}>发送</Button>}
    </div>
  </div>
}
