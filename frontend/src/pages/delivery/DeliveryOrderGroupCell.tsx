import { CalendarOutlined, CheckSquareOutlined, ClearOutlined, WarningOutlined } from '@ant-design/icons'
import { Button, Space, Tag, Tooltip, Typography } from 'antd'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryOrderGroupRow } from './deliveryFinishGrouping'

interface Props {
  group: DeliveryOrderGroupRow
  onClear: () => void
  onSelect: () => void
}

export default function DeliveryOrderGroupCell({ group, onClear, onSelect }: Props) {
  const scopeName = group.scope === 'remain' ? '余料' : '成品'
  return (
    <div className="delivery-order-group-cell">
      <div className="delivery-order-group-cell__identity">
        <Typography.Text strong>{group.orderNo}</Typography.Text>
        <span><CalendarOutlined /> {group.orderDate || '未记录日期'}</span>
        <Space className="delivery-order-group-cell__actions" size={2}>
          <Tooltip title={`全选本加工单${scopeName}`}>
            <Button
              type="text"
              size="small"
              aria-label={`全选本加工单${scopeName}`}
              icon={<CheckSquareOutlined />}
              disabled={group.selectedCount === group.totalCount}
              onClick={(event) => {
                event.stopPropagation()
                onSelect()
              }}
            />
          </Tooltip>
          <Tooltip title={`清空本加工单${scopeName}选择`}>
            <Button
              type="text"
              size="small"
              aria-label={`清空本加工单${scopeName}选择`}
              icon={<ClearOutlined />}
              disabled={group.selectedCount === 0}
              onClick={(event) => {
                event.stopPropagation()
                onClear()
              }}
            />
          </Tooltip>
        </Space>
      </div>
      <div className="delivery-order-group-cell__metrics">
        <span className="delivery-order-group-cell__total">
          本单{scopeName} <strong>{group.totalCount} 卷</strong> / <strong>{formatTon(group.totalWeight)}</strong>
        </span>
        <span className="delivery-order-group-cell__selected">
          已选{scopeName} <strong>{group.selectedCount} 卷</strong> / <strong>{formatTon(group.selectedWeight)}</strong>
        </span>
        {group.riskCount > 0 && (
          <Tag color="warning" icon={<WarningOutlined />}>
            {group.riskCount} 卷待收款确认
          </Tag>
        )}
      </div>
    </div>
  )
}
