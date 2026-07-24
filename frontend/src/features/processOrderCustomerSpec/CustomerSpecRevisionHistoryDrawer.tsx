import { EyeOutlined } from '@ant-design/icons'
import { Button, Divider, Drawer, Empty, List, Tag, Typography } from 'antd'
import { useState } from 'react'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { formatKg } from '../../utils/numberFormatters'
import type { FinishCustomerRevisionDetail, FinishCustomerRevisionSummary } from './customerSpecTypes'
import CustomerSpecRevisionDetailTable from './CustomerSpecRevisionDetailTable'
import { useFinishCustomerSpecRevisionDetail, useFinishCustomerSpecRevisions } from './useFinishCustomerSpecs'

interface Props { open: boolean; orderUuid?: string; onClose: () => void }

export default function CustomerSpecRevisionHistoryDrawer({ open, orderUuid, onClose }: Props) {
  const [selectedUuid, setSelectedUuid] = useState<string>()
  const historyQuery = useFinishCustomerSpecRevisions(orderUuid, open)
  const detailQuery = useFinishCustomerSpecRevisionDetail(orderUuid, selectedUuid)
  const close = () => { setSelectedUuid(undefined); onClose() }
  return (
    <Drawer title="客户口径版本" open={open} width="min(1080px, calc(100vw - 24px))" onClose={close}>
      <CustomerSpecRevisionList data={historyQuery.data} isError={historyQuery.isError}
        loading={historyQuery.isLoading} onRetry={() => void historyQuery.refetch()} onSelect={setSelectedUuid} />
      {selectedUuid && <CustomerSpecRevisionDetailSection detail={detailQuery.data}
        isError={detailQuery.isError} loading={detailQuery.isLoading}
        onRetry={() => void detailQuery.refetch()} />}
    </Drawer>
  )
}

export function CustomerSpecRevisionList(props: {
  data?: FinishCustomerRevisionSummary[]; isError: boolean; loading: boolean
  onRetry: () => void; onSelect: (uuid: string) => void
}) {
  if (props.isError) return <QueryLoadErrorAlert message="客户口径版本加载失败"
    description="版本列表未成功加载，当前空白不代表没有历史版本。" onRetry={props.onRetry} />
  return <List loading={props.loading} dataSource={props.data ?? []}
    locale={{ emptyText: <Empty description="尚未发布客户口径版本" /> }}
    renderItem={(item) => <List.Item actions={[<Button key="detail" type="link"
      icon={<EyeOutlined />} onClick={() => props.onSelect(item.uuid)}>查看明细</Button>]}>
      <List.Item.Meta title={<span><Tag color="blue">V{item.revisionNo}</Tag>{item.reason}</span>}
        description={<VersionDescription item={item} />} />
    </List.Item>} />
}

export function CustomerSpecRevisionDetailSection(props: {
  detail?: FinishCustomerRevisionDetail; isError: boolean; loading: boolean; onRetry: () => void
}) {
  return <><Divider orientation="left">版本逐件对照</Divider>{props.isError
    ? <QueryLoadErrorAlert message="客户口径版本明细加载失败"
        description="该版本明细未成功加载，请重新加载后查看。" onRetry={props.onRetry} />
    : <CustomerSpecRevisionDetailTable detail={props.detail} loading={props.loading} />}</>
}

function VersionDescription({ item }: { item: FinishCustomerRevisionSummary }) {
  return <div className="customer-version-description"><Typography.Text type="secondary">{item.itemCount} 件 · {formatKg(item.customerTotalWeight)}</Typography.Text><span>{[item.operator, formatTime(item.createdAt)].filter(Boolean).join(' · ')}</span></div>
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : undefined
}
