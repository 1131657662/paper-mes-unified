import { Button, Segmented, Space, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import DocumentDetailTable from '../../../components/biz/DocumentDetailTable'
import MesTooltip from '../../../components/biz/MesTooltip'
import { mesTablePagination } from '../../../components/biz/MesPaginationBar'
import TooltipText from '../../../components/biz/TooltipText'
import type { ReportDetailVO, ReportDimension, ReportDimensionVO } from '../../../types/report'
import ReportDimensionSummaryRow from './ReportDimensionSummaryRow'
import {
  formatKg,
  formatMoney,
  formatNumber,
  formatPercent,
} from '../utils/reportFormatters'

interface Props {
  details: ReportDetailVO[]
  dimension: ReportDimension
  dimensions: ReportDimensionVO[]
  loading: boolean
  onDimensionChange: (dimension: ReportDimension) => void
  onRefresh: () => void
}

export default function ReportTables({
  details,
  dimension,
  dimensions,
  loading,
  onDimensionChange,
  onRefresh,
}: Props) {
  const navigate = useNavigate()

  return (
    <section className="report-panel report-panel--tables">
      <div className="report-panel__head report-panel__head--inline">
        <div>
          <h3>报表明细</h3>
          <p>维度汇总与加工单明细均支持列宽拖拽、列设置和刷新。</p>
        </div>
        <Segmented
          value={dimension}
          options={dimensionOptions}
          onChange={(value) => onDimensionChange(value as ReportDimension)}
        />
      </div>
      <div className="report-table-stack">
        <div className="report-table-block">
          <div className="report-table-block__head">
            <strong>维度汇总</strong>
            <span>按当前维度聚合费用、重量、损耗和未收款。</span>
          </div>
          <div className="document-module-table">
            <DocumentDetailTable<ReportDimensionVO>
              columns={dimensionColumns(dimension)}
              dataSource={dimensions}
              loading={loading}
              onReload={onRefresh}
              pagination={false}
              rowKey={(record) => `${dimension}-${record.dimensionKey}`}
              scroll={{ x: 1448, y: 260 }}
              storageKey={`report-dimension-${dimension}`}
              summary={() => <ReportDimensionSummaryRow rows={dimensions} />}
            />
          </div>
        </div>
        <div className="report-table-block">
          <div className="report-table-block__head">
            <strong>加工单明细</strong>
            <span>点击加工单号可进入详情核对来源、成品和结算链路。</span>
          </div>
          <div className="document-module-table">
            <DocumentDetailTable<ReportDetailVO>
              columns={detailColumns((uuid) => navigate(`/process-orders/${uuid}`))}
              dataSource={details}
              loading={loading}
              onReload={onRefresh}
              pagination={mesTablePagination(20)}
              rowKey="orderUuid"
              scroll={{ x: 1884, y: 560 }}
              storageKey="report-order-details"
            />
          </div>
        </div>
      </div>
    </section>
  )
}

const dimensionOptions = [
  { value: 'customer', label: '客户' },
  { value: 'paper', label: '产品' },
  { value: 'process', label: '工艺' },
  { value: 'machine', label: '机台' },
  { value: 'month', label: '月度' },
  { value: 'invoice', label: '开票' },
  { value: 'settleType', label: '结算' },
  { value: 'status', label: '状态' },
]

function dimensionColumns(dimension: ReportDimension): ColumnsType<ReportDimensionVO> {
  return [
    { title: dimensionTitle(dimension), dataIndex: 'dimensionName', key: 'dimensionName', width: 180, render: dimensionCell },
    { title: '加工单', dataIndex: 'orderCount', key: 'orderCount', width: 96, align: 'right', render: countCell('单') },
    { title: '原卷', dataIndex: 'originalRollCount', key: 'originalRollCount', width: 88, align: 'right', render: countCell('卷') },
    { title: '成品', dataIndex: 'finishRollCount', key: 'finishRollCount', width: 88, align: 'right', render: countCell('卷') },
    { title: '原纸重量', dataIndex: 'originalWeight', key: 'originalWeight', width: 128, align: 'right', render: formatKg },
    { title: '成品重量', dataIndex: 'finishWeight', key: 'finishWeight', width: 128, align: 'right', render: formatKg },
    { title: '损耗', dataIndex: 'lossWeight', key: 'lossWeight', width: 120, align: 'right', render: formatKg },
    { title: '损耗率', dataIndex: 'lossRatio', key: 'lossRatio', width: 98, align: 'right', render: formatPercent },
    { title: '刀数', dataIndex: 'knifeCount', key: 'knifeCount', width: 88, align: 'right', render: numberCell },
    { title: '锯纸费', dataIndex: 'sawAmount', key: 'sawAmount', width: 118, align: 'right', render: formatMoney },
    { title: '复卷费', dataIndex: 'rewindAmount', key: 'rewindAmount', width: 118, align: 'right', render: formatMoney },
    { title: '加工费', dataIndex: 'processAmount', key: 'processAmount', width: 118, align: 'right', render: formatMoney },
    { title: '附加费', dataIndex: 'extraAmount', key: 'extraAmount', width: 118, align: 'right', render: formatMoney },
    { title: '应收', dataIndex: 'totalAmount', key: 'totalAmount', width: 128, align: 'right', render: formatMoney },
    { title: '已收', dataIndex: 'receivedAmount', key: 'receivedAmount', width: 128, align: 'right', render: formatMoney },
    { title: '未收', dataIndex: 'unreceivedAmount', key: 'unreceivedAmount', width: 128, align: 'right', render: formatMoney },
  ]
}

function detailColumns(onOpenOrder: (uuid: string) => void): ColumnsType<ReportDetailVO> {
  return [
    { title: '加工单号', dataIndex: 'orderNo', key: 'orderNo', width: 156, fixed: 'left', render: (_, record) => orderLinkCell(record, onOpenOrder) },
    { title: '日期', dataIndex: 'orderDate', key: 'orderDate', width: 108 },
    { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 170, render: textCell },
    { title: '纸品规格', dataIndex: 'paperSummary', key: 'paperSummary', width: 260, render: textCell },
    { title: '工艺', dataIndex: 'processSummary', key: 'processSummary', width: 140, render: tagTextCell },
    { title: '状态', dataIndex: 'orderStatus', key: 'orderStatus', width: 96, render: statusCell },
    { title: '结算', dataIndex: 'settleType', key: 'settleType', width: 92, render: settleTypeCell },
    { title: '开票', dataIndex: 'isInvoice', key: 'isInvoice', width: 92, render: invoiceCell },
    { title: '原卷', dataIndex: 'originalRollCount', key: 'originalRollCount', width: 88, align: 'right', render: countCell('卷') },
    { title: '成品', dataIndex: 'finishRollCount', key: 'finishRollCount', width: 88, align: 'right', render: countCell('卷') },
    { title: '原纸重量', dataIndex: 'originalWeight', key: 'originalWeight', width: 128, align: 'right', render: formatKg },
    { title: '成品重量', dataIndex: 'finishWeight', key: 'finishWeight', width: 128, align: 'right', render: formatKg },
    { title: '损耗', dataIndex: 'lossWeight', key: 'lossWeight', width: 118, align: 'right', render: formatKg },
    { title: '损耗率', dataIndex: 'lossRatio', key: 'lossRatio', width: 98, align: 'right', render: formatPercent },
    { title: '刀数', dataIndex: 'knifeCount', key: 'knifeCount', width: 88, align: 'right', render: numberCell },
    { title: '锯纸费', dataIndex: 'sawAmount', key: 'sawAmount', width: 116, align: 'right', render: formatMoney },
    { title: '复卷费', dataIndex: 'rewindAmount', key: 'rewindAmount', width: 116, align: 'right', render: formatMoney },
    { title: '加工费', dataIndex: 'processAmount', key: 'processAmount', width: 116, align: 'right', render: formatMoney },
    { title: '附加费', dataIndex: 'extraAmount', key: 'extraAmount', width: 116, align: 'right', render: formatMoney },
    { title: '应收', dataIndex: 'totalAmount', key: 'totalAmount', width: 124, align: 'right', render: formatMoney },
    { title: '已收', dataIndex: 'receivedAmount', key: 'receivedAmount', width: 124, align: 'right', render: formatMoney },
    { title: '未收', dataIndex: 'unreceivedAmount', key: 'unreceivedAmount', width: 124, align: 'right', render: formatMoney },
  ]
}

function orderLinkCell(record: ReportDetailVO, onOpenOrder: (uuid: string) => void) {
  return (
    <MesTooltip title="打开加工单详情">
      <Button
        className="report-order-link"
        type="link"
        size="small"
        onClick={() => onOpenOrder(record.orderUuid)}
      >
        {record.orderNo || '-'}
      </Button>
    </MesTooltip>
  )
}

function dimensionCell(value: string, record: ReportDimensionVO) {
  return (
    <Space size={6}>
      <TooltipText value={value || record.dimensionKey} />
      {record.dimensionKey === 'none' && <Tag>未分配</Tag>}
    </Space>
  )
}

function tagTextCell(value?: string) {
  return <Tag color="blue">{value || '-'}</Tag>
}

function textCell(value?: string | number) {
  return <TooltipText value={value} />
}

function countCell(unit: string) {
  return (value?: number) => `${formatNumber(value)} ${unit}`
}

function numberCell(value?: number) {
  return formatNumber(value)
}

function statusCell(value?: number) {
  const item = statusMap[value ?? -1]
  return <Tag color={item?.color ?? 'default'}>{item?.text ?? '-'}</Tag>
}

function settleTypeCell(value?: number) {
  return value === 1 ? '次结' : value === 2 ? '月结' : '-'
}

function invoiceCell(value?: number) {
  return <Tag color={value === 1 ? 'gold' : 'default'}>{value === 1 ? '开票' : '不开票'}</Tag>
}

function dimensionTitle(dimension: ReportDimension) {
  const titles: Record<ReportDimension, string> = {
    customer: '客户',
    invoice: '开票',
    machine: '机台',
    month: '月份',
    paper: '产品',
    process: '工艺',
    settleType: '结算方式',
    status: '状态',
  }
  return titles[dimension]
}

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待下发', color: 'blue' },
  2: { text: '加工中', color: 'processing' },
  3: { text: '待回录', color: 'warning' },
  4: { text: '已完成', color: 'success' },
  5: { text: '已结算', color: 'cyan' },
}
