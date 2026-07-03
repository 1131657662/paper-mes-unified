import { Form, Input, Modal } from 'antd'
import type { OriginalRollRemarkDTO, RollProductionVO } from '../../../types/processOrder'

interface Props {
  loading?: boolean
  open: boolean
  roll?: RollProductionVO
  onCancel: () => void
  onSubmit: (values: OriginalRollRemarkDTO) => Promise<void>
}

export default function RollRemarkModal({ loading, open, roll, onCancel, onSubmit }: Props) {
  const [form] = Form.useForm<OriginalRollRemarkDTO>()

  return (
    <Modal
      destroyOnClose
      title="编辑原纸备注"
      open={open}
      confirmLoading={loading}
      onCancel={onCancel}
      onOk={() => form.submit()}
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          batchNo: roll?.batchNo,
          damageDesc: roll?.damageDesc,
          remark: roll?.remark,
        }}
        onFinish={onSubmit}
      >
        <Form.Item name="batchNo" label="批次" rules={[{ max: 100, message: '批次不能超过100个字符' }]}>
          <Input placeholder="客户批次或来料批次" />
        </Form.Item>
        <Form.Item name="damageDesc" label="损伤说明" rules={[{ max: 255, message: '损伤说明不能超过255个字符' }]}>
          <Input.TextArea rows={3} placeholder="记录来料破损、压痕、水渍等情况" />
        </Form.Item>
        <Form.Item name="remark" label="明细备注" rules={[{ max: 255, message: '备注不能超过255个字符' }]}>
          <Input.TextArea rows={3} placeholder="填写该母卷的现场说明" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
