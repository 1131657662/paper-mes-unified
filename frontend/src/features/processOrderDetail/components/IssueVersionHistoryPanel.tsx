import { Alert, Button, Flex, Spin, Table, Tag, Tooltip, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { EyeOutlined } from '@ant-design/icons'
import { useState } from 'react'
import QueryLoadErrorAlert from '../../../components/feedback/QueryLoadErrorAlert'
import type { ProcessOrderIssueVersion } from '../../../types/processOrder'
import { formatDateTime } from '../../../utils/dateTime'
import { useProcessOrderIssueVersions } from '../hooks/useProcessOrderIssueVersions'
import HistoricalIssueVersionDrawer from './HistoricalIssueVersionDrawer'

interface Props {
  orderUuid?: string
}

export default function IssueVersionHistoryPanel({ orderUuid }: Props) {
  const [historicalVersion, setHistoricalVersion] = useState<number>()
  const {
    data: issueVersions = [],
    isError: isIssueVersionsError,
    isLoading: isLoadingIssueVersions,
    refetch: refetchIssueVersions,
  } = useProcessOrderIssueVersions(orderUuid)

  return (
    <section className="order-detail-section">
      <div className="order-detail-section__header">
        <h2 className="order-detail-section__title">下发版本历史</h2>
        <Tag>{isLoadingIssueVersions ? '加载中' : `${issueVersions.length} 条`}</Tag>
      </div>
      <div className="order-detail-section__body">
        <IssueVersionHistoryContent
          isError={isIssueVersionsError}
          loading={isLoadingIssueVersions}
          versions={issueVersions}
          onViewHistorical={setHistoricalVersion}
          onRetry={() => void refetchIssueVersions()}
        />
      </div>
      <HistoricalIssueVersionDrawer issueVersion={historicalVersion} orderUuid={orderUuid}
        onClose={() => setHistoricalVersion(undefined)} />
    </section>
  )
}

export function IssueVersionHistoryContent(props: {
  isError: boolean
  loading: boolean
  onRetry: () => void
  onViewHistorical: (version: number) => void
  versions: ProcessOrderIssueVersion[]
}) {
  if (props.isError) return <QueryLoadErrorAlert message="下发版本历史加载失败"
    description="版本元数据未成功加载，当前空白不代表没有历史记录。" onRetry={props.onRetry} />
  if (props.loading) {
    return (
      <Flex className="issue-version-history__loading" align="center" justify="center" gap={12}>
        <Spin size="small" />
        <Typography.Text type="secondary">正在加载下发版本历史</Typography.Text>
      </Flex>
    )
  }
  const hasLegacy = props.versions.some((item) => item.status === 'LEGACY_UNVERSIONED')
  return (
    <Flex vertical gap={12}>
      {hasLegacy && <Alert type="warning" showIcon message="包含 V3.53 前历史下发快照"
        description="该快照未版本化；系统未补造版本号、操作者、变更时间或下发时间。" />}
      <Table rowKey={(row) => row.uuid ?? `legacy-${row.orderUuid}`} size="small"
        columns={versionColumns(props.onViewHistorical)} dataSource={props.versions} pagination={false}
        locale={{ emptyText: '暂无下发版本记录' }} scroll={{ x: 920 }} />
    </Flex>
  )
}

const versionColumns = (onViewHistorical: (version: number) => void): ColumnsType<ProcessOrderIssueVersion> => [
  { title: '版本', width: 80, render: (_, row) => row.versionNo == null ? '历史' : `V${row.versionNo}` },
  { title: '状态', width: 130, render: (_, row) => statusTag(row.status) },
  { title: '快照', width: 120, render: (_, row) => snapshotLabel(row) },
  { title: '变更原因', dataIndex: 'changeReason', width: 220, render: (value) => value || '-' },
  { title: '变更记录', width: 190, render: (_, row) => auditLabel(row.operatorName, row.changeTime) },
  { title: '下发记录', width: 190, render: (_, row) => auditLabel(row.issueOperatorName, row.issueTime) },
  {
    title: '追溯', width: 72, align: 'center', render: (_, row) => !canPreviewHistoricalVersion(row) ? '-' : (
      <Tooltip title={`查看历史下发 V${row.versionNo}（只读）`}>
        <Button aria-label={`查看历史下发 V${row.versionNo}`} icon={<EyeOutlined />} size="small" type="text"
          onClick={() => onViewHistorical(row.versionNo!)} />
      </Tooltip>
    ),
  },
]

function canPreviewHistoricalVersion(row: ProcessOrderIssueVersion): boolean {
  return row.versionNo != null
    && row.status === 'APPLIED'
    && row.hasSnapshotAfter
}

function statusTag(status: string) {
  if (status === 'LEGACY_UNVERSIONED') return <Tag color="gold">历史未版本化</Tag>
  if (status === 'PENDING') return <Tag color="blue">待重新下发</Tag>
  if (status === 'APPLIED') return <Tag color="green">已下发</Tag>
  if (status === 'ARCHIVED') return <Tag>已归档</Tag>
  return <Tag>{status}</Tag>
}

function snapshotLabel(row: ProcessOrderIssueVersion): string {
  if (row.status === 'LEGACY_UNVERSIONED') return '历史下发'
  if (row.hasSnapshotBefore && row.hasSnapshotAfter) return '变更前 / 下发后'
  if (row.hasSnapshotBefore) return '变更前'
  if (row.hasSnapshotAfter) return '下发后'
  return '-'
}

function auditLabel(operator?: string, time?: string) {
  return <span>{operator || '-'}<br /><Typography.Text type="secondary">
    {formatDateTime(time)}</Typography.Text></span>
}
