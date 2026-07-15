import { Table, Tag } from 'antd'
import type { ReportDimensionVO } from '../../../types/report'
import { formatMoney, formatNumber, formatPercent, formatTonFromKg } from '../utils/reportFormatters'
import { hasWeightGain } from '../utils/reportWeightBalance'

interface Props {
  rows: ReportDimensionVO[]
}

export default function ReportDimensionSummaryRow({ rows }: Props) {
  const totals = sumDimensions(rows)

  return (
    <Table.Summary fixed>
      <Table.Summary.Row className="report-table-summary-row">
        <Table.Summary.Cell index={0}>合计</Table.Summary.Cell>
        <Table.Summary.Cell index={1} align="right">{formatNumber(totals.orderCount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={2} align="right">{formatNumber(totals.originalRollCount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={3} align="right">{formatNumber(totals.finishRollCount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={4} align="right">{formatTonFromKg(totals.originalWeight)}</Table.Summary.Cell>
        <Table.Summary.Cell index={5} align="right">{formatTonFromKg(totals.finishWeight)}</Table.Summary.Cell>
        <Table.Summary.Cell index={6} align="center">
          {hasWeightGain(totals.originalWeight, totals.finishWeight)
            ? <Tag color="error">产出超出</Tag>
            : <Tag color="success">正常</Tag>}
        </Table.Summary.Cell>
        <Table.Summary.Cell index={7} align="right">{formatTonFromKg(totals.lossWeight)}</Table.Summary.Cell>
        <Table.Summary.Cell index={8} align="right">{formatPercent(totals.lossRatio)}</Table.Summary.Cell>
        <Table.Summary.Cell index={9} align="right">{formatNumber(totals.knifeCount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={10} align="right">{formatMoney(totals.sawAmount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={11} align="right">{formatMoney(totals.rewindAmount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={12} align="right">{formatMoney(totals.processAmount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={13} align="right">{formatMoney(totals.extraAmount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={14} align="right">{formatMoney(totals.totalAmount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={15} align="right">{formatMoney(totals.settledAmount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={16} align="right">{formatMoney(totals.pendingSettleAmount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={17} align="right">{formatMoney(totals.receivedAmount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={18} align="right">{formatMoney(totals.cashReceivedAmount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={19} align="right">{formatMoney(totals.scrapOffsetAmount)}</Table.Summary.Cell>
        <Table.Summary.Cell index={20} align="right">{formatMoney(totals.unreceivedAmount)}</Table.Summary.Cell>
      </Table.Summary.Row>
    </Table.Summary>
  )
}

function sumDimensions(rows: ReportDimensionVO[]) {
  const totals = rows.reduce((acc, row) => ({
    cashReceivedAmount: acc.cashReceivedAmount + Number(row.cashReceivedAmount ?? 0),
    extraAmount: acc.extraAmount + Number(row.extraAmount ?? 0),
    finishRollCount: acc.finishRollCount + Number(row.finishRollCount ?? 0),
    finishWeight: acc.finishWeight + Number(row.finishWeight ?? 0),
    knifeCount: acc.knifeCount + Number(row.knifeCount ?? 0),
    lossWeight: acc.lossWeight + Number(row.lossWeight ?? 0),
    orderCount: acc.orderCount + Number(row.orderCount ?? 0),
    originalRollCount: acc.originalRollCount + Number(row.originalRollCount ?? 0),
    originalWeight: acc.originalWeight + Number(row.originalWeight ?? 0),
    pendingSettleAmount: acc.pendingSettleAmount + Number(row.pendingSettleAmount ?? 0),
    processAmount: acc.processAmount + Number(row.processAmount ?? 0),
    receivedAmount: acc.receivedAmount + Number(row.receivedAmount ?? 0),
    rewindAmount: acc.rewindAmount + Number(row.rewindAmount ?? 0),
    sawAmount: acc.sawAmount + Number(row.sawAmount ?? 0),
    scrapOffsetAmount: acc.scrapOffsetAmount + Number(row.scrapOffsetAmount ?? 0),
    settledAmount: acc.settledAmount + Number(row.settledAmount ?? 0),
    totalAmount: acc.totalAmount + Number(row.totalAmount ?? 0),
    unreceivedAmount: acc.unreceivedAmount + Number(row.unreceivedAmount ?? 0),
  }), emptyTotals())

  return {
    ...totals,
    lossRatio: totals.originalWeight > 0 ? (totals.lossWeight / totals.originalWeight) * 100 : 0,
  }
}

function emptyTotals() {
  return {
    cashReceivedAmount: 0,
    extraAmount: 0,
    finishRollCount: 0,
    finishWeight: 0,
    knifeCount: 0,
    lossWeight: 0,
    orderCount: 0,
    originalRollCount: 0,
    originalWeight: 0,
    pendingSettleAmount: 0,
    processAmount: 0,
    receivedAmount: 0,
    rewindAmount: 0,
    sawAmount: 0,
    scrapOffsetAmount: 0,
    settledAmount: 0,
    totalAmount: 0,
    unreceivedAmount: 0,
  }
}
