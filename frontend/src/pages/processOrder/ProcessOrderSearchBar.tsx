import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import { Button, DatePicker, Form, Input, Select } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import type { ProcessOrderSearchFilters } from './processOrderListUrlState'

const { RangePicker } = DatePicker

interface SearchFormValues {
  keyword?: string
  customerUuid?: string
  dateRange?: [Dayjs, Dayjs] | null
}

interface Props {
  customerEnum: Record<string, { text: string }>
  filters: ProcessOrderSearchFilters
  onSearch: (filters: ProcessOrderSearchFilters) => void
}

export default function ProcessOrderSearchBar({ customerEnum, filters, onSearch }: Props) {
  const [form] = Form.useForm<SearchFormValues>()

  const handleFinish = (values: SearchFormValues) => {
    onSearch({
      keyword: normalizeText(values.keyword),
      customerUuid: values.customerUuid,
      dateFrom: values.dateRange?.[0]?.format('YYYY-MM-DD'),
      dateTo: values.dateRange?.[1]?.format('YYYY-MM-DD'),
    })
  }

  const handleReset = () => {
    form.resetFields()
    onSearch({})
  }

  return (
    <Form
      form={form}
      layout="vertical"
      className="process-order-searchbar"
      initialValues={initialValues(filters)}
      onFinish={handleFinish}
    >
      <div className="process-order-searchbar__grid">
        <Form.Item name="keyword" label="关键字">
          <Input allowClear placeholder="输入单号、客户或备注" />
        </Form.Item>
        <Form.Item name="customerUuid" label="客户">
          <Select allowClear showSearch placeholder="全部客户" options={customerOptions(customerEnum)} optionFilterProp="label" />
        </Form.Item>
        <Form.Item name="dateRange" label="制单日期">
          <RangePicker placeholder={['开始日期', '结束日期']} />
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

function initialValues(filters: ProcessOrderSearchFilters): SearchFormValues {
  const dateFrom = filters.dateFrom ? dayjs(filters.dateFrom) : undefined
  const dateTo = filters.dateTo ? dayjs(filters.dateTo) : undefined
  return {
    keyword: filters.keyword,
    customerUuid: filters.customerUuid,
    dateRange: dateFrom?.isValid() && dateTo?.isValid() ? [dateFrom, dateTo] : null,
  }
}
