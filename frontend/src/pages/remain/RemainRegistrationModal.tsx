import { PlusOutlined, SaveOutlined } from '@ant-design/icons'
import { Button, DatePicker, Form, Input, InputNumber, Modal, Space } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useCreateRemainRegistration } from '../../features/remain/hooks/useCreateRemainRegistration'
import type { CreateRemainRegistrationRequest } from '../../types/remain'

interface FormValues extends Omit<CreateRemainRegistrationRequest, 'confirmationAt' | 'lines'> {
  confirmationAt: Dayjs
  lines: CreateRemainRegistrationRequest['lines']
}

export function RemainRegistrationModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [form] = Form.useForm<FormValues>()
  const { mutateAsync: createRegistration, isPending: isCreating } = useCreateRemainRegistration()

  const submit = async (values: FormValues) => {
    await createRegistration({ ...values, confirmationAt: values.confirmationAt.format('YYYY-MM-DDTHH:mm:ss') })
    form.resetFields()
    onClose()
  }

  return (
    <Modal title="新建余料登记" open={open} onCancel={onClose} footer={null} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={submit} initialValues={{ confirmationAt: dayjs(), lines: [{}] }}>
        <Form.Item name="requestId" label="请求号" rules={[{ required: true, message: '请输入请求号' }]}>
          <Input placeholder="例如 REMAIN-20260820-001" />
        </Form.Item>
        <Form.Item name="orderUuid" label="来源加工单 UUID" rules={[{ required: true, message: '请输入来源加工单 UUID' }]}>
          <Input />
        </Form.Item>
        <Space.Compact block>
          <Form.Item name="confirmationName" label="客户确认人" rules={[{ required: true }]} style={{ width: '50%' }}>
            <Input />
          </Form.Item>
          <Form.Item name="confirmationChannel" label="确认渠道" rules={[{ required: true }]} style={{ width: '50%' }}>
            <Input placeholder="电话/微信/纸面" />
          </Form.Item>
        </Space.Compact>
        <Form.Item name="confirmationAt" label="确认时间" rules={[{ required: true }]}>
          <DatePicker showTime style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="confirmationEvidence" label="凭证或核验说明" rules={[{ required: true }]}>
          <Input.TextArea rows={2} />
        </Form.Item>
        <Form.List name="lines">
          {(fields, { add, remove }) => (
            <>
              {fields.map((field) => (
                <Space key={field.key} align="baseline" style={{ width: '100%' }}>
                  <Form.Item {...field} name={[field.name, 'sourceFinishRollUuid']} rules={[{ required: true, message: '请输入来源余卷 UUID' }]}>
                    <Input placeholder="来源余卷 UUID" />
                  </Form.Item>
                  <Form.Item {...field} name={[field.name, 'transferredSystemWeight']} rules={[{ required: true, type: 'number', min: 0.001 }]}>
                    <InputNumber aria-label="转入系统重量" min={0.001} precision={3} addonAfter="kg" />
                  </Form.Item>
                  {fields.length > 1 && <Button type="text" danger onClick={() => remove(field.name)}>删除</Button>}
                </Space>
              ))}
              <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add()}>增加来源余卷</Button>
            </>
          )}
        </Form.List>
        <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
        <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={isCreating} block>保存登记</Button>
      </Form>
    </Modal>
  )
}
