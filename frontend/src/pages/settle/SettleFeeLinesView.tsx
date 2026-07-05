import { Tag, Typography } from 'antd'
import { formatMoney } from '../../features/settle/utils/settleFormatters'
import type { SettleFeeLine } from '../../types/settle'

interface Props {
  feeLines?: SettleFeeLine[]
}

export function SettleFeeSourceCell({ feeLines }: Props) {
  if (!feeLines?.length) {
    return <Typography.Text type="secondary">-</Typography.Text>
  }

  return (
    <div className="settle-fee-lines settle-fee-lines--table">
      {feeLines.map((line, index) => (
        <div className="settle-fee-line" key={feeLineKey(line, index)}>
          <Tag className="mes-status-tag">{line.feeName}</Tag>
          <span>{line.formulaText || feeLineFormula(line)}</span>
          <strong>{formatMoney(line.amountTax ?? line.amountNoTax)}</strong>
        </div>
      ))}
    </div>
  )
}

export function SettleFeeSourcePrint({ feeLines }: Props) {
  if (!feeLines?.length) {
    return null
  }

  return (
    <span className="settle-fee-lines settle-fee-lines--print">
      {feeLines.map((line, index) => (
        <span className="settle-fee-line" key={feeLineKey(line, index)}>
          <b>{line.feeName}</b>
          <span>{line.formulaText || feeLineFormula(line)}</span>
          <em>{formatMoney(line.amountTax ?? line.amountNoTax)}</em>
        </span>
      ))}
    </span>
  )
}

function feeLineFormula(line: SettleFeeLine) {
  const quantity = line.quantity != null ? `${line.quantity}${line.quantityUnit || ''}` : undefined
  const unitPrice = line.unitPrice != null ? `${formatMoney(line.unitPrice)}` : undefined
  if (quantity && unitPrice) {
    return `${quantity} × ${unitPrice}`
  }
  return line.sourceText && line.outputText ? `${line.sourceText} -> ${line.outputText}` : '-'
}

function feeLineKey(line: SettleFeeLine, index: number) {
  return `${line.feeType}-${line.stageLevel ?? 'x'}-${index}`
}
