import { InputNumber } from 'antd'
import { formatFixedNumberInput } from '../../utils/numberFormatters'

interface Props {
  ariaLabel?: string
  disabled?: boolean
  max: number
  min: number
  placeholder?: string
  precision?: number
  unit: string
  value?: number
  onChange: (value?: number) => void
}

export default function CustomerSpecNumberInput({ ariaLabel, disabled, max, min, placeholder, precision, unit, value, onChange }: Props) {
  const formatter = precision == null ? undefined : (next: number | undefined, info: { userTyping: boolean; input: string }) => (
    info.userTyping ? info.input : formatFixedNumberInput(next, precision)
  )
  return (
    <div className="customer-spec-number-input">
      <InputNumber aria-label={ariaLabel} disabled={disabled} min={min} max={max} placeholder={placeholder} precision={precision} formatter={formatter} value={value} onChange={(next) => onChange(next ?? undefined)} />
      <span aria-hidden="true">{unit}</span>
    </div>
  )
}
