import { CalendarOutlined } from '@ant-design/icons'
import { Button, DatePicker, Dropdown, Form, Input, Radio, Select, Space, Tooltip } from 'antd'
import type { MenuProps } from 'antd'
import type { Dayjs } from 'dayjs'
import { periodFor } from '../../features/report/utils/reportPeriod'

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
        <PeriodRangeField />
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

const periodPresetItems: MenuProps['items'] = [
  { key: 'previousMonth', label: '上月' },
]

interface PeriodRangeFieldProps {
  value?: [Dayjs, Dayjs] | null
  onChange?: (value: [Dayjs, Dayjs] | null) => void
}

function PeriodRangeField({ value, onChange }: PeriodRangeFieldProps) {
  const applyPreviousMonth = () => onChange?.(periodFor('previousMonth'))
  return (
    <Space.Compact block>
      <Tooltip title="快捷选择归属日期范围">
        <Dropdown menu={{ items: periodPresetItems, onClick: applyPreviousMonth }}
          trigger={['click']} placement="bottomLeft">
          <Button aria-label="快捷选择归属日期范围" icon={<CalendarOutlined />} />
        </Dropdown>
      </Tooltip>
      <DatePicker.RangePicker value={value}
        onChange={(next) => onChange?.(next?.[0] && next[1] ? [next[0], next[1]] : null)} />
    </Space.Compact>
  )
}
