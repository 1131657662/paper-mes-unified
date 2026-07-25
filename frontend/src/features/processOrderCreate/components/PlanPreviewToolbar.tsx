import { ReloadOutlined } from '@ant-design/icons'
import { Button, Space, Tag } from 'antd'
import type { PlanPreviewVO } from '../../../types/processOrder'
import { planPreviewSaveStatus } from '../planPreviewPresentation'

interface Props {
  configured: boolean
  loading?: boolean
  onPreview?: () => void
  preview?: PlanPreviewVO
}

export default function PlanPreviewToolbar({ configured, loading, onPreview, preview }: Props) {
  const status = planPreviewSaveStatus(preview, configured)
  return (
    <div className="plan-preview-panel__toolbar">
      <Space size={8} wrap>
        {status && <Tag color={status.color}>{status.label}</Tag>}
        {loading && <Tag color="processing">预览中</Tag>}
      </Space>
      {onPreview && (
        <Button size="small" icon={<ReloadOutlined />} loading={loading} onClick={onPreview}>
          刷新预览
        </Button>
      )}
    </div>
  )
}
