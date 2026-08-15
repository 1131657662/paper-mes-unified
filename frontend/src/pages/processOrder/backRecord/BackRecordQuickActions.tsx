import { Button, Space, message } from 'antd'
import { CopyOutlined, SwapOutlined } from '@ant-design/icons'
import type { FormInstance } from 'antd/es/form'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { applyBackRecordFilledValues } from './applyBackRecordFilledValues'
import type { BackRecordFormValues } from './backRecordUtils'
import { theoreticalBackRecordValues } from './backRecordTheoryFill'

interface Props {
  detail: ProcessOrderDetailVO | null
  form: FormInstance<BackRecordFormValues>
  onDirty?: () => void
  onValuesFilled?: (values: BackRecordFormValues) => void
  onOpenChange: () => void
}

export default function BackRecordQuickActions({ detail, form, onDirty, onOpenChange, onValuesFilled }: Props) {
  const handleTheoryFill = () => {
    if (!detail) return
    const values = theoreticalBackRecordValues(detail)
    applyBackRecordFilledValues({ form, onDirty, onValuesFilled, values })
    form.validateFields().catch(() => undefined)
    message.success('已带入现有实测参数、母卷标称重量和成品预估；标称重量仍需现场复称')
  }

  return (
    <Space wrap size={[8, 8]} className="back-record-quick-actions">
      <MesTooltip title="带入母卷已有实测参数和成品预估值；标称/参考母卷重量不会被伪装成实测，备用号未用会保持空白。">
        <Button icon={<CopyOutlined />} disabled={!detail} onClick={handleTheoryFill}>
          带入现有参数与预估
        </Button>
      </MesTooltip>
      <Button icon={<SwapOutlined />} disabled={!detail} onClick={onOpenChange}>
        现场变更处理
      </Button>
    </Space>
  )
}
