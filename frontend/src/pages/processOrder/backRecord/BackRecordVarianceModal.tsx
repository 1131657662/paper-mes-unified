import { Alert, Form, Input, Modal } from 'antd'
import type { FormInstance } from 'antd'
import type { BackRecordVarianceConfirmation } from './backRecordUtils'

interface Props {
  open: boolean
  form: FormInstance<BackRecordVarianceConfirmation>
  onCancel: () => void
  onSubmit: () => void
}

export default function BackRecordVarianceModal({ open, form, onCancel, onSubmit }: Props) {
  return (
    <Modal
      title="重量偏差确认"
      open={open}
      onCancel={onCancel}
      onOk={onSubmit}
      okText="确认原因并继续"
      cancelText="取消"
      destroyOnHidden
      forceRender
    >
      <Alert
        showIcon
        type="warning"
        message="重量偏差处于警告范围，请核对现场数据并说明原因。"
      />
      <Form form={form} layout="vertical">
        <Form.Item
          name="varianceReason"
          label="偏差原因"
          rules={[{ required: true, message: '偏差原因必填' }]}
        >
          <Input.TextArea rows={3} maxLength={500} showCount placeholder="请填写复称差异或现场损耗原因" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
