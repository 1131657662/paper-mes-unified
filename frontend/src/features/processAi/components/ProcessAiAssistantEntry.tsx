import { lazy, Suspense, useState } from 'react'
import { Button, Tooltip } from 'antd'
import { RobotOutlined } from '@ant-design/icons'
import { PERMISSIONS } from '../../../constants/permissions'
import { useHasPermission } from '../../../stores/authStore'
import type { RollDraft } from '../../processOrderCreate/types'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import { useOpenProcessAiSession } from '../hooks/useOpenProcessAiSession'
import { useProcessAiStatus } from '../hooks/useProcessAiStatus'
import { processAiAvailability } from '../processAiAvailability'
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
  onApply: (confirmation: ProcessAiConfirmResponse) => Promise<void> | void
}

export default function ProcessAiAssistantEntry(props: Props) {
  const [open, setOpen] = useState(false)
  const [session, setSession] = useState<ProcessAiSession>()
  const canUseAi = useHasPermission(PERMISSIONS.aiAssist)
  const {
    data: status,
    isError: isStatusError,
    isLoading: isLoadingStatus,
    refetch: refetchStatus,
  } = useProcessAiStatus(canUseAi)
  const { mutateAsync: openSession, isPending: isOpening } = useOpenProcessAiSession()
  if (!canUseAi) return null
  const availability = processAiAvailability(props.orderUuid, status, isStatusError)

  const handleOpen = async () => {
    if (!props.orderUuid || availability.unavailable) return
    if (isStatusError) {
      const refreshed = await refetchStatus()
      const refreshedAvailability = processAiAvailability(props.orderUuid, refreshed.data)
      if (refreshed.isError || refreshedAvailability.unavailable) return
    }
    const opened = await openSession({
      orderUuid: props.orderUuid,
      expectedVersion: props.draftVersion,
      currentStep: props.currentStep,
    })
    setSession(opened)
    setOpen(true)
  }

  return <>
    <Tooltip title={availability.hint}>
      <Button icon={<RobotOutlined />} loading={isOpening || isLoadingStatus}
        disabled={Boolean(availability.unavailable)} onClick={() => void handleOpen()}>
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
