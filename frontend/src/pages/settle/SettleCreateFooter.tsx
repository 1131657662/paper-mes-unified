import { FileDoneOutlined } from '@ant-design/icons'
import { Button, Tag } from 'antd'
import { formatMoney } from '../../features/settle/utils/settleFormatters'

interface Props {
  amount: number
  count: number
  disabled?: boolean
  loading: boolean
  pendingPriceCount: number
  onCancel: () => void
  onSubmit: () => void
}

export default function SettleCreateFooter(props: Props) {
  return (
    <footer className="settle-create-footer">
      <div className="settle-create-footer__summary">
        <span>本次结算 <strong>{props.count}</strong> 单</span>
        <span>应收合计 <strong>{formatMoney(props.amount)}</strong></span>
        {props.pendingPriceCount > 0 && <Tag color="warning">{props.pendingPriceCount} 单待核价</Tag>}
      </div>
      <div className="settle-create-footer__actions">
        <Button onClick={props.onCancel}>取消</Button>
        <Button
          type="primary"
          icon={<FileDoneOutlined />}
          loading={props.loading}
          disabled={props.disabled || props.count === 0 || props.pendingPriceCount > 0}
          onClick={props.onSubmit}
        >
          生成结算单
        </Button>
      </div>
    </footer>
  )
}
