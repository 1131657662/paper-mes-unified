import { RollbackOutlined } from '@ant-design/icons'
import { Button, Form, Input, InputNumber, Modal, Space } from 'antd'
import { useRollbackRemain } from '../../features/remain/hooks/useRollbackRemain'
import type { RemainRegistration } from '../../types/remain'

interface Props { row?: RemainRegistration; onClose: () => void }

export function RemainRollbackModal({ row, onClose }: Props) {
  const [form] = Form.useForm()
  const { mutateAsync: rollback, isPending } = useRollbackRemain()
  if (!row) return null
  const submit = async (values: { requestId: string; reason: string; lines: { registrationLineUuid: string; rollbackWeight: number }[] }) => {
    await rollback({ uuid: row.uuid, data: values })
    onClose()
  }
  return (
    <Modal title={`回滚登记 · ${row.registrationNo}`} open={Boolean(row)} onCancel={onClose} footer={null} destroyOnClose>
      <Form form={form} layout="vertical" onFinish={submit} initialValues={{ requestId: crypto.randomUUID(), lines: row.lines?.map((line) => ({ registrationLineUuid: line.uuid, rollbackWeight: line.currentOwnWeight ?? 0 })) }}>
        <Form.Item name="requestId" label="请求号" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.List name="lines">
          {(fields) => fields.map((field, index) => {
            const line = row.lines?.[index]
            return <Space key={field.key} align="baseline" style={{ width: '100%' }}>
              <Form.Item {...field} name={[field.name, 'registrationLineUuid']} hidden><Input /></Form.Item>
              <span>{line?.sourceFinishRollUuid ?? '来源余卷'}</span>
              <Form.Item {...field} name={[field.name, 'rollbackWeight']} rules={[{ required: true, type: 'number', min: 0 }]}><InputNumber aria-label="回滚重量" precision={3} min={0} addonAfter="kg" /></Form.Item>
            </Space>
          })}
        </Form.List>
        <Form.Item name="reason" label="回滚原因" rules={[{ required: true }]}><Input.TextArea rows={3} /></Form.Item>
        <Button type="primary" danger htmlType="submit" icon={<RollbackOutlined />} loading={isPending} block>确认回滚</Button>
      </Form>
    </Modal>
  )
}
