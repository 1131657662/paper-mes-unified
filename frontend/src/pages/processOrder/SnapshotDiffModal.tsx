import { useEffect, useState } from 'react'
import { Modal, Spin, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getSnapshotDiff } from '../../api/processOrder'
import type { FinishDiff, RollDiff, SnapshotDiffVO } from '../../types/processOrder'

interface Props {
  uuid: string | null
  open: boolean
  onClose: () => void
}

const diffCell = (changed?: boolean, value?: number) =>
  changed ? <Tag color="red">{value ?? '-'}</Tag> : <span>{value ?? '-'}</span>

export default function SnapshotDiffModal({ uuid, open, onClose }: Props) {
  const [loading, setLoading] = useState(false)
  const [diff, setDiff] = useState<SnapshotDiffVO | null>(null)

  useEffect(() => {
    if (open && uuid) {
      setLoading(true)
      getSnapshotDiff(uuid)
        .then(setDiff)
        .catch(() => onClose())
        .finally(() => setLoading(false))
    } else {
      setDiff(null)
    }
  }, [open, uuid, onClose])

  const rollColumns: ColumnsType<RollDiff> = [
    { title: '母卷号', dataIndex: 'rollNo', width: 120 },
    {
      title: '克重(下发→完成)',
      width: 160,
      render: (_, r) => (
        <>
          {r.printGramWeight ?? '-'} → {diffCell(r.gramWeightChanged, r.finishGramWeight)}
        </>
      ),
    },
    {
      title: '门幅(下发→完成)',
      width: 160,
      render: (_, r) => (
        <>
          {r.printWidth ?? '-'} → {diffCell(r.widthChanged, r.finishWidth)}
        </>
      ),
    },
  ]

  const finishColumns: ColumnsType<FinishDiff> = [
    { title: '成品卷号', dataIndex: 'finishRollNo', width: 140 },
    {
      title: '门幅(mm)',
      width: 150,
      render: (_, r) => <>{r.printWidth || '现场确认'} → {diffCell(r.widthChanged, r.finishWidth)}</>,
    },
    {
      title: '直径(英寸)',
      width: 150,
      render: (_, r) => <>{r.printDiameter ?? '-'} → {diffCell(r.diameterChanged, r.finishDiameter)}</>,
    },
    { title: '预估(kg)', dataIndex: 'estimateWeight', width: 110 },
    {
      title: '实际(kg)',
      width: 110,
      render: (_, r) => diffCell(r.weightChanged, r.actualWeight),
    },
  ]

  return (
    <Modal
      title="快照对比（下发标称 vs 完成实际）"
      open={open}
      onCancel={onClose}
      footer={null}
      width={1040}
      destroyOnHidden
    >
      <Spin spinning={loading}>
        <Typography.Title level={5}>原纸快照差异</Typography.Title>
        <Table
          rowKey={(r) => r.uuid ?? r.rollNo ?? Math.random().toString()}
          size="small"
          columns={rollColumns}
          dataSource={diff?.rollDiffs ?? []}
          pagination={false}
          style={{ marginBottom: 16 }}
        />
        <Typography.Title level={5}>成品快照差异</Typography.Title>
        <Table
          rowKey={(r) => r.uuid ?? r.finishRollNo ?? Math.random().toString()}
          size="small"
          columns={finishColumns}
          dataSource={diff?.finishDiffs ?? []}
          pagination={false}
        />
      </Spin>
    </Modal>
  )
}
