import { DollarOutlined } from '@ant-design/icons'
import { Button, Form, Input, InputNumber, Modal } from 'antd'
import { useConfirmRemainPrice } from '../../features/remain/hooks/useConfirmRemainPrice'
import type { RemainRegistration } from '../../types/remain'

interface Props { row?: RemainRegistration; onClose: () => void }

export function RemainPriceModal({ row, onClose }: Props) {
  const [form] = Form.useForm()
  const { mutateAsync: confirmPrice, isPending } = useConfirmRemainPrice()
  if (!row) return null
  const submit = async (values: { requestId: string; pricingBasis: string; totalAmount: number }) => {
    await confirmPrice({ uuid: row.uuid, data: values })
    onClose()
  }
  return (
    <Modal title={`确认价格 · ${row.registrationNo}`} open={Boolean(row)} onCancel={onClose} footer={null} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={submit} initialValues={{ requestId: crypto.randomUUID(), pricingBasis: 'SYSTEM_WEIGHT', totalAmount: row.totalAmount || undefined }}>
        <Form.Item name="requestId" label="请求号" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="pricingBasis" label="计价依据" rules={[{ required: true }]}><Input placeholder="SYSTEM_WEIGHT / ACTUAL_WEIGHT / TOTAL_AMOUNT" /></Form.Item>
        <Form.Item name="totalAmount" label="确认金额（元）" rules={[{ required: true, type: 'number', min: 0 }]}><InputNumber precision={0} min={0} style={{ width: '100%' }} /></Form.Item>
        <Button type="primary" htmlType="submit" icon={<DollarOutlined />} loading={isPending} block>确认价格</Button>
      </Form>
    </Modal>
  )
}
