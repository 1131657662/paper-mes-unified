import { CheckOutlined, CloseOutlined, DollarOutlined } from '@ant-design/icons'
import { Button, Form, Input, Modal } from 'antd'
import { useApproveRemainRefund } from '../../features/remain/hooks/useApproveRemainRefund'
import { useCancelRemainRefund } from '../../features/remain/hooks/useCancelRemainRefund'
import { usePayRemainRefund } from '../../features/remain/hooks/usePayRemainRefund'
import type { RemainRefund } from '../../types/remain'

export type RemainRefundAction = 'approve' | 'pay' | 'cancel'

interface Props { row?: RemainRefund; action?: RemainRefundAction; onClose: () => void }

const COPY: Record<RemainRefundAction, { title: string; button: string }> = {
  approve: { title: '审批退款', button: '确认审批' },
  pay: { title: '支付退款', button: '确认支付' },
  cancel: { title: '取消退款', button: '确认取消' },
}

export function RemainRefundActionModal({ row, action, onClose }: Props) {
  const [form] = Form.useForm()
  const approve = useApproveRemainRefund()
  const pay = usePayRemainRefund()
  const cancel = useCancelRemainRefund()
  if (!row || !action) return null
  const copy = COPY[action]
  const isPending = approve.isPending || pay.isPending || cancel.isPending
  const submit = async (values: { requestId: string; reason?: string; paymentReference?: string }) => {
    const payload = { uuid: row.uuid, data: values }
    if (action === 'approve') await approve.mutateAsync(payload)
    if (action === 'pay') await pay.mutateAsync(payload)
    if (action === 'cancel') await cancel.mutateAsync(payload)
    form.resetFields()
    onClose()
  }
  return (
    <Modal title={`${copy.title} · ${row.refundNo}`} open={Boolean(row)} onCancel={onClose} footer={null} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={submit} initialValues={{ requestId: crypto.randomUUID() }}>
        <Form.Item name="requestId" label="请求号" rules={[{ required: true }]}><Input /></Form.Item>
        {action === 'pay' && <Form.Item name="paymentReference" label="支付凭证" rules={[{ required: true }]}><Input prefix={<DollarOutlined />} /></Form.Item>}
        <Form.Item name="reason" label="处理说明" rules={[{ required: true, whitespace: true, message: '请输入处理说明' }]}><Input.TextArea rows={3} /></Form.Item>
        <Button type="primary" danger={action === 'cancel'} htmlType="submit" icon={action === 'cancel' ? <CloseOutlined /> : <CheckOutlined />} loading={isPending} block>{copy.button}</Button>
      </Form>
    </Modal>
  )
}
