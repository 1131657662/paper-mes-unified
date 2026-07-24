import { Input, Table, Tag, Typography } from 'antd'
import type { ColumnsType, TableRowSelection } from 'antd/es/table/interface'
import { formatKg } from '../../utils/numberFormatters'
import CustomerSpecNumberInput from '../processOrderCustomerSpec/CustomerSpecNumberInput'
import type { DeliveryCustomerSpecDraft } from './deliveryCustomerDraftModel'
import type { DeliveryCustomerSpec } from './deliveryCustomerSpecTypes'

interface Props {
  rows: DeliveryCustomerSpecDraft[]
  selected: string[]
  previewItems?: DeliveryCustomerSpec[]
  onSelect: (selected: string[]) => void
  onUpdate: (uuid: string, values: Partial<DeliveryCustomerSpecDraft>) => void
}

export default function DeliveryCustomerSpecEditTable({ rows, selected, previewItems, onSelect, onUpdate }: Props) {
  const preview = new Map((previewItems ?? []).map((item) => [item.deliveryDetailUuid, item]))
  const selection: TableRowSelection<DeliveryCustomerSpecDraft> = {
    selectedRowKeys: selected,
    onChange: (keys) => onSelect(keys.map(String)),
  }
  return <Table<DeliveryCustomerSpecDraft> bordered className="delivery-customer-edit-table" columns={columns(onUpdate, preview)} dataSource={rows} pagination={false} rowKey="deliveryDetailUuid" rowSelection={selection} scroll={{ x: 1330, y: 390 }} size="small" />
}

function columns(update: Props['onUpdate'], preview: Map<string, DeliveryCustomerSpec>): ColumnsType<DeliveryCustomerSpecDraft> {
  return [
    { title: '成品卷号', dataIndex: 'finishRollNo', width: 150, fixed: 'left', render: (value) => <Typography.Text strong>{value ?? '-'}</Typography.Text> },
    { title: '签收实物', width: 210, render: (_, row) => <PhysicalCell row={row} /> },
    { title: '客户品名', width: 160, render: (_, row) => <Input aria-label={`客户品名 ${row.finishRollNo ?? row.deliveryDetailUuid}`} value={row.customerPaperName} onChange={(event) => update(row.deliveryDetailUuid, { customerPaperName: event.target.value })} /> },
    { title: '客户克重', width: 125, render: (_, row) => <CustomerSpecNumberInput min={1} max={5000} unit="g" value={row.customerGramWeight} onChange={(value) => update(row.deliveryDetailUuid, { customerGramWeight: value })} /> },
    { title: '客户门幅', width: 135, render: (_, row) => <CustomerSpecNumberInput min={1} max={100000} unit="mm" value={row.customerFinishWidth} onChange={(value) => update(row.deliveryDetailUuid, { customerFinishWidth: value })} /> },
    { title: '客户重量', width: 155, render: (_, row) => <CustomerSpecNumberInput min={1} max={1e12} precision={0} unit="kg" value={preview.get(row.deliveryDetailUuid)?.customerDisplayWeight ?? row.customerDisplayWeight} onChange={(value) => update(row.deliveryDetailUuid, { customerDisplayWeight: value, calculationMode: 'MANUAL' })} /> },
    { title: '客户备注', width: 180, render: (_, row) => <Input aria-label={`客户备注 ${row.finishRollNo ?? row.deliveryDetailUuid}`} maxLength={255} value={row.customerRemark} onChange={(event) => update(row.deliveryDetailUuid, { customerRemark: event.target.value })} /> },
    { title: '重量方式', width: 100, render: (_, row) => <Tag color={row.calculationMode === 'KEEP' ? 'default' : 'blue'}>{modeText[row.calculationMode]}</Tag> },
    { title: '校验', width: 150, render: (_, row) => <Validation item={preview.get(row.deliveryDetailUuid)} /> },
  ]
}

function PhysicalCell({ row }: { row: DeliveryCustomerSpecDraft }) {
  return <div className="delivery-customer-cell"><Typography.Text>{row.physicalPaperName ?? '-'}</Typography.Text><span>{row.physicalGramWeight ?? '-'}g / {row.physicalFinishWidth ?? '-'}mm / {formatKg(row.physicalDeliveryWeight)}</span></div>
}

function Validation({ item }: { item?: DeliveryCustomerSpec }) {
  if (!item) return <Typography.Text type="secondary">待预览</Typography.Text>
  return item.valid ? <Tag color="success">通过</Tag> : <Typography.Text type="danger">{item.error}</Typography.Text>
}

const modeText = { KEEP: '保持', FIXED: '固定', DELTA: '加减', RATIO: '比例', FORMULA: '公式', MANUAL: '手工' }
