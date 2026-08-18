import { lazy, Suspense, useState } from 'react'
import { Button, Tooltip } from 'antd'
import { RobotOutlined } from '@ant-design/icons'
import { PERMISSIONS } from '../../../constants/permissions'
import { useHasPermission } from '../../../stores/authStore'
import type { RollDraft } from '../../processOrderCreate/types'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import { useOpenProcessAiSession } from '../hooks/useOpenProcessAiSession'
import { useProcessAiStatus } from '../hooks/useProcessAiStatus'
import type { ProcessAiConfirmResponse, ProcessAiSession } from '../types'

const ProcessAiConversationDrawer = lazy(() => import('./ProcessAiConversationDrawer'))

interface Props {
  currentStep: 3 | 4
  draftVersion: number
  orderUuid?: string
  plans: Record<string, ProcessPlanDTO>
  remarkLong?: string
  rolls: RollDraft[]
  selectedRollId?: string
  onApply: (confirmation: ProcessAiConfirmResponse) => void
}

export default function ProcessAiAssistantEntry(props: Props) {
  const [open, setOpen] = useState(false)
  const [session, setSession] = useState<ProcessAiSession>()
  const canUseAi = useHasPermission(PERMISSIONS.aiAssist)
  const { data: status, isError: isStatusError, isLoading: isLoadingStatus } = useProcessAiStatus(canUseAi)
  const { mutateAsync: openSession, isPending: isOpening } = useOpenProcessAiSession()
  if (!canUseAi) return null
  const unavailable = isStatusError ? '无法确认 AI 工艺解析状态' : unavailableText(props.orderUuid, status)

  const handleOpen = async () => {
    if (!props.orderUuid || unavailable) return
    const opened = await openSession({
      orderUuid: props.orderUuid,
      expectedVersion: props.draftVersion,
      currentStep: props.currentStep,
    })
    setSession(opened)
    setOpen(true)
  }

  return <>
    <Tooltip title={unavailable ?? '打开 AI 工艺助手'}>
      <Button icon={<RobotOutlined />} loading={isOpening || isLoadingStatus}
        disabled={Boolean(unavailable)} onClick={() => void handleOpen()}>
        AI 工艺助手
      </Button>
    </Tooltip>
    {open && session && props.orderUuid && <Suspense fallback={null}>
      <ProcessAiConversationDrawer
        key={session.conversationId}
        currentStep={props.currentStep}
        expectedVersion={props.draftVersion}
        open={open}
        orderUuid={props.orderUuid}
        plans={props.plans}
        remarkLong={props.remarkLong}
        rolls={props.rolls}
        selectedRollId={props.selectedRollId}
        session={session}
        status={status}
        onApply={props.onApply}
        onSessionChange={setSession}
        onClose={() => setOpen(false)}
      />
    </Suspense>}
  </>
}

function unavailableText(orderUuid: string | undefined, status: ReturnType<typeof useProcessAiStatus>['data']) {
  if (!orderUuid) return '请先完成母卷录入'
  if (!status) return undefined
  if (!status.enabled) return 'AI 工艺解析尚未启用'
  if (!status.ready && status.unavailableReason !== 'AI_MEMORY_UNAVAILABLE') {
    return status.unavailableReason || 'AI 工艺解析尚未就绪'
  }
  return undefined
}
