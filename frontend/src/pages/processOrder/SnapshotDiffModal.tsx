import { Modal, Spin, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { useSnapshotDiff } from '../../features/processOrderDetail/hooks/useSnapshotDiff'
import type { FinishDiff, RollDiff, SnapshotDiffVO } from '../../types/processOrder'

interface Props {
  uuid: string | null
  open: boolean
  onClose: () => void
}

const diffCell = (changed?: boolean, value?: number) =>
  changed ? <Tag color="red">{value ?? '-'}</Tag> : <span>{value ?? '-'}</span>

export default function SnapshotDiffModal({ uuid, open, onClose }: Props) {
  const {
    data: diff,
    isError: isDiffError,
    isLoading: isLoadingDiff,
    refetch: refetchDiff,
  } = useSnapshotDiff(uuid ?? undefined, open)

  return (
    <Modal
      title="快照对比（下发标称 vs 完成实际）"
      open={open}
      onCancel={onClose}
      footer={null}
      width="min(1040px, calc(100vw - 32px))"
      destroyOnHidden
    >
      <SnapshotDiffLoadState diff={diff} isError={isDiffError} loading={isLoadingDiff}
        onRetry={() => void refetchDiff()} />
    </Modal>
  )
}

export function SnapshotDiffLoadState(props: {
  diff?: SnapshotDiffVO; isError: boolean; loading: boolean; onRetry: () => void
}) {
  if (props.isError) return <QueryLoadErrorAlert message="快照差异加载失败"
    description="下发快照与完成数据未成功加载，请重新加载后再核对。" onRetry={props.onRetry} />
  return (
      <Spin spinning={props.loading}>
        <Typography.Title level={5}>原纸快照差异</Typography.Title>
        <Table
          rowKey={(record, index) => record.uuid ?? record.rollNo ?? `roll-${index}`}
          size="small"
          columns={rollColumns}
          dataSource={props.diff?.rollDiffs ?? []}
          pagination={false}
          style={{ marginBottom: 16 }}
        />
        <Typography.Title level={5}>成品快照差异</Typography.Title>
        <Table
          rowKey={(record, index) => record.uuid ?? record.finishRollNo ?? `finish-${index}`}
          size="small"
          columns={finishColumns}
          dataSource={props.diff?.finishDiffs ?? []}
          pagination={false}
        />
      </Spin>
  )
}

const rollColumns: ColumnsType<RollDiff> = [
  { title: '母卷号', dataIndex: 'rollNo', width: 120 },
  { title: '克重(下发→完成)', width: 160, render: (_, row) => <>
    {row.printGramWeight ?? '-'} → {diffCell(row.gramWeightChanged, row.finishGramWeight)}</> },
  { title: '门幅(下发→完成)', width: 160, render: (_, row) => <>
    {row.printWidth ?? '-'} → {diffCell(row.widthChanged, row.finishWidth)}</> },
]

const finishColumns: ColumnsType<FinishDiff> = [
  { title: '成品卷号', dataIndex: 'finishRollNo', width: 140 },
  { title: '门幅(mm)', width: 150, render: (_, row) => <>
    {row.printWidth || '现场确认'} → {diffCell(row.widthChanged, row.finishWidth)}</> },
  { title: '直径(英寸)', width: 150, render: (_, row) => <>
    {row.printDiameter ?? '-'} → {diffCell(row.diameterChanged, row.finishDiameter)}</> },
  { title: '预估(kg)', dataIndex: 'estimateWeight', width: 110 },
  { title: '实际(kg)', width: 110, render: (_, row) => diffCell(row.weightChanged, row.actualWeight) },
]
