import { Button, Space, message } from 'antd'
import { CopyOutlined, SwapOutlined } from '@ant-design/icons'
import type { FormInstance } from 'antd/es/form'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import { theoreticalFinishValues, theoreticalRollValues } from './backRecordTheoryFill'

interface Props {
  detail: ProcessOrderDetailVO | null
  form: FormInstance<BackRecordFormValues>
  onValuesFilled?: (values: BackRecordFormValues) => void
  onOpenChange: () => void
}

export default function BackRecordQuickActions({ detail, form, onOpenChange, onValuesFilled }: Props) {
  const handleTheoryFill = () => {
    if (!detail) return
    const values = {
      rolls: theoreticalRollValues(detail),
      finishes: theoreticalFinishValues(detail),
    }
    form.setFieldsValue(values)
    onValuesFilled?.(values)
    form.validateFields().catch(() => undefined)
    message.success('已按标称/预估回填整单，可继续逐卷微调')
  }

  return (
    <Space wrap size={[8, 8]} className="back-record-quick-actions">
      <MesTooltip title="母卷复称带入标称值，正式成品优先带入预估值；没有预估时按母卷理论重量兜底分配，备用号未用会保持空白。">
        <Button icon={<CopyOutlined />} disabled={!detail} onClick={handleTheoryFill}>
          整单按理论回录
        </Button>
      </MesTooltip>
      <Button icon={<SwapOutlined />} disabled={!detail} onClick={onOpenChange}>
        现场变更处理
      </Button>
    </Space>
  )
}
