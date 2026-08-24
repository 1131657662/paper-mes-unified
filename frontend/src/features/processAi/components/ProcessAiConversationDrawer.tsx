import { useState } from 'react'
import { Alert, Button, Tag, Tooltip } from 'antd'
import { CloseOutlined, DragOutlined, MinusOutlined } from '@ant-design/icons'
import type { RollDraft } from '../../processOrderCreate/types'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import { useDraggableAssistantWindow } from '../hooks/useDraggableAssistantWindow'
import { useProcessAiMessages } from '../hooks/useProcessAiMessages'
import { useProcessAiStream } from '../hooks/useProcessAiStream'
import { useRefreshProcessAiMemory } from '../hooks/useRefreshProcessAiMemory'
import { latestStoredProcessAiResult } from '../processAiStoredResult'
import { processAiModelLabel } from '../processAiModelLabel'
import type { ProcessAiConfirmResponse, ProcessAiSession, ProcessAiStatus } from '../types'
import ProcessAiComposer from './ProcessAiComposer'
import ProcessAiMessageList from './ProcessAiMessageList'
import ProcessAiResultPanel from './ProcessAiResultPanel'
import './ProcessAiConversationDrawer.css'

interface Props {
  currentStep: 3 | 4
  expectedVersion: number
  open: boolean
  orderUuid: string
  plans: Record<string, ProcessPlanDTO>
  remarkLong?: string
  rolls: RollDraft[]
  selectedRollId?: string
  session: ProcessAiSession
  status?: ProcessAiStatus
  onApply: (confirmation: ProcessAiConfirmResponse) => Promise<void> | void
  onClose: () => void
  onSessionChange: (session: ProcessAiSession) => void
}

export default function ProcessAiConversationDrawer(props: Props) {
  const [minimized, setMinimized] = useState(false)
  const refreshMemory = useRefreshProcessAiMemory()
  const drag = useDraggableAssistantWindow()
  const messagesQuery = useProcessAiMessages({
    orderUuid: props.orderUuid,
    conversationId: props.session.conversationId,
    expectedVersion: props.expectedVersion,
  })
  const messages = messagesQuery.data ?? []
  const stream = useProcessAiStream({
    input: {
      orderUuid: props.orderUuid,
      conversationId: props.session.conversationId,
      expectedVersion: props.expectedVersion,
    },
    onMessagesChanged: messagesQuery.refetch,
  })
  const result = stream.result ?? (!stream.streaming
    ? latestStoredProcessAiResult(messages, props.expectedVersion) : undefined)
  const customerRequirement = props.remarkLong?.trim()
    || messages.find((item) => item.role === 'USER' && item.content.trim())?.content.trim()
    || ''
  const close = () => {
    stream.cancel()
    props.onClose()
  }
  const handleRefreshMemory = async () => {
    const updated = await refreshMemory.mutateAsync({
      orderUuid: props.orderUuid,
      conversationId: props.session.conversationId,
      expectedVersion: props.expectedVersion,
    })
    props.onSessionChange(updated)
  }

  if (!props.open) return null
  return <section role="dialog" aria-modal="false" aria-label="AI 工艺助手"
    className={`process-ai-conversation-window${minimized ? ' process-ai-conversation-window--minimized' : ''}`}
    style={{ left: drag.position.x, top: drag.position.y,
      maxWidth: drag.maxSize.width, maxHeight: drag.maxSize.height }}>
    <header className={`process-ai-conversation-window__header${drag.dragging ? ' is-dragging' : ''}`}
      onPointerDown={drag.start} onPointerMove={drag.move}
      onPointerUp={drag.stop} onPointerCancel={drag.stop}>
      <DragOutlined className="process-ai-conversation-window__drag-icon" />
      <DrawerTitle status={props.status} step={props.currentStep} />
      <div className="process-ai-conversation-window__actions">
        <Tooltip title={minimized ? '展开' : '最小化'}>
          <Button type="text" size="small" icon={<MinusOutlined />}
            aria-label={minimized ? '展开 AI 工艺助手' : '最小化 AI 工艺助手'}
            onClick={() => setMinimized((value) => !value)} />
        </Tooltip>
        <Tooltip title="关闭">
          <Button type="text" size="small" icon={<CloseOutlined />}
            aria-label="关闭 AI 工艺助手" onClick={close} />
        </Tooltip>
      </div>
    </header>
    {!minimized && <>
      <div className="process-ai-conversation__content">
        {props.session.memoryRefreshAvailable && <Alert
          type="info" showIcon message="项目记忆已有新版本"
          description={`当前会话使用 ${props.session.projectMemoryVersion}，最新版本为 ${props.session.latestProjectMemoryVersion}。刷新后会保留历史记录，并从新代际开始新的解析。`}
          action={<Button size="small" loading={refreshMemory.isPending}
            onClick={() => void handleRefreshMemory()}>刷新记忆</Button>}
        />}
        {messagesQuery.isError && <Alert type="error" showIcon message="历史对话恢复失败"
          action={<Button onClick={() => void messagesQuery.refetch()}>重试</Button>} />}
        <ProcessAiMessageList messages={messages} loading={messagesQuery.isLoading}
          pendingUser={stream.pendingUser} progress={stream.progress} error={stream.error} />
        {result && <ProcessAiResultPanel key={`${result.parseId}:${result.parseRevision}`} result={result}
          conversationRequirement={customerRequirement}
          currentDraft={{ plans: props.plans, remarkLong: props.remarkLong, rolls: props.rolls }}
          expectedVersion={props.expectedVersion} orderUuid={props.orderUuid}
          onClarify={(question, answerCode) => void stream.clarify?.(question, answerCode)}
          onRevised={stream.replaceResult}
          onApply={async (confirmation) => {
            await props.onApply(confirmation)
            stream.clearResult()
          }} />}
      </div>
      <ProcessAiComposer disabled={messagesQuery.isLoading || messagesQuery.isError}
        retryable={Boolean(stream.retryAttempt)} streaming={stream.streaming} onCancel={stream.cancel}
        onRetry={stream.retry} onSend={stream.send} />
    </>}
  </section>
}

function DrawerTitle({ status, step }: { status?: ProcessAiStatus; step: 3 | 4 }) {
  const modelLabel = processAiModelLabel(status)
  return <div className="process-ai-conversation__title">
    <span>AI 工艺助手</span>
    <Tag>{step === 3 ? '加工方式' : '工艺配置'}</Tag>
    {modelLabel && <span className="process-ai-conversation__model">{modelLabel}</span>}
  </div>
}
