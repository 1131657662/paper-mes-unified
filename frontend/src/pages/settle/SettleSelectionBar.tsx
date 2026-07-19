import { Button } from 'antd'
import { CloseOutlined, WalletOutlined } from '@ant-design/icons'
import type { SettleOrder } from '../../types/settle'
import { formatMoney } from '../../features/settle/utils/settleFormatters'
import './SettleSelectionBar.css'

interface Props {
  selectedRows: SettleOrder[]
  selectedReceivable: SettleOrder[]
  onClear: () => void
  onReceive: () => void
}

export default function SettleSelectionBar({ selectedRows, selectedReceivable, onClear, onReceive }: Props) {
  if (selectedRows.length === 0) return null

  const current = selectedReceivable.length === 1 ? selectedReceivable[0] : undefined
  const isReceiveReady = selectedRows.length === 1 && Boolean(current)

  return (
    <div className="settle-selection-bar" role="status" aria-live="polite">
      <div className="settle-selection-bar__summary">
        <strong>已选 {selectedRows.length} 张</strong>
        {current ? <span>{current.settleNo} · 未收 {formatMoney(current.unreceivedAmount)}</span> : (
          <span>登记收款一次只能处理一张未结清结算单</span>
        )}
      </div>
      <div className="settle-selection-bar__actions">
        <Button type="link" icon={<CloseOutlined />} onClick={onClear}>清除选择</Button>
        <Button type="primary" icon={<WalletOutlined />} disabled={!isReceiveReady} onClick={onReceive}>
          登记收款
        </Button>
      </div>
    </div>
  )
}
