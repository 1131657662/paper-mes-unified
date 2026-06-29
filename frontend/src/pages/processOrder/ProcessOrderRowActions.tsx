import { Button, Modal, Space } from 'antd'
import type { ProcessOrder } from '../../types/processOrder'

interface Props {
  record: ProcessOrder
  onBackRecord: (uuid: string) => void
  onChangeStatus: (record: ProcessOrder, target: number) => Promise<void>
  onDetail: (uuid: string) => void
  onEditDraft: (uuid: string) => void
  onGoDelivery: () => void
  onPrint: (record: ProcessOrder) => void
}

export default function ProcessOrderRowActions({ record, ...actions }: Props) {
  const primary = primaryAction(record, actions)

  return (
    <Space size={6} wrap={false} className="process-order-list__actions">
      <Button type="link" size="small" onClick={() => actions.onDetail(record.uuid)}>
        详情
      </Button>
      {primary && (
        <Button type="link" size="small" onClick={primary.onClick}>
          {primary.label}
        </Button>
      )}
    </Space>
  )
}

function primaryAction(record: ProcessOrder, actions: Omit<Props, 'record'>) {
  const status = record.orderStatus ?? 0
  if (status === 0) return { label: '继续编辑', onClick: () => actions.onEditDraft(record.uuid) }
  if (status === 1) return { label: '打印下发', onClick: () => actions.onPrint(record) }
  if (status === 2) return { label: '转待回录', onClick: () => confirmStatus(record, 3, '确认车间已完成加工，转入待回录？', actions) }
  if (status === 3) return { label: '进入回录', onClick: () => actions.onBackRecord(record.uuid) }
  if (status === 4) return { label: '创建出库', onClick: actions.onGoDelivery }
  return null
}

function confirmStatus(
  record: ProcessOrder,
  targetStatus: number,
  title: string,
  actions: Omit<Props, 'record'>,
) {
  Modal.confirm({
    title,
    content: record.orderNo,
    onOk: () => actions.onChangeStatus(record, targetStatus),
  })
}
