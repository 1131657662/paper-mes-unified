import { FullscreenExitOutlined, FullscreenOutlined } from '@ant-design/icons'
import { Button, Space, Tooltip } from 'antd'
import type { AvailableFinishVO } from '../../types/delivery'
import { DeliveryFinishScopeControl } from './DeliveryFinishScopeControl'
import type { DeliveryFinishScope } from './deliveryFinishScope'

interface Props {
  expanded: boolean
  finishes: AvailableFinishVO[]
  scope: DeliveryFinishScope
  selectedRowKeys: React.Key[]
  totalCount: number
  scopeTotals?: { product: number; remain: number }
  onScopeChange: (value: DeliveryFinishScope) => void
  onToggleExpanded: () => void
}

export default function DeliverySelectionHeaderActions(props: Props) {
  const label = props.expanded ? '恢复页面布局' : '扩大选择区'
  return (
    <Space className="delivery-selection-header-actions" size={8} wrap>
      <DeliveryFinishScopeControl
        finishes={props.finishes}
        selectedRowKeys={props.selectedRowKeys}
        totalCount={props.totalCount}
        scopeTotals={props.scopeTotals}
        value={props.scope}
        onChange={props.onScopeChange}
      />
      <Tooltip title={label}>
        <Button
          aria-label={label}
          aria-pressed={props.expanded}
          icon={props.expanded ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
          onClick={props.onToggleExpanded}
        />
      </Tooltip>
    </Space>
  )
}
