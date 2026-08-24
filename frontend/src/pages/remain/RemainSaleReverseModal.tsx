import { CloseOutlined } from '@ant-design/icons'
import { Button, Form, Input, Modal } from 'antd'
import { useReverseRemainSale } from '../../features/remain/hooks/useReverseRemainSale'
import type { RemainSale } from '../../types/remain'

export function RemainSaleReverseModal({ row, onClose }: { row?: RemainSale; onClose: () => void }) {
  const [form] = Form.useForm<{ requestId: string; reason: string }>()
  const reversal = useReverseRemainSale()
  if (!row) return null
  const submit = async (values: { requestId: string; reason: string }) => {
    await reversal.mutateAsync({ uuid: row.uuid, data: values })
    form.resetFields()
    onClose()
  }
  return <Modal title={`作废处理单 · ${row.saleNo}`} open onCancel={onClose} footer={null} destroyOnClose>
    <Form form={form} layout="vertical" onFinish={submit} initialValues={{ requestId: crypto.randomUUID() }}>
      <Form.Item name="requestId" label="请求号" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="reason" label="作废原因" rules={[{ required: true, whitespace: true }]}><Input.TextArea rows={3} /></Form.Item>
      <Button type="primary" danger htmlType="submit" icon={<CloseOutlined />} loading={reversal.isPending} block>确认作废</Button>
    </Form>
  </Modal>
}
