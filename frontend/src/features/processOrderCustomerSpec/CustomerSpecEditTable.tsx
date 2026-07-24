import { Input, Table, Tag, Typography } from 'antd'
import type { ColumnsType, TableRowSelection } from 'antd/es/table/interface'
import { formatKg } from '../../utils/numberFormatters'
import type { CustomerSpecDraft } from './customerSpecDraftModel'
import type { FinishCustomerSpec } from './customerSpecTypes'
import CustomerSpecNumberInput from './CustomerSpecNumberInput'

interface Props {
  rows: CustomerSpecDraft[]
  selected: string[]
  previewItems?: FinishCustomerSpec[]
  onSelect: (selected: string[]) => void
  onUpdate: (uuid: string, values: Partial<CustomerSpecDraft>) => void
}

export default function CustomerSpecEditTable({ rows, selected, previewItems, onSelect, onUpdate }: Props) {
  const preview = new Map((previewItems ?? []).map((item) => [item.finishUuid, item]))
  const columns = buildColumns(onUpdate, preview)
  const selection: TableRowSelection<CustomerSpecDraft> = {
    selectedRowKeys: selected,
    onChange: (keys) => onSelect(keys.map(String)),
  }
  return <Table<CustomerSpecDraft> bordered className="customer-spec-edit-table" columns={columns} dataSource={rows} pagination={false} rowKey="finishUuid" rowSelection={selection} scroll={{ x: 1180, y: 420 }} size="small" />
}

function buildColumns(
  update: Props['onUpdate'], preview: Map<string, FinishCustomerSpec>,
): ColumnsType<CustomerSpecDraft> {
  return [
    { title: '成品卷号', dataIndex: 'finishRollNo', width: 160, fixed: 'left', render: (value) => <Typography.Text strong>{value ?? '-'}</Typography.Text> },
    { title: '现场实物', width: 210, render: (_, row) => <PhysicalCell row={row} /> },
    { title: '客户品名', width: 170, render: (_, row) => <Input aria-label={`客户品名 ${row.finishRollNo ?? row.finishUuid}`} value={row.customerPaperName} onChange={(event) => update(row.finishUuid, { customerPaperName: event.target.value })} /> },
    { title: '客户克重', align: 'right', width: 125, render: (_, row) => <CustomerSpecNumberInput min={1} max={5000} unit="g" value={row.customerGramWeight} onChange={(value) => update(row.finishUuid, { customerGramWeight: value })} /> },
    { title: '客户门幅', align: 'right', width: 135, render: (_, row) => <CustomerSpecNumberInput min={1} max={100000} unit="mm" value={row.customerFinishWidth} onChange={(value) => update(row.finishUuid, { customerFinishWidth: value })} /> },
    { title: '客户重量', align: 'right', width: 155, render: (_, row) => <CustomerSpecNumberInput min={1} max={1e12} precision={0} unit="kg" value={preview.get(row.finishUuid)?.customerDisplayWeight ?? row.customerDisplayWeight} onChange={(value) => update(row.finishUuid, { customerDisplayWeight: value, calculationMode: 'MANUAL' })} /> },
    { title: '重量方式', width: 105, render: (_, row) => <Tag color={row.calculationMode === 'KEEP' ? 'default' : 'blue'}>{modeText[row.calculationMode]}</Tag> },
    { title: '校验', width: 170, render: (_, row) => <ValidationCell item={preview.get(row.finishUuid)} /> },
  ]
}

function PhysicalCell({ row }: { row: CustomerSpecDraft }) {
  return <div className="customer-spec-physical-cell"><Typography.Text>{row.physicalPaperName ?? '-'}</Typography.Text><span>{row.physicalGramWeight ?? '-'}g / {row.physicalFinishWidth ?? '-'}mm / {formatKg(row.physicalWeight)}</span></div>
}

function ValidationCell({ item }: { item?: FinishCustomerSpec }) {
  if (!item) return <Typography.Text type="secondary">待预览</Typography.Text>
  return item.valid ? <Tag color="success">通过</Tag> : <Typography.Text type="danger">{item.error}</Typography.Text>
}

const modeText = { KEEP: '保持', FIXED: '固定', DELTA: '加减', RATIO: '比例', FORMULA: '公式', MANUAL: '手工' }
