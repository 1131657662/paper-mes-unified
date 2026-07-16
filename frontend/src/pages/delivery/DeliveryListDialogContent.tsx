import { Input } from 'antd'

export function SignUserInput({ onChange }: { onChange: (value: string) => void }) {
  return <div className="delivery-sign-modal">
    <p>确认后将扣减所选成品库存，状态变为已出库。</p>
    <Input placeholder="签收人姓名（可选）" onChange={(event) => onChange(event.target.value)} />
  </div>
}

export function RollbackReasonInput({ onChange }: { onChange: (value: string) => void }) {
  return <div className="delivery-sign-modal">
    <p>回退后成品卷恢复为已入库，出库单回到待出库状态，可在详情页调整后重新签收。</p>
    <Input.TextArea rows={3} placeholder="请输入回退原因" onChange={(event) => onChange(event.target.value)} />
  </div>
}
