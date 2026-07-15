import { Typography } from 'antd'
import { formatKg, formatMoney } from '../../features/settle/utils/settleFormatters'
import type { SettleFeeLine, SettlePrintLine } from '../../types/settle'
import { formatGram, formatMm } from '../../utils/numberFormatters'

interface Props {
  line: SettlePrintLine
}

export function SettleOriginalCell({ line }: Props) {
  const identities = [
    line.originalRollNo && line.originalRollNo !== line.originalLabel && `卷号 ${line.originalRollNo}`,
    line.originalExtraNo && `编号 ${line.originalExtraNo}`,
  ].filter(Boolean)
  return (
    <div className="settle-cell-stack mes-cell-stack">
      <Typography.Text strong>{line.originalLabel || '-'}</Typography.Text>
      {identities.length > 0 && <span>{identities.join(' / ')}</span>}
    </div>
  )
}

export function SettleOriginalSpecCell({ line }: Props) {
  const gram = line.actualGramWeight ?? line.gramWeight
  const width = line.actualWidth ?? line.originalWidth
  const spec = [
    gram == null ? undefined : formatGram(gram),
    width == null ? undefined : formatMm(width),
  ].filter(Boolean).join(' / ')
  return (
    <div className="settle-cell-stack mes-cell-stack">
      <Typography.Text>{line.paperName || '-'}</Typography.Text>
      <span>{spec || '-'}</span>
    </div>
  )
}

export function SettleFeeBasisCell({ line }: Props) {
  const feeLines = processFeeLines(line)
  if (feeLines.length === 0) return <Typography.Text type="secondary">-</Typography.Text>
  return (
    <div className="settle-fee-lines settle-fee-lines--table">
      {feeLines.map((fee, index) => (
        <div className="settle-fee-line" key={feeKey(fee, index)}>
          <b>{fee.feeName}</b>
          <span>{feeFormula(fee)}</span>
          <strong>{formatMoney(fee.amountNoTax)}</strong>
        </div>
      ))}
    </div>
  )
}

export function SettleFinishResultCell({ line }: Props) {
  return (
    <div className="settle-cell-stack mes-cell-stack">
      <Typography.Text strong>{line.finishCount ?? 0} 卷 / {formatKg(line.finishWeight)}</Typography.Text>
      {line.finishSummary && line.finishSummary !== '-' && <span>卷号 {line.finishSummary}</span>}
    </div>
  )
}

export function SettleProcessNameCell({ line }: Props) {
  const names = Array.from(new Set(processFeeLines(line).map((fee) => fee.feeName.replace(/费$/, ''))))
  return <>{names.join(' / ') || line.processText || '-'}</>
}

export function SettleTrimCell({ line }: Props) {
  const value = line.trimSummary && line.trimSummary !== '-'
    ? line.trimSummary
    : formatKg(line.trimWeight)
  return <>{value}</>
}

function processFeeLines(line: SettlePrintLine) {
  return (line.feeLines ?? []).filter((fee) => fee.feeType === 'saw' || fee.feeType === 'rewind')
}

function feeFormula(fee: SettleFeeLine) {
  if (fee.formulaText) return fee.formulaText
  if (fee.quantity == null || fee.unitPrice == null) return '-'
  return `${fee.quantity}${fee.quantityUnit || ''} × ${formatMoney(fee.unitPrice)}`
}

function feeKey(fee: SettleFeeLine, index: number) {
  return `${fee.feeType}-${fee.stageLevel ?? 'x'}-${index}`
}
