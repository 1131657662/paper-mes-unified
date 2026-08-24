import { SaveOutlined } from '@ant-design/icons'
import { Button, DatePicker, Form, Input, InputNumber, Modal, Select, Space } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useCreateRemainSale } from '../../features/remain/hooks/useCreateRemainSale'
import type { RemainInventory, RemainSaleCreateRequest } from '../../types/remain'

interface FormValues extends Omit<RemainSaleCreateRequest, 'processDate' | 'lines'> {
  processDate: Dayjs
  lines: RemainSaleCreateRequest['lines']
}

interface Props { rows: RemainInventory[]; onClose: () => void }

const PRICING_OPTIONS = [
  { value: 'SYSTEM_WEIGHT_UNIT_PRICE', label: '按系统重量 × 单价' },
  { value: 'ACTUAL_WEIGHT_UNIT_PRICE', label: '按整车过磅重量 × 单价' },
  { value: 'TOTAL_AMOUNT', label: '协商总价' },
]
const DEFAULT_PRICING_MODE = 'SYSTEM_WEIGHT_UNIT_PRICE'

export function RemainSaleModal({ rows, onClose }: Props) {
  const [form] = Form.useForm<FormValues>()
  const sale = useCreateRemainSale()
  if (rows.length === 0) return null

  const submit = async (values: FormValues) => {
    await sale.mutateAsync({
      ...values,
      processDate: values.processDate.format('YYYY-MM-DDTHH:mm:ss'),
      lines: values.lines.map((line) => ({ lotUuid: line.lotUuid, systemWeight: line.systemWeight })),
    })
    form.resetFields()
    onClose()
  }

  return (
    <Modal title="出售或处理我方余料" open onCancel={onClose} footer={null} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={submit} initialValues={{ processDate: dayjs(), pricingMode: DEFAULT_PRICING_MODE, receivedAmount: 0, lines: rows.map((row) => ({ lotUuid: row.lotUuid, systemWeight: row.currentWeight })) }}>
        <Form.Item name="requestId" label="请求号" rules={[{ required: true, message: '请输入请求号' }]}><Input /></Form.Item>
        <Space.Compact block>
          <Form.Item name="processDate" label="处理日期" rules={[{ required: true }]} style={{ width: '50%' }}><DatePicker showTime style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="warehouseUuid" label="仓库 UUID" style={{ width: '50%' }}><Input /></Form.Item>
        </Space.Compact>
        <Form.Item name="pricingMode" label="计价方式" rules={[{ required: true }]}><Select options={PRICING_OPTIONS} /></Form.Item>
        <Space.Compact block>
          <Form.Item name="unitPrice" label="单价" style={{ width: '50%' }}><InputNumber min={0} precision={4} style={{ width: '100%' }} addonAfter="元/kg" /></Form.Item>
          <Form.Item name="actualWeight" label="整车实际重量" style={{ width: '50%' }}><InputNumber min={0} precision={3} style={{ width: '100%' }} addonAfter="kg" /></Form.Item>
        </Space.Compact>
        <Form.Item name="totalAmount" label="协商总价"><InputNumber min={0} precision={0} style={{ width: '100%' }} addonAfter="元" /></Form.Item>
        <Form.Item name="receivedAmount" label="最终实收金额" rules={[{ required: true, type: 'number', min: 0 }, { validator: (_, value) => Number.isInteger(value) ? Promise.resolve() : Promise.reject(new Error('金额必须为整数元')) }]}><InputNumber min={0} precision={0} style={{ width: '100%' }} addonAfter="元" /></Form.Item>
        <Form.List name="lines">
          {(fields) => <>
            {fields.map((field, index) => <Space key={field.key} align="baseline" style={{ width: '100%' }}>
              <Form.Item {...field} name={[field.name, 'lotUuid']} hidden><Input /></Form.Item>
              <Input aria-label={`库存批次 ${index + 1}`} value={rows[index]?.lotUuid} readOnly style={{ width: 220 }} />
              <Form.Item {...field} name={[field.name, 'systemWeight']} rules={[{ required: true, type: 'number', min: 0.001 }]}><InputNumber aria-label={`库存批次 ${index + 1} 系统重量`} min={0.001} precision={3} addonAfter="kg" /></Form.Item>
            </Space>)}
          </>}
        </Form.List>
        <Space.Compact block>
          <Form.Item name="buyerName" label="买方/处理对象" style={{ width: '50%' }}><Input /></Form.Item>
          <Form.Item name="vehicleNo" label="车号" style={{ width: '50%' }}><Input /></Form.Item>
        </Space.Compact>
        <Form.Item name="weighingTicketNo" label="磅单号"><Input /></Form.Item>
        <Form.Item name="weighingEvidence" label="过磅凭证说明"><Input.TextArea rows={2} /></Form.Item>
        <Form.Item name="reason" label="处理原因"><Input.TextArea rows={2} /></Form.Item>
        <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={sale.isPending} block>保存处理单</Button>
      </Form>
    </Modal>
  )
}
