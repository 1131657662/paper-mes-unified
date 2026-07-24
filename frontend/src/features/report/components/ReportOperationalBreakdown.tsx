import type { ColumnsType } from 'antd/es/table'
import DocumentDetailTable from '../../../components/biz/DocumentDetailTable'
import type { ReportCollectionDimensionVO, ReportDeliveryDimensionVO,
  ReportInventoryDimensionVO, ReportOperationalAnalysisVO,
  ReportSettlementDimensionVO } from '../../../types/reportOperational'
import { formatMoney, formatNumber, formatTonFromKg } from '../utils/reportFormatters'

type Dimension = ReportSettlementDimensionVO | ReportCollectionDimensionVO
  | ReportInventoryDimensionVO | ReportDeliveryDimensionVO

export default function ReportOperationalBreakdown({ analysis }: { analysis: ReportOperationalAnalysisVO }) {
  const rows = breakdownRows(analysis)
  const warehouse = analysis.topicCode === 'inventory' || analysis.topicCode === 'delivery'
  return <section className="report-topic-panel report-topic-breakdown">
    <header><div><h3>{warehouse ? '仓库结构' : '客户结构'}</h3>
      <p>{breakdownHint[analysis.topicCode]}</p></div></header>
    <DocumentDetailTable<Dimension> dataSource={rows} columns={columns(analysis.topicCode)}
      pagination={false} rowKey="dimensionKey" scroll={{ x: 760, y: 360 }}
      storageKey={'report-' + analysis.topicCode + '-breakdown'} />
  </section>
}

function breakdownRows(analysis: ReportOperationalAnalysisVO): Dimension[] {
  if (analysis.topicCode === 'settlement' || analysis.topicCode === 'collection') {
    return analysis.customerBreakdown
  }
  return analysis.warehouseBreakdown
}

function columns(topic: ReportOperationalAnalysisVO['topicCode']): ColumnsType<Dimension> {
  if (topic === 'settlement') return settlementColumns
  if (topic === 'collection') return collectionColumns
  if (topic === 'inventory') return inventoryColumns
  return deliveryColumns
}

const settlementColumns: ColumnsType<Dimension> = [
  { title: '客户', dataIndex: 'dimensionName', width: 180, fixed: 'left', ellipsis: true },
  { title: '结算单', dataIndex: 'documentCount', width: 90, align: 'right', render: renderNumber },
  { title: '应收金额', dataIndex: 'totalAmount', width: 130, align: 'right', render: formatMoney },
  { title: '已收金额', dataIndex: 'receivedAmount', width: 130, align: 'right', render: formatMoney },
  { title: '未收金额', dataIndex: 'unreceivedAmount', width: 130, align: 'right', render: formatMoney },
]
const collectionColumns: ColumnsType<Dimension> = [
  { title: '客户', dataIndex: 'dimensionName', width: 180, fixed: 'left', ellipsis: true },
  { title: '回款流水', dataIndex: 'recordCount', width: 100, align: 'right', render: renderNumber },
  { title: '结清金额', dataIndex: 'settledAmount', width: 130, align: 'right', render: formatMoney },
  { title: '现金到账', dataIndex: 'cashAmount', width: 130, align: 'right', render: formatMoney },
  { title: '废纸重量', dataIndex: 'scrapWeight', width: 120, align: 'right', render: formatTonFromKg },
]
const inventoryColumns: ColumnsType<Dimension> = [
  { title: '仓库', dataIndex: 'dimensionName', width: 180, fixed: 'left', ellipsis: true },
  { title: '库存卷数', dataIndex: 'rollCount', width: 100, align: 'right', render: renderNumber },
  { title: '库存重量', dataIndex: 'totalWeight', width: 130, align: 'right', render: formatTonFromKg },
  { title: '锁定重量', dataIndex: 'lockedWeight', width: 130, align: 'right', render: formatTonFromKg },
]
const deliveryColumns: ColumnsType<Dimension> = [
  { title: '仓库', dataIndex: 'dimensionName', width: 180, fixed: 'left', ellipsis: true },
  { title: '出库单', dataIndex: 'documentCount', width: 100, align: 'right', render: renderNumber },
  { title: '卷数', dataIndex: 'rollCount', width: 90, align: 'right', render: renderNumber },
  { title: '计划重量', dataIndex: 'totalWeight', width: 130, align: 'right', render: formatTonFromKg },
  { title: '已签收重量', dataIndex: 'completedWeight', width: 130, align: 'right', render: formatTonFromKg },
]

const breakdownHint = {
  settlement: '按客户汇总结算单与未收余额，优先显示风险高的客户。',
  collection: '按客户汇总有效到账流水，不把优惠误计为现金。',
  inventory: '按库存档案中的仓库归属汇总，未分配仓库单独列出。',
  delivery: '按出库单仓库汇总计划重量与已签收重量。',
} satisfies Record<ReportOperationalAnalysisVO['topicCode'], string>

function renderNumber(value: number) { return formatNumber(value) }
