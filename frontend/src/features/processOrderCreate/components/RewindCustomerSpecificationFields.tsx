import { Input, InputNumber, Space } from 'antd'
import type { RewindLayoutItemPlanDTO } from '../../../types/processOrder'

interface Props {
  index: number
  item: RewindLayoutItemPlanDTO
  onChange: (patch: Partial<RewindLayoutItemPlanDTO>) => void
}

export default function RewindCustomerSpecificationFields({ index, item, onChange }: Props) {
  return (
    <Space wrap>
      <Input value={item.customerPaperName} placeholder="客户品名"
        aria-label={`排布 ${index + 1} 客户品名`}
        onChange={(event) => onChange({ customerPaperName: event.target.value || undefined })} />
      <InputNumber min={1} suffix="g" value={item.customerGramWeight} placeholder="客户克重"
        aria-label={`排布 ${index + 1} 客户克重`}
        onChange={(value) => onChange({ customerGramWeight: value ?? undefined })} />
      <InputNumber min={1} suffix="mm" value={item.customerFinishWidth} placeholder="客户门幅"
        aria-label={`排布 ${index + 1} 客户门幅`}
        onChange={(value) => onChange({ customerFinishWidth: value ?? undefined })} />
      <Input value={item.customerSpecOverrideReason} placeholder="规格不同时填写原因"
        aria-label={`排布 ${index + 1} 客户规格改写原因`}
        onChange={(event) => onChange({ customerSpecOverrideReason: event.target.value || undefined })} />
    </Space>
  )
}
