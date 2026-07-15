import { Form, Input, InputNumber, Select } from 'antd'
import type { FormInstance } from 'antd'
import AutoCodeInput from '../../components/biz/AutoCodeInput'
import { DICT_TYPES, invoiceFallbackOptions, settleFallbackOptions } from '../../features/systemConfig/configFallbacks'
import { useNumberDictOptions } from '../../features/systemConfig/hooks/useRuntimeDictOptions'
import type { CustomerSaveDTO } from '../../types/customer'

interface Props {
  editing: boolean
  form: FormInstance<CustomerSaveDTO>
  onFinish?: (values: CustomerSaveDTO) => void
  onValuesChange?: () => void
}

const customerFormDefaults: Partial<CustomerSaveDTO> = {
  defaultInvoice: 2,
  priceIncludeTax: 2,
  settleType: 2,
  taxRate: 13,
}

export default function CustomerProfileForm({ editing, form, onFinish, onValuesChange }: Props) {
  const { options: settleOptions } = useNumberDictOptions(DICT_TYPES.settleType, settleFallbackOptions)
  const { options: invoiceOptions } = useNumberDictOptions(DICT_TYPES.invoiceType, invoiceFallbackOptions)

  return (
    <Form
      className="mes-modal-form customer-profile-form"
      form={form}
      layout="vertical"
      initialValues={customerFormDefaults}
      onFinish={onFinish}
      onValuesChange={onValuesChange}
    >
      <section className="customer-profile-form__section">
        <h3>基础信息</h3>
        <div className="mes-form-grid">
          <Form.Item
            name="customerCode"
            label="客户编码"
          >
            <AutoCodeInput editing={editing} />
          </Form.Item>
          <Form.Item
            name="customerName"
            label="客户名称"
            rules={[{ required: true, message: '请输入客户名称' }]}
          >
            <Input placeholder="请输入客户名称" />
          </Form.Item>
          <Form.Item name="contact" label="联系人">
            <Input placeholder="联系人" />
          </Form.Item>
          <Form.Item name="phone" label="电话">
            <Input placeholder="电话" />
          </Form.Item>
          <Form.Item name="customerLevel" label="客户等级">
            <Select allowClear placeholder="请选择" options={customerLevelOptions} />
          </Form.Item>
          <Form.Item name="exportTemplate" label="模板标识">
            <Input placeholder="如 default" />
          </Form.Item>
        </div>
      </section>

      <section className="customer-profile-form__section">
        <h3>默认结算与计费</h3>
        <div className="mes-form-grid">
          <Form.Item name="settleType" label="结算方式">
            <Select placeholder="请选择" options={settleOptions} />
          </Form.Item>
          <Form.Item name="settleDay" label="月结日">
            <InputNumber min={1} max={31} placeholder="如 25" />
          </Form.Item>
          <Form.Item name="sawPrice" label="默认锯纸单价(元/刀)">
            <InputNumber min={0} precision={2} placeholder="如 1.50" />
          </Form.Item>
          <Form.Item name="rewindPrice" label="默认复卷单价(元/吨)">
            <InputNumber min={0} precision={2} placeholder="如 200.00" />
          </Form.Item>
          <Form.Item name="defaultInvoice" label="默认开票">
            <Select placeholder="请选择" options={invoiceOptions} />
          </Form.Item>
          <Form.Item name="priceIncludeTax" label="价格口径">
            <Select placeholder="请选择" options={priceTaxOptions} />
          </Form.Item>
          <Form.Item name="taxRate" label="税率(%)">
            <InputNumber min={0} max={100} precision={2} placeholder="如 13" />
          </Form.Item>
        </div>
      </section>

      <section className="customer-profile-form__section">
        <h3>开票与物流</h3>
        <div className="mes-form-grid">
          <Form.Item name="taxNo" label="税号">
            <Input placeholder="纳税人识别号" />
          </Form.Item>
          <Form.Item name="bankAccount" label="开户行账号">
            <Input placeholder="开户行及账号" />
          </Form.Item>
          <Form.Item className="mes-form-grid__full" name="invoiceAddress" label="开票地址电话">
            <Input placeholder="开票地址、电话" />
          </Form.Item>
          <Form.Item className="mes-form-grid__full" name="deliveryAddress" label="默认送货地址">
            <Input placeholder="送货地址" />
          </Form.Item>
          <Form.Item className="mes-form-grid__full" name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="备注" />
          </Form.Item>
        </div>
      </section>
    </Form>
  )
}

const customerLevelOptions = [
  { value: 1, label: 'A 重点客户' },
  { value: 2, label: 'B 常规客户' },
  { value: 3, label: 'C 临时客户' },
]

const priceTaxOptions = [
  { value: 1, label: '含税价' },
  { value: 2, label: '未税价' },
]
