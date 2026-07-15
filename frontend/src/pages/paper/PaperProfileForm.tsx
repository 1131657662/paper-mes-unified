import { Form, Input, InputNumber } from 'antd'
import type { FormInstance } from 'antd'
import AutoCodeInput from '../../components/biz/AutoCodeInput'
import type { PaperSaveDTO } from '../../types/paper'

interface Props {
  editing: boolean
  form: FormInstance<PaperSaveDTO>
  onFinish?: (values: PaperSaveDTO) => void
  onValuesChange?: () => void
}

export default function PaperProfileForm({ editing, form, onFinish, onValuesChange }: Props) {
  return (
    <Form
      className="mes-modal-form paper-profile-form"
      form={form}
      layout="vertical"
      onFinish={onFinish}
      onValuesChange={onValuesChange}
    >
      <section className="paper-profile-form__section">
        <h3>基础信息</h3>
        <div className="mes-form-grid">
          <Form.Item name="paperCode" label="纸张编码">
            <AutoCodeInput editing={editing} />
          </Form.Item>
          <Form.Item
            name="paperName"
            label="纸张品名"
            rules={[{ required: true, message: '请输入纸张品名' }]}
          >
            <Input placeholder="请输入纸张品名" />
          </Form.Item>
          <Form.Item name="gramWeight" label="常用克重(g/㎡)">
            <InputNumber min={0} step={1} precision={0} placeholder="如 250" />
          </Form.Item>
          <Form.Item name="paperType" label="纸张类型">
            <Input placeholder="如 白卡 / 牛皮 / 瓦楞" />
          </Form.Item>
        </div>
      </section>

      <section className="paper-profile-form__section">
        <h3>备注</h3>
        <Form.Item name="remark" label="备注说明">
          <Input.TextArea rows={4} placeholder="记录该纸张常见用途、特殊要求或识别说明" />
        </Form.Item>
      </section>
    </Form>
  )
}
