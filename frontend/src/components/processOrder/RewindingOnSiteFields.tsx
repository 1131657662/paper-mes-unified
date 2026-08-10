import { InputNumber, Typography } from 'antd'

export interface RewindingOnSiteValue {
  processMode: number
  rewindMode: number
  spareCount: number
  totalFinishCount: number
  unitPrice?: number
}

interface Props {
  value: RewindingOnSiteValue
  onChange: (count: number) => void
}

export default function RewindingOnSiteFields({ value, onChange }: Props) {
  return (
    <div>
      <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
        现场定尺模式：请输入预计成品件数，保存后由后端生成对应数量的正式成品号
      </Typography.Text>
      <InputNumber
        aria-label="预计成品件数"
        min={1}
        value={value.totalFinishCount || 1}
        onChange={(count) => onChange(count ?? 1)}
        addonBefore="预计成品件数"
        suffix="件"
      />
    </div>
  )
}
