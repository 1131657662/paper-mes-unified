import { InputNumber } from 'antd'

interface Props {
  label: string
  min?: number
  precision?: number
  value?: number
  onChange: (value?: number) => void
}

export default function ProcessRouteNumberField({
  label,
  min = 0,
  precision,
  value,
  onChange,
}: Props) {
  return (
    <label className="process-route-number">
      <span>{label}</span>
      <InputNumber
        min={min}
        precision={precision}
        value={value}
        onChange={(nextValue) => onChange(nextValue == null ? undefined : Number(nextValue))}
      />
    </label>
  )
}
