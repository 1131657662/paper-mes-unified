import { CheckOutlined, CreditCardOutlined, LinkOutlined, UndoOutlined } from '@ant-design/icons'
import { Button, Form, Input, Modal } from 'antd'
import { useBindRemainNextSettlement } from '../../features/remain/hooks/useBindRemainNextSettlement'
import { useCancelRemainAdjustment } from '../../features/remain/hooks/useCancelRemainAdjustment'
import { useCreditRemainAdjustment } from '../../features/remain/hooks/useCreditRemainAdjustment'
import { useReverseRemainCredit } from '../../features/remain/hooks/useReverseRemainCredit'
import { useCreateRemainRefund } from '../../features/remain/hooks/useCreateRemainRefund'
import type { RemainAdjustment } from '../../types/remain'

export type RemainAdjustmentAction = 'next-settlement' | 'credit' | 'reverse-credit' | 'refund' | 'cancel'

interface Props { row?: RemainAdjustment; action?: RemainAdjustmentAction; onClose: () => void }

const ACTION_COPY: Record<RemainAdjustmentAction, { title: string; button: string }> = {
  'next-settlement': { title: '挂接下一张结算', button: '确认挂接' },
  credit: { title: '入客户余款', button: '确认入账' },
  'reverse-credit': { title: '冲回客户余款', button: '确认冲回' },
  refund: { title: '申请退款', button: '提交申请' },
  cancel: { title: '取消待调整', button: '确认取消' },
}

export function RemainAdjustmentActionModal({ row, action, onClose }: Props) {
  const [form] = Form.useForm()
  const nextSettlement = useBindRemainNextSettlement()
  const credit = useCreditRemainAdjustment()
  const reverseCredit = useReverseRemainCredit()
  const refund = useCreateRemainRefund()
  const cancel = useCancelRemainAdjustment()
  if (!row || !action) return null
  const copy = ACTION_COPY[action]
  const isPending = nextSettlement.isPending || credit.isPending || reverseCredit.isPending || refund.isPending || cancel.isPending

  const submit = async (values: { requestId: string; settleUuid?: string; reason?: string }) => {
    if (action === 'next-settlement') await nextSettlement.mutateAsync({ uuid: row.uuid, data: { requestId: values.requestId, settleUuid: values.settleUuid ?? '' } })
    if (action === 'credit') await credit.mutateAsync({ uuid: row.uuid, data: { requestId: values.requestId } })
    if (action === 'reverse-credit') await reverseCredit.mutateAsync({ uuid: row.uuid, data: { requestId: values.requestId, reason: values.reason ?? '' } })
    if (action === 'refund') await refund.mutateAsync({ uuid: row.uuid, data: { requestId: values.requestId, reason: values.reason } })
    if (action === 'cancel') await cancel.mutateAsync({ uuid: row.uuid, data: { requestId: values.requestId, reason: values.reason ?? '' } })
    form.resetFields()
    onClose()
  }

  return (
    <Modal title={`${copy.title} · ${row.adjustmentNo}`} open={Boolean(row)} onCancel={onClose} footer={null} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={submit} initialValues={{ requestId: crypto.randomUUID() }}>
        <Form.Item name="requestId" label="请求号" rules={[{ required: true }]}><Input /></Form.Item>
        {action === 'next-settlement' && <Form.Item name="settleUuid" label="目标结算单 UUID" rules={[{ required: true }]}><Input prefix={<LinkOutlined />} /></Form.Item>}
        {action === 'credit' && <p>将 {row.amount} 元记入客户余款账户。</p>}
        {action === 'refund' && <p>将 {row.amount} 元、{row.weight} kg 提交退款申请。</p>}
        {action !== 'credit' && action !== 'next-settlement' && <Form.Item name="reason" label="操作原因" rules={[{ required: true }]}><Input.TextArea rows={3} /></Form.Item>}
        <Button type="primary" htmlType="submit" icon={action === 'refund' ? <CheckOutlined /> : action === 'credit' ? <CreditCardOutlined /> : <UndoOutlined />} loading={isPending} block>{copy.button}</Button>
      </Form>
    </Modal>
  )
}
