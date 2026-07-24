import { Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { DeliveryDetail } from '../../types/delivery'
import { formatKg, formatWholeKg } from '../../utils/numberFormatters'
import type { DeliveryCustomerSpec, DeliveryDocumentView } from './deliveryCustomerSpecTypes'

interface Row { detail?: DeliveryDetail; spec: DeliveryCustomerSpec }
interface Props { details: DeliveryDetail[]; items?: DeliveryCustomerSpec[]; view: Exclude<DeliveryDocumentView, 'physical'> }

export default function DeliveryCustomerViewTable({ details, items = [], view }: Props) {
  const detailIndex = new Map(details.map((detail) => [detail.uuid, detail]))
  const rows = items.map((spec) => ({ spec, detail: detailIndex.get(spec.deliveryDetailUuid) }))
  const total = rows.reduce((sum, row) => sum + (row.spec.customerDisplayWeight ?? 0), 0)
  return <Table<Row> bordered className="delivery-customer-table" columns={columns(view)} dataSource={rows} pagination={false} rowKey={({ spec }) => spec.deliveryDetailUuid} scroll={{ x: view === 'trace' ? 1080 : 820 }} size="small" summary={() => summary(rows.length, total, view)} />
}

function columns(view: Props['view']): ColumnsType<Row> {
  const result: ColumnsType<Row> = [
    { title: '成品卷号', width: 150, fixed: 'left', render: (_, { spec }) => <Typography.Text strong>{spec.finishRollNo ?? '-'}</Typography.Text> },
    { title: '客户品名', width: 170, render: (_, { spec }) => <CustomerName spec={spec} trace={view === 'trace'} /> },
    { title: '客户规格', width: 180, render: (_, { spec }) => <CustomerSpecification spec={spec} trace={view === 'trace'} /> },
    { title: '客户单据重量', width: 155, align: 'right', render: (_, { spec }) => <Weight spec={spec} trace={view === 'trace'} /> },
    { title: '加工单', width: 150, render: (_, { spec }) => spec.orderNo ?? '-' },
    { title: '客户备注', width: 180, render: (_, { spec }) => spec.customerRemark || '-' },
  ]
  if (view === 'trace') result.splice(5, 0, { title: '来源母卷', width: 260, render: (_, { detail }) => detail?.originalSummary || detail?.originalRollNos || '-' })
  return result
}

function CustomerName({ spec, trace }: { spec: DeliveryCustomerSpec; trace: boolean }) {
  return <div className="delivery-customer-cell"><Typography.Text strong>{spec.customerPaperName ?? '-'}</Typography.Text>{trace && <span>实物：{spec.physicalPaperName ?? '-'}</span>}{spec.specificationChanged && <Tag color="gold">已调整</Tag>}</div>
}

function CustomerSpecification({ spec, trace }: { spec: DeliveryCustomerSpec; trace: boolean }) {
  return <div className="delivery-customer-cell"><Typography.Text>{spec.customerGramWeight ?? '-'}g / {spec.customerFinishWidth ?? '-'}mm</Typography.Text>{trace && <span>实物：{spec.physicalGramWeight ?? '-'}g / {spec.physicalFinishWidth ?? '-'}mm</span>}</div>
}

function Weight({ spec, trace }: { spec: DeliveryCustomerSpec; trace: boolean }) {
  return <div className="delivery-customer-cell is-right"><Typography.Text strong>{formatWholeKg(spec.customerDisplayWeight)}</Typography.Text>{trace && <span>实物：{formatKg(spec.physicalDeliveryWeight)}</span>}</div>
}

function summary(count: number, total: number, view: Props['view']) {
  const columns = view === 'trace' ? 7 : 6
  return <Table.Summary.Row className="delivery-customer-summary"><Table.Summary.Cell index={0}>客户单据合计</Table.Summary.Cell><Table.Summary.Cell index={1} colSpan={2}>{count} 件</Table.Summary.Cell><Table.Summary.Cell index={3} align="right">{formatWholeKg(total)}</Table.Summary.Cell><Table.Summary.Cell index={4} colSpan={columns - 4} /></Table.Summary.Row>
}
