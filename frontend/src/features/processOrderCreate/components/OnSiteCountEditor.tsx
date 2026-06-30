import { Alert, InputNumber, Space, Typography } from 'antd'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import { onSiteCount, toOnSitePlan } from '../onSitePlanUtils'

interface Props {
  plan: ProcessPlanDTO
  onChange: (plan: ProcessPlanDTO) => void
}

export default function OnSiteCountEditor({ plan, onChange }: Props) {
  const count = onSiteCount(plan.finishSpecs)
  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <Alert
        type="info"
        showIcon
        message="现场定尺只预占成品卷号"
        description="开单时填写预计成品件数；门幅、直径、重量由车间现场确认，并在回录时补齐。"
      />
      <div>
        <Typography.Text strong>预计成品件数</Typography.Text>
        <InputNumber
          min={1}
          precision={0}
          value={count}
          addonAfter="件"
          style={{ width: 180, marginLeft: 12 }}
          onChange={(value) => onChange(toOnSitePlan(plan, value ?? 1))}
        />
      </div>
    </Space>
  )
}
