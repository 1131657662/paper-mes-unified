import { Button, DatePicker, Form, Input, Select } from 'antd'
import type { FormInstance } from 'antd'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import { SETTLE_TYPE } from '../../constants/settle'

export interface SettleSearchFormValues {
  customerUuid?: string
  dateRange?: [Dayjs, Dayjs] | null
  keyword?: string
  settleType?: number
}

interface Props {
  customers: { customerName: string; uuid: string }[]
  form: FormInstance<SettleSearchFormValues>
  loadingCustomers: boolean
  onReset: () => void
  onSearch: (values: SettleSearchFormValues) => void
}

export default function SettleSearchBar({ customers, form, loadingCustomers, onReset, onSearch }: Props) {
  return (
    <Form form={form} layout="vertical" className="document-searchbar" onFinish={onSearch}>
      <div className="document-searchbar__grid">
        <Form.Item name="keyword" label="结算单号/客户">
          <Input allowClear placeholder="输入单号、客户或备注" />
        </Form.Item>
        <Form.Item name="customerUuid" label="客户">
          <Select
            allowClear
            showSearch
            loading={loadingCustomers}
            placeholder="全部客户"
            options={customers.map((item) => ({ label: item.customerName, value: item.uuid }))}
            optionFilterProp="label"
          />
        </Form.Item>
        <Form.Item name="dateRange" label="结算日期">
          <DatePicker.RangePicker placeholder={['开始日期', '结束日期']} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="settleType" label="类型">
          <Select allowClear placeholder="全部类型" options={typeOptions()} />
        </Form.Item>
        <div className="document-searchbar__actions">
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
          <Button icon={<ReloadOutlined />} onClick={onReset}>重置</Button>
        </div>
      </div>
    </Form>
  )
}

function typeOptions() {
  return Object.entries(SETTLE_TYPE).map(([value, label]) => ({ label, value: Number(value) }))
}
