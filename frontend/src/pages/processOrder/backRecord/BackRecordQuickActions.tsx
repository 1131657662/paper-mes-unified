import { Button, Space, Tooltip, message } from 'antd'
import { CopyOutlined, SwapOutlined } from '@ant-design/icons'
import type { FormInstance } from 'antd/es/form'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import {
  fillFinishActuals,
  fillRollActuals,
  type BackRecordFormValues,
} from './backRecordUtils'

interface Props {
  detail: ProcessOrderDetailVO | null
  form: FormInstance<BackRecordFormValues>
  onOpenChange: () => void
}

export default function BackRecordQuickActions({ detail, form, onOpenChange }: Props) {
  const handleTheoryFill = () => {
    if (!detail) return
    form.setFieldsValue({
      rolls: fillRollActuals(detail),
      finishes: fillFinishActuals(detail),
    })
    message.success('已按标称/预估回填整单，可继续逐卷微调')
  }

  return (
    <Space wrap size={[8, 8]} className="back-record-quick-actions">
      <Tooltip title="母卷复称带入标称值，正式成品实重带入预估值；备用号未用会保持空白。">
        <Button icon={<CopyOutlined />} disabled={!detail} onClick={handleTheoryFill}>
          整单按理论回录
        </Button>
      </Tooltip>
      <Button icon={<SwapOutlined />} disabled={!detail} onClick={onOpenChange}>
        现场变更处理
      </Button>
    </Space>
  )
}
