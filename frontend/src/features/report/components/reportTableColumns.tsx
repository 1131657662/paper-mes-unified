import { Button, Space, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import DocumentTooltip from '../../../components/biz/MesTooltip'
import TooltipText from '../../../components/biz/TooltipText'
import { ORDER_STATUS } from '../../../constants/processOrder'
import type { ReportDetailVO, ReportDimension, ReportDimensionVO } from '../../../types/report'
import { formatMoney, formatNumber, formatPercent, formatTonFromKg } from '../utils/reportFormatters'
import { hasWeightGain, weightGain } from '../utils/reportWeightBalance'

export function dimensionColumns(dimension: ReportDimension): ColumnsType<ReportDimensionVO> {
  return [
    { title: dimensionTitle(dimension), dataIndex: 'dimensionName', key: 'dimensionName', width: 180, render: dimensionCell },
    { title: '加工单', dataIndex: 'orderCount', key: 'orderCount', width: 96, align: 'right', render: countCell('单') },
    { title: '原卷', dataIndex: 'originalRollCount', key: 'originalRollCount', width: 88, align: 'right', render: countCell('卷') },
    { title: '成品', dataIndex: 'finishRollCount', key: 'finishRollCount', width: 88, align: 'right', render: countCell('卷') },
    { title: '原纸吨位', dataIndex: 'originalWeight', key: 'originalWeight', width: 128, align: 'right', render: formatTonFromKg },
    { title: '成品吨位', dataIndex: 'finishWeight', key: 'finishWeight', width: 128, align: 'right', render: formatTonFromKg },
    { title: '重量校验', key: 'weightBalance', width: 118, render: (_, record) => balanceCell(record.originalWeight, record.finishWeight) },
    { title: '损耗吨位', dataIndex: 'lossWeight', key: 'lossWeight', width: 120, align: 'right', render: formatTonFromKg },
    { title: '损耗率', dataIndex: 'lossRatio', key: 'lossRatio', width: 98, align: 'right', render: formatPercent },
    { title: '刀数', dataIndex: 'knifeCount', key: 'knifeCount', width: 88, align: 'right', render: numberCell },
    { title: '锯纸费', dataIndex: 'sawAmount', key: 'sawAmount', width: 118, align: 'right', render: formatMoney },
    { title: '复卷费', dataIndex: 'rewindAmount', key: 'rewindAmount', width: 118, align: 'right', render: formatMoney },
    { title: '加工费', dataIndex: 'processAmount', key: 'processAmount', width: 118, align: 'right', render: formatMoney },
    { title: '附加费', dataIndex: 'extraAmount', key: 'extraAmount', width: 118, align: 'right', render: formatMoney },
    { title: '应收合计', dataIndex: 'totalAmount', key: 'totalAmount', width: 128, align: 'right', render: formatMoney },
    { title: '已结算应收', dataIndex: 'settledAmount', key: 'settledAmount', width: 132, align: 'right', render: formatMoney },
    { title: '待结算应收', dataIndex: 'pendingSettleAmount', key: 'pendingSettleAmount', width: 132, align: 'right', render: formatMoney },
    { title: '已结清', dataIndex: 'receivedAmount', key: 'receivedAmount', width: 128, align: 'right', render: formatMoney },
    { title: '实际到账', dataIndex: 'cashReceivedAmount', key: 'cashReceivedAmount', width: 128, align: 'right', render: formatMoney },
    { title: '废纸抵扣', dataIndex: 'scrapOffsetAmount', key: 'scrapOffsetAmount', width: 128, align: 'right', render: formatMoney },
    { title: '已结算未收', dataIndex: 'unreceivedAmount', key: 'unreceivedAmount', width: 128, align: 'right', render: formatMoney },
  ]
}

export function detailColumns(onOpenOrder: (uuid: string) => void): ColumnsType<ReportDetailVO> {
  return [
    { title: '加工单号', dataIndex: 'orderNo', key: 'orderNo', width: 156, fixed: 'left', render: (_, record) => orderLinkCell(record, onOpenOrder) },
    { title: '制单日期', dataIndex: 'orderDate', key: 'orderDate', width: 108 },
    { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 170, render: textCell },
    { title: '纸品规格', dataIndex: 'paperSummary', key: 'paperSummary', width: 260, render: textCell },
    { title: '工艺', dataIndex: 'processSummary', key: 'processSummary', width: 140, render: tagTextCell },
    { title: '状态', dataIndex: 'orderStatus', key: 'orderStatus', width: 96, render: statusCell },
    { title: '结算', dataIndex: 'settleType', key: 'settleType', width: 92, render: settleTypeCell },
    { title: '开票', dataIndex: 'isInvoice', key: 'isInvoice', width: 92, render: invoiceCell },
    { title: '原卷', dataIndex: 'originalRollCount', key: 'originalRollCount', width: 88, align: 'right', render: countCell('卷') },
    { title: '成品', dataIndex: 'finishRollCount', key: 'finishRollCount', width: 88, align: 'right', render: countCell('卷') },
    { title: '原纸吨位', dataIndex: 'originalWeight', key: 'originalWeight', width: 128, align: 'right', render: formatTonFromKg },
    { title: '成品吨位', dataIndex: 'finishWeight', key: 'finishWeight', width: 128, align: 'right', render: formatTonFromKg },
    { title: '重量校验', key: 'weightBalance', width: 118, render: (_, record) => balanceCell(record.originalWeight, record.finishWeight) },
    { title: '损耗吨位', dataIndex: 'lossWeight', key: 'lossWeight', width: 118, align: 'right', render: formatTonFromKg },
    { title: '损耗率', dataIndex: 'lossRatio', key: 'lossRatio', width: 98, align: 'right', render: formatPercent },
    { title: '刀数', dataIndex: 'knifeCount', key: 'knifeCount', width: 88, align: 'right', render: numberCell },
    { title: '锯纸费', dataIndex: 'sawAmount', key: 'sawAmount', width: 116, align: 'right', render: formatMoney },
    { title: '复卷费', dataIndex: 'rewindAmount', key: 'rewindAmount', width: 116, align: 'right', render: formatMoney },
    { title: '加工费', dataIndex: 'processAmount', key: 'processAmount', width: 116, align: 'right', render: formatMoney },
    { title: '附加费', dataIndex: 'extraAmount', key: 'extraAmount', width: 116, align: 'right', render: formatMoney },
    { title: '应收合计', dataIndex: 'totalAmount', key: 'totalAmount', width: 124, align: 'right', render: formatMoney },
    { title: '已结算应收', dataIndex: 'settledAmount', key: 'settledAmount', width: 132, align: 'right', render: formatMoney },
    { title: '待结算应收', dataIndex: 'pendingSettleAmount', key: 'pendingSettleAmount', width: 132, align: 'right', render: formatMoney },
    { title: '已结清', dataIndex: 'receivedAmount', key: 'receivedAmount', width: 124, align: 'right', render: formatMoney },
    { title: '实际到账', dataIndex: 'cashReceivedAmount', key: 'cashReceivedAmount', width: 124, align: 'right', render: formatMoney },
    { title: '废纸抵扣', dataIndex: 'scrapOffsetAmount', key: 'scrapOffsetAmount', width: 124, align: 'right', render: formatMoney },
    { title: '已结算未收', dataIndex: 'unreceivedAmount', key: 'unreceivedAmount', width: 124, align: 'right', render: formatMoney },
  ]
}

function orderLinkCell(record: ReportDetailVO, onOpenOrder: (uuid: string) => void) {
  return (
    <DocumentTooltip title="打开加工单详情">
      <Button className="report-order-link" type="link" size="small" onClick={() => onOpenOrder(record.orderUuid)}>
        {record.orderNo || '-'}
      </Button>
    </DocumentTooltip>
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
  const item = ORDER_STATUS[value ?? -1]
  return <Tag color={item?.color ?? 'default'}>{item?.text ?? '-'}</Tag>
}

function settleTypeCell(value?: number) {
  return value === 1 ? '次结' : value === 2 ? '月结' : '-'
}

function invoiceCell(value?: number) {
  return <Tag color={value === 1 ? 'gold' : 'default'}>{value === 1 ? '开票' : '不开票'}</Tag>
}

function balanceCell(originalWeight?: number, finishWeight?: number) {
  const difference = weightGain(originalWeight, finishWeight)
  if (hasWeightGain(originalWeight, finishWeight)) return <Tag color="error">产出超出 {formatTonFromKg(difference)}</Tag>
  return <Tag color="success">正常</Tag>
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
