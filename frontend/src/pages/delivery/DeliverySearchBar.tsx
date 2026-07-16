import { Button, DatePicker, Form, Input, Select } from 'antd'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import type { DeliverySearchFormValues } from './deliveryListModel'

interface Props {
  customers: { customerName: string; uuid: string }[]
  form: ReturnType<typeof Form.useForm<DeliverySearchFormValues>>[0]
  loadingCustomers: boolean
  onReset: () => void
  onSearch: (values: DeliverySearchFormValues) => void
}

export default function DeliverySearchBar(props: Props) {
  return (
    <Form form={props.form} layout="vertical" className="document-searchbar" onFinish={props.onSearch}>
      <div className="document-searchbar__grid">
        <Form.Item name="keyword" label="出库单号/客户">
          <Input allowClear placeholder="输入单号、客户或备注" />
        </Form.Item>
        <Form.Item name="customerUuid" label="客户">
          <Select allowClear showSearch loading={props.loadingCustomers} placeholder="全部客户"
            options={props.customers.map(customerOption)} optionFilterProp="label" />
        </Form.Item>
        <Form.Item name="dateRange" label="出库日期">
          <DatePicker.RangePicker placeholder={['开始日期', '结束日期']} style={{ width: '100%' }} />
        </Form.Item>
        <div className="document-searchbar__actions">
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
          <Button icon={<ReloadOutlined />} onClick={props.onReset}>重置</Button>
        </div>
      </div>
    </Form>
  )
}

function customerOption(item: { customerName: string; uuid: string }) {
  return { label: item.customerName, value: item.uuid }
}
