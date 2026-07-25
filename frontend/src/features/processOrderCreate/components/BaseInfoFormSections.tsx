import { Col, DatePicker, Form, Input, InputNumber, Row, Segmented, Select } from 'antd'
import type { OrderSettlementMode } from '../../../types/processOrder'
import type { ReferenceOption } from '../types'

interface NumberOption {
  label: string
  value: number | string
}

export interface BaseInfoFieldOptions {
  customers: ReferenceOption[]
  invoices: NumberOption[]
  priorities: NumberOption[]
  settlements: NumberOption[]
  warehouses: ReferenceOption[]
}

interface Props {
  customerSelected: boolean
  onCustomerChange: (customerUuid: string) => void
  onSettleModeChange: (mode: OrderSettlementMode) => void
  options: BaseInfoFieldOptions
  settleMode?: OrderSettlementMode
  settleType?: number
}

export default function BaseInfoFormSections(props: Props) {
  return (
    <div className="process-order-base__sections">
      <ScheduleFields onCustomerChange={props.onCustomerChange} options={props.options} />
      <SettlementFields {...props} />
      <section className="process-order-base__section">
        <div className="process-order-base__section-title">生产备注</div>
        <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
      </section>
    </div>
  )
}

function ScheduleFields({ onCustomerChange, options }: Pick<Props, 'onCustomerChange' | 'options'>) {
  return (
    <section className="process-order-base__section">
      <div className="process-order-base__section-title">客户与生产安排</div>
      <Row gutter={14}>
        <Col span={6}><Form.Item name="customerUuid" label="客户" rules={[{ required: true, message: '请选择客户' }]}><Select showSearch optionFilterProp="label" options={options.customers} placeholder="选择客户" onChange={onCustomerChange} /></Form.Item></Col>
        <Col span={6}><Form.Item name="warehouseUuid" label="仓库"><Select allowClear options={options.warehouses} placeholder="选择仓库" /></Form.Item></Col>
        <Col span={6}><Form.Item name="teamGroup" label="班组"><Input placeholder="班组" /></Form.Item></Col>
        <Col span={6}><Form.Item name="priority" label="优先级"><Select options={options.priorities} /></Form.Item></Col>
        <Col span={6}><Form.Item name="orderDate" label="制单日期" rules={[{ required: true, message: '请选择日期' }]}><DatePicker /></Form.Item></Col>
        <Col span={6}><Form.Item name="expectFinishDate" label="期望完成"><DatePicker /></Form.Item></Col>
      </Row>
    </section>
  )
}

function SettlementFields({ customerSelected, onSettleModeChange, options, settleMode, settleType }: Props) {
  const isOverride = settleMode === 'OVERRIDE'
  return (
    <section className="process-order-base__section">
      <div className="process-order-base__section-title">结算设置</div>
      <Row gutter={14}>
        <Col span={6}><Form.Item name="isInvoice" label="是否开票"><Select options={options.invoices} /></Form.Item></Col>
        <Col span={6}><Form.Item name="settleMode" label="结算来源" rules={[{ required: true, message: '请选择结算来源' }]}><Segmented block disabled={!customerSelected} options={[{ label: '跟随客户', value: 'INHERIT' }, { label: '本单覆盖', value: 'OVERRIDE' }]} onChange={(value) => onSettleModeChange(value === 'OVERRIDE' ? 'OVERRIDE' : 'INHERIT')} /></Form.Item></Col>
        <Col span={6}><Form.Item name="settleType" label="结算方式" rules={isOverride ? [{ required: true, message: '请选择结算方式' }] : []}><Select disabled={!isOverride} options={options.settlements} placeholder={settleMode ? undefined : '先选择结算来源'} /></Form.Item></Col>
        <Col span={6}><Form.Item name="settleDay" label="月结对账日"><InputNumber min={1} max={31} disabled={!isOverride || settleType !== 2} placeholder={settleType === 2 ? '如 25' : '次结无需填写'} /></Form.Item></Col>
        <Col span={6}><Form.Item name="taxRate" label="税率(%)"><InputNumber min={0} max={100} /></Form.Item></Col>
        {isOverride && <Col span={18}><Form.Item name="settleOverrideReason" label="覆盖原因" rules={[{ required: true, whitespace: true, message: '请填写本单覆盖原因' }, { max: 200, message: '最多200字' }]}><Input placeholder="填写本单与客户默认结算方式不同的原因" /></Form.Item></Col>}
      </Row>
      <Form.Item name="customerVersion" hidden><Input /></Form.Item>
    </section>
  )
}
