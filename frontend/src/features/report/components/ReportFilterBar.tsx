import { Button, DatePicker, Form, Select } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import type { Customer } from '../../../types/customer'

const { RangePicker } = DatePicker

export interface ReportFilterValues {
  customerUuid?: string
  period: [Dayjs, Dayjs]
}

interface Props {
  customers: Customer[]
  initialValues: ReportFilterValues
  loading: boolean
  onRefresh: () => void
  onValuesChange: (_: Partial<ReportFilterValues>, values: ReportFilterValues) => void
}

export default function ReportFilterBar({
  customers,
  initialValues,
  loading,
  onRefresh,
  onValuesChange,
}: Props) {
  return (
    <div className="report-header">
      <div className="report-header__title">
        <h2>经营与生产统计</h2>
        <p>按完成加工单统计吨位、金额、损耗和机台产出，辅助对账与生产复盘。</p>
      </div>
      <Button icon={<ReloadOutlined />} onClick={onRefresh}>
        刷新
      </Button>
      <Form<ReportFilterValues>
        className="report-filter"
        initialValues={initialValues}
        layout="vertical"
        onValuesChange={onValuesChange}
      >
        <Form.Item label="统计周期" name="period">
          <RangePicker allowClear={false} />
        </Form.Item>
        <Form.Item label="客户" name="customerUuid">
          <Select
            allowClear
            showSearch
            loading={loading}
            optionFilterProp="label"
            placeholder="全部客户"
            options={customers.map((item) => ({ value: item.uuid, label: item.customerName }))}
          />
        </Form.Item>
      </Form>
    </div>
  )
}
