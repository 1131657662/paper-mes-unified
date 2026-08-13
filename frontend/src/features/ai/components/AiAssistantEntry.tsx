import { lazy, Suspense, useState } from 'react'
import { Button, Tooltip } from 'antd'
import { RobotOutlined } from '@ant-design/icons'
import { useLocation } from 'react-router'
import { PERMISSIONS } from '../../../constants/permissions'
import { useHasPermission } from '../../../stores/authStore'
import { buildAiContextEpoch, buildAiPageTemplate } from '../aiContext'
import { useAiStatus } from '../hooks/useAiStatus'

const AiAssistantDrawer = lazy(() => import('./AiAssistantDrawer'))

export default function AiAssistantEntry() {
  const location = useLocation()
  const [open, setOpen] = useState(false)
  const canUseAi = useHasPermission(PERMISSIONS.aiAssist)
  const { data: status } = useAiStatus(canUseAi)
  const isAvailable = canUseAi && status?.enabled === true
    && status.rulesReady && status.dataMode === 'FAQ_ONLY'

  if (!isAvailable) return null
  const contextEpoch = buildAiContextEpoch(location.key)

  return (
    <>
      <Tooltip title="智能助手">
        <Button
          type="text"
          className="ai-assistant__trigger"
          icon={<RobotOutlined />}
          aria-label="智能助手"
          onClick={() => setOpen(true)}
        />
      </Tooltip>
      {open && (
        <Suspense fallback={null}>
          <AiAssistantDrawer
            key={contextEpoch}
            open={open}
            onClose={() => setOpen(false)}
            pageTemplate={buildAiPageTemplate(location.pathname)}
            contextEpoch={contextEpoch}
          />
        </Suspense>
      )}
    </>
  )
}
