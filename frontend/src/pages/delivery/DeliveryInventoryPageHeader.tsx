import { DownloadOutlined, InboxOutlined, ReloadOutlined, ToolOutlined } from '@ant-design/icons'
import { Badge, Button, Segmented, Space, Tooltip, Typography } from 'antd'

interface Props {
  exporting: boolean
  onExport: () => void
  onOpenRepair?: () => void
  onRefresh: () => void
  onViewChange: (value: 'customers' | 'finishes') => void
  updatedAt: number
  view: 'customers' | 'finishes'
  unassignedCount?: number
}

export default function DeliveryInventoryPageHeader(props: Props) {
  return (
    <header className="delivery-inventory-header">
      <div className="delivery-inventory-header__title-group">
        <span className="delivery-inventory-header__icon" aria-hidden="true"><InboxOutlined /></span>
        <Typography.Title level={4}>成品库存</Typography.Title>
        <Segmented
          aria-label="库存视图"
          value={props.view}
          options={[{ label: '按客户', value: 'customers' }, { label: '按成品', value: 'finishes' }]}
          onChange={(value) => props.onViewChange(value as 'customers' | 'finishes')}
        />
      </div>
      <Space className="delivery-inventory-header__actions" size={8}>
        <Typography.Text type="secondary">更新于 {updatedTime(props.updatedAt)}</Typography.Text>
        {props.onOpenRepair ? (
          <Badge count={props.unassignedCount} size="small" overflowCount={99}>
            <Button icon={<ToolOutlined />} onClick={props.onOpenRepair}>未分仓治理</Button>
          </Badge>
        ) : null}
        <Tooltip title="刷新库存">
          <Button aria-label="刷新库存" icon={<ReloadOutlined />} onClick={props.onRefresh} />
        </Tooltip>
        <Button icon={<DownloadOutlined />} loading={props.exporting} onClick={props.onExport}>导出</Button>
      </Space>
    </header>
  )
}

function updatedTime(value: number) {
  return value ? new Date(value).toLocaleTimeString('zh-CN', { hour12: false }) : '-'
}
