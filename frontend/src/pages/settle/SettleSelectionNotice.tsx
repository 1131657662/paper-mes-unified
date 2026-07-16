import { Button, Tag } from 'antd'
import { DeleteOutlined, LockOutlined } from '@ant-design/icons'

interface Props {
  customerName?: string
  selectedCount: number
  onClear: () => void
}

export default function SettleSelectionNotice({ customerName, selectedCount, onClear }: Props) {
  if (selectedCount === 0) return null
  return (
    <div className="settle-selection-notice">
      <Tag icon={<LockOutlined />} color="blue">
        已锁定客户：{customerName ?? '-'}，其他客户暂不可选
      </Tag>
      <Button size="small" icon={<DeleteOutlined />} onClick={onClear}>清空已选</Button>
    </div>
  )
}
