import { Button, Input, InputNumber, Table } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import MesTooltip from '../biz/MesTooltip'
import type { FinishConfigSpecDTO } from '../../types/processOrder'
import { createStableObjectRowKey } from '../../utils/createStableObjectRowKey'

interface Props {
  specs: FinishConfigSpecDTO[]
  onChange: (specs: FinishConfigSpecDTO[]) => void
}

export default function SawSpecificationTable({ specs, onChange }: Props) {
  return <Table size="small" rowKey={specRowKey} pagination={false}
    columns={columns(specs, onChange)} dataSource={specs} scroll={{ x: 1180 }} />
}

const specRowKey = createStableObjectRowKey('saw-spec')

function columns(specs: FinishConfigSpecDTO[], onChange: Props['onChange']): ColumnsType<FinishConfigSpecDTO> {
  return [
    { title: '类型', width: 72, render: (_, spec) => isTrim(spec) ? '余料' : '成品' },
    {
      title: '物理门幅', width: 145,
      render: (_, spec, index) => <InputNumber className="process-spec-number-input" min={1} suffix="mm" value={spec.finishWidth}
        aria-label={`规格 ${index + 1} 物理门幅`} onChange={(value) => patch(specs, index, { finishWidth: value ?? 1 }, onChange)} />,
    },
    {
      title: '数量', width: 100,
      render: (_, spec, index) => <InputNumber className="process-spec-number-input process-spec-number-input--count" min={1} value={spec.count}
        aria-label={`规格 ${index + 1} 数量`} onChange={(value) => patch(specs, index, { count: value ?? 1 }, onChange)} />,
    },
    {
      title: '客户品名', width: 150,
      render: (_, spec, index) => <Input disabled={isTrim(spec)} value={spec.customerPaperName}
        placeholder="默认同物理品名" aria-label={`规格 ${index + 1} 客户品名`}
        onChange={(event) => patch(specs, index, { customerPaperName: event.target.value || undefined }, onChange)} />,
    },
    {
      title: '客户克重', width: 125,
      render: (_, spec, index) => <InputNumber disabled={isTrim(spec)} min={1} suffix="g"
        value={spec.customerGramWeight} placeholder="同物理" aria-label={`规格 ${index + 1} 客户克重`}
        onChange={(value) => patch(specs, index, { customerGramWeight: value ?? undefined }, onChange)} />,
    },
    {
      title: '客户门幅', width: 125,
      render: (_, spec, index) => <InputNumber disabled={isTrim(spec)} min={1} suffix="mm"
        value={spec.customerFinishWidth} placeholder="同物理" aria-label={`规格 ${index + 1} 客户门幅`}
        onChange={(value) => patch(specs, index, { customerFinishWidth: value ?? undefined }, onChange)} />,
    },
    {
      title: '改写原因', width: 190,
      render: (_, spec, index) => <Input disabled={isTrim(spec)} value={spec.customerSpecOverrideReason}
        placeholder="客户规格不同时必填" aria-label={`规格 ${index + 1} 客户规格改写原因`}
        onChange={(event) => patch(specs, index, { customerSpecOverrideReason: event.target.value || undefined }, onChange)} />,
    },
    {
      title: '操作', width: 64,
      render: (_, __, index) => <MesTooltip title="删除规格">
        <Button danger size="small" icon={<DeleteOutlined />} aria-label="删除锯纸规格"
          disabled={specs.length <= 1} onClick={() => onChange(specs.filter((_, itemIndex) => itemIndex !== index))} />
      </MesTooltip>,
    },
  ]
}

function patch(specs: FinishConfigSpecDTO[], index: number,
  values: Partial<FinishConfigSpecDTO>, onChange: Props['onChange']) {
  onChange(specs.map((spec, itemIndex) => itemIndex === index ? { ...spec, ...values } : spec))
}

function isTrim(spec: FinishConfigSpecDTO) {
  return spec.itemType === 'TRIM'
}
