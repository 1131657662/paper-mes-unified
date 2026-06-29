import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import { Button, DatePicker, Form, Input, Select } from 'antd'
import type { Dayjs } from 'dayjs'
import { ORDER_STATUS } from '../../constants/processOrder'

const { RangePicker } = DatePicker

export interface ProcessOrderSearchFilters {
  keyword?: string
  customerUuid?: string
  orderStatus?: number
  dateFrom?: string
  dateTo?: string
}

interface SearchFormValues {
  keyword?: string
  customerUuid?: string
  orderStatus?: number
  dateRange?: [Dayjs, Dayjs] | null
}

interface Props {
  customerEnum: Record<string, { text: string }>
  onSearch: (filters: ProcessOrderSearchFilters) => void
}

export default function ProcessOrderSearchBar({ customerEnum, onSearch }: Props) {
  const [form] = Form.useForm<SearchFormValues>()

  const handleFinish = (values: SearchFormValues) => {
    onSearch({
      keyword: normalizeText(values.keyword),
      customerUuid: values.customerUuid,
      orderStatus: values.orderStatus,
      dateFrom: values.dateRange?.[0]?.format('YYYY-MM-DD'),
      dateTo: values.dateRange?.[1]?.format('YYYY-MM-DD'),
    })
  }

  const handleReset = () => {
    form.resetFields()
    onSearch({})
  }

  return (
    <Form form={form} layout="vertical" className="process-order-searchbar" onFinish={handleFinish}>
      <div className="process-order-searchbar__grid">
        <Form.Item name="keyword" label="加工单号/客户">
          <Input allowClear placeholder="输入单号、客户或备注" />
        </Form.Item>
        <Form.Item name="customerUuid" label="客户">
          <Select allowClear showSearch placeholder="全部客户" options={customerOptions(customerEnum)} optionFilterProp="label" />
        </Form.Item>
        <Form.Item name="dateRange" label="制单日期">
          <RangePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="orderStatus" label="状态">
          <Select allowClear placeholder="全部状态" options={statusOptions()} />
        </Form.Item>
        <div className="process-order-searchbar__actions">
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
        </div>
      </div>
    </Form>
  )
}

function normalizeText(value?: string) {
  const text = value?.trim()
  return text || undefined
}

function customerOptions(customerEnum: Record<string, { text: string }>) {
  return Object.entries(customerEnum).map(([value, item]) => ({ value, label: item.text }))
}

function statusOptions() {
  return Object.entries(ORDER_STATUS).map(([value, item]) => ({ value: Number(value), label: item.text }))
}
