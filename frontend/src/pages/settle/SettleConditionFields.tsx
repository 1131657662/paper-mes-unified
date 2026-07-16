import { DatePicker, Form, Input, Radio, Select } from 'antd'
import type { Dayjs } from 'dayjs'

export interface SettleCreateForm {
  customerUuid?: string
  createMode: 'selected' | 'month'
  isInvoice: number
  period?: [Dayjs, Dayjs] | null
  remark?: string
  settleDate: Dayjs
}

interface Props {
  customers: { customerName: string; uuid: string }[]
  invoiceOptions: { label: string; value: number }[]
  isMonthMode: boolean
  loading: boolean
}

export default function SettleConditionFields({ customers, invoiceOptions, isMonthMode, loading }: Props) {
  const requiredCustomerRules = isMonthMode ? [{ required: true, message: '请选择客户' }] : undefined
  const requiredPeriodRules = isMonthMode ? [{ required: true, message: '请选择归属日期范围' }] : undefined

  return (
    <div className="document-module-grid settle-create-page__form">
      <Form.Item name="createMode" label="创建方式">
        <Radio.Group>
          <Radio.Button value="selected">勾选加工单</Radio.Button>
          <Radio.Button value="month">按账期自动圈单</Radio.Button>
        </Radio.Group>
      </Form.Item>
      <Form.Item name="customerUuid" label="客户" required={isMonthMode} rules={requiredCustomerRules}>
        <Select allowClear showSearch loading={loading} placeholder={isMonthMode ? '请选择客户' : '全部客户'}
          options={customers.map((item) => ({ label: item.customerName, value: item.uuid }))} optionFilterProp="label" />
      </Form.Item>
      <Form.Item name="period" label="归属日期范围" required={isMonthMode} rules={requiredPeriodRules}>
        <DatePicker.RangePicker />
      </Form.Item>
      <Form.Item name="settleDate" label="结算日期" rules={[{ required: true, message: '请选择结算日期' }]}>
        <DatePicker />
      </Form.Item>
      <Form.Item name="isInvoice" label="是否开票">
        <Radio.Group>
          <Radio.Button value={0}>沿用原单/客户</Radio.Button>
          {invoiceOptions.map((item) => <Radio.Button key={item.value} value={item.value}>{item.label}</Radio.Button>)}
        </Radio.Group>
      </Form.Item>
      <Form.Item name="remark" label="备注"><Input maxLength={255} placeholder="结算备注" /></Form.Item>
    </div>
  )
}
