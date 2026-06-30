import { Form, Input, Select } from 'antd'
import type { FormInstance } from 'antd'
import type { MachineSaveDTO } from '../../types/machine'

interface Props {
  editing: boolean
  form: FormInstance<MachineSaveDTO>
  onFinish?: (values: MachineSaveDTO) => void
}

export const machineFormDefaults: Partial<MachineSaveDTO> = {
  status: 1,
}

export default function MachineProfileForm({ editing, form, onFinish }: Props) {
  return (
    <Form
      className="mes-modal-form machine-profile-form"
      form={form}
      initialValues={machineFormDefaults}
      layout="vertical"
      onFinish={onFinish}
    >
      <section className="machine-profile-form__section">
        <h3>基础信息</h3>
        <div className="mes-form-grid">
          <Form.Item name="machineCode" label="机台编码">
            <Input placeholder={editing ? undefined : '保存后系统自动生成'} title="系统自动生成，不支持手工修改" disabled readOnly />
          </Form.Item>
          <Form.Item
            name="machineName"
            label="机台名称"
            rules={[{ required: true, message: '请输入机台名称' }]}
          >
            <Input placeholder="请输入机台名称" />
          </Form.Item>
          <Form.Item name="machineType" label="机台类型">
            <Select allowClear placeholder="请选择" options={machineTypeOptions} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={statusOptions} />
          </Form.Item>
        </div>
      </section>

      <section className="machine-profile-form__section">
        <h3>备注</h3>
        <Form.Item name="remark" label="备注说明">
          <Input.TextArea rows={4} placeholder="记录机台能力、使用限制或维护说明" />
        </Form.Item>
      </section>
    </Form>
  )
}

const machineTypeOptions = [
  { value: 1, label: '锯纸' },
  { value: 2, label: '复卷' },
  { value: 3, label: '通用' },
]

const statusOptions = [
  { value: 1, label: '启用' },
  { value: 2, label: '停用' },
]
