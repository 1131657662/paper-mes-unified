import { WalletOutlined } from '@ant-design/icons'
import { Button, Segmented, Space } from 'antd'

export type SettleListViewMode = 'documents' | 'collection'

interface Props {
  canReceive: boolean
  receiveDisabled: boolean
  value: SettleListViewMode
  onChange: (value: SettleListViewMode) => void
  onReceive: () => void
}

export default function SettleListModeActions(props: Props) {
  return (
    <Space wrap>
      <Segmented aria-label="结算列表视图" value={props.value}
        options={[{ label: '单据列表', value: 'documents' }, { label: '催收队列', value: 'collection' }]}
        onChange={(value) => props.onChange(value as SettleListViewMode)} />
      {props.canReceive && (
        <Button icon={<WalletOutlined />} disabled={props.receiveDisabled} onClick={props.onReceive}>
          登记收款
        </Button>
      )}
    </Space>
  )
}
