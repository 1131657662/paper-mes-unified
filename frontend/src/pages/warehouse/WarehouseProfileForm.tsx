import { Form, Input, Select } from 'antd'
import type { FormInstance } from 'antd'
import AutoCodeInput from '../../components/biz/AutoCodeInput'
import type { WarehouseSaveDTO } from '../../types/warehouse'

interface Props {
  editing: boolean
  form: FormInstance<WarehouseSaveDTO>
  onFinish?: (values: WarehouseSaveDTO) => void
  onValuesChange?: () => void
}

const warehouseFormDefaults: Partial<WarehouseSaveDTO> = {
  status: 1,
}

export default function WarehouseProfileForm({ editing, form, onFinish, onValuesChange }: Props) {
  return (
    <Form
      className="mes-modal-form warehouse-profile-form"
      form={form}
      initialValues={warehouseFormDefaults}
      layout="vertical"
      onFinish={onFinish}
      onValuesChange={onValuesChange}
    >
      <section className="warehouse-profile-form__section">
        <h3>基础信息</h3>
        <div className="mes-form-grid">
          <Form.Item name="warehouseCode" label="仓库编码">
            <AutoCodeInput editing={editing} />
          </Form.Item>
          <Form.Item
            name="warehouseName"
            label="仓库名称"
            rules={[{ required: true, message: '请输入仓库名称' }]}
          >
            <Input placeholder="请输入仓库名称" />
          </Form.Item>
          <Form.Item name="location" label="库位/地址">
            <Input placeholder="如 A区1号 / 东仓" />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={statusOptions} />
          </Form.Item>
        </div>
      </section>

      <section className="warehouse-profile-form__section">
        <h3>备注</h3>
        <Form.Item name="remark" label="备注说明">
          <Input.TextArea rows={4} placeholder="记录仓库用途、库位范围或管理说明" />
        </Form.Item>
      </section>
    </Form>
  )
}

const statusOptions = [
  { value: 1, label: '启用' },
  { value: 2, label: '停用' },
]
