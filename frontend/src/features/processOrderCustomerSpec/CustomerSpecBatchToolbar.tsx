import { CopyOutlined, DownOutlined, ImportOutlined } from '@ant-design/icons'
import { Button, Input, InputNumber, Space } from 'antd'
import type { BulkSpecificationValues } from './customerSpecDraftModel'

interface Props {
  disabled: boolean
  values: BulkSpecificationValues
  onChange: (values: BulkSpecificationValues) => void
  onApply: () => void
  onFillDown: () => void
  onSameSpec: () => void
  onPaste: () => void
}

export default function CustomerSpecBatchToolbar(props: Props) {
  const change = (values: Partial<BulkSpecificationValues>) => props.onChange({ ...props.values, ...values })
  return (
    <div className="customer-spec-batch-toolbar">
      <div className="customer-spec-batch-fields">
        <Input allowClear aria-label="批量客户品名" placeholder="客户品名" value={props.values.paperName} onChange={(event) => change({ paperName: event.target.value })} />
        <InputNumber aria-label="批量客户克重" min={1} max={5000} placeholder="克重 g" value={props.values.gramWeight} onChange={(value) => change({ gramWeight: value ?? undefined })} />
        <InputNumber aria-label="批量客户门幅" min={1} max={100000} placeholder="门幅 mm" value={props.values.finishWidth} onChange={(value) => change({ finishWidth: value ?? undefined })} />
        <Button type="primary" disabled={props.disabled} onClick={props.onApply}>应用到已选</Button>
      </div>
      <Space size={6} wrap>
        <Button icon={<DownOutlined />} disabled={props.disabled} onClick={props.onFillDown}>向下填充</Button>
        <Button icon={<CopyOutlined />} disabled={props.disabled} onClick={props.onSameSpec}>同实物规格</Button>
        <Button icon={<ImportOutlined />} onClick={props.onPaste}>粘贴表格</Button>
      </Space>
    </div>
  )
}
