import { EyeOutlined } from '@ant-design/icons'
import { Button, Divider, Drawer, Empty, List, Tag, Typography } from 'antd'
import { useState } from 'react'
import { formatKg } from '../../utils/numberFormatters'
import type { DeliveryCustomerRevisionSummary } from './deliveryCustomerSpecTypes'
import DeliveryCustomerRevisionDetailTable from './DeliveryCustomerRevisionDetailTable'
import { useDeliveryCustomerSpecRevisionDetail, useDeliveryCustomerSpecRevisions } from './useDeliveryCustomerSpecs'

interface Props { open: boolean; uuid?: string; onClose: () => void }

export default function DeliveryCustomerRevisionHistoryDrawer({ open, uuid, onClose }: Props) {
  const [selectedUuid, setSelectedUuid] = useState<string>()
  const { data = [], isLoading } = useDeliveryCustomerSpecRevisions(uuid, open)
  const detailQuery = useDeliveryCustomerSpecRevisionDetail(uuid, selectedUuid)
  const close = () => { setSelectedUuid(undefined); onClose() }
  return (
    <Drawer title="客户更正版记录" open={open} width="min(1080px, calc(100vw - 24px))" onClose={close}>
      <List loading={isLoading} locale={{ emptyText: <Empty description="尚未发布客户更正版" /> }} dataSource={data} renderItem={(item) => (
        <List.Item actions={[<Button key="detail" type="link" icon={<EyeOutlined />} onClick={() => setSelectedUuid(item.uuid)}>查看明细</Button>]}><List.Item.Meta title={<span><Tag color="blue">V{item.revisionNo}</Tag>{item.reason}</span>} description={<Description item={item} />} /></List.Item>
      )} />
      {selectedUuid && <><Divider orientation="left">更正版逐件对照</Divider><DeliveryCustomerRevisionDetailTable detail={detailQuery.data} loading={detailQuery.isLoading} /></>}
    </Drawer>
  )
}

function Description({ item }: { item: DeliveryCustomerRevisionSummary }) {
  return <div className="delivery-customer-history"><Typography.Text type="secondary">{item.itemCount} 件 · {formatKg(item.customerTotalWeight)}</Typography.Text><span>{[item.operator, formatTime(item.createdAt)].filter(Boolean).join(' · ')}</span></div>
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : undefined
}
