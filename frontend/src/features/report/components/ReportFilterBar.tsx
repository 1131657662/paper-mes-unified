import { AutoComplete, Button, DatePicker, Form, Select } from 'antd'
import { DownloadOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import type { Customer } from '../../../types/customer'
import type { Machine } from '../../../types/machine'
import type { Paper } from '../../../types/paper'
import type { ReportDimension } from '../../../types/report'
import { DICT_TYPES, invoiceFallbackOptions, settleFallbackOptions } from '../../systemConfig/configFallbacks'
import { useNumberDictOptions } from '../../systemConfig/hooks/useRuntimeDictOptions'

const { RangePicker } = DatePicker

export interface ReportFilterValues {
  customerUuid?: string
  dimension: ReportDimension
  isInvoice?: number
  machineUuid?: string
  mainStepType?: number
  orderStatus?: number
  paperName?: string
  period: [Dayjs, Dayjs]
  processMode?: number
  settleType?: number
}

interface Props {
  customers: Customer[]
  exporting: boolean
  initialValues: ReportFilterValues
  loading: boolean
  machines: Machine[]
  onExport: () => void
  onRefresh: () => void
  onSubmit: (values: ReportFilterValues) => void
  papers: Paper[]
}

export default function ReportFilterBar(props: Props) {
  const { options: invoiceOptions } = useNumberDictOptions(DICT_TYPES.invoiceType, invoiceFallbackOptions)
  const { options: settleTypeOptions } = useNumberDictOptions(DICT_TYPES.settleType, settleFallbackOptions)

  return (
    <div className="report-header">
      <p className="report-header__description">
        按客户、产品、工艺、机台、开票和结算方式分析加工应收、吨位、损耗与未收款。
      </p>
      <div className="report-header__actions">
        <Button icon={<ReloadOutlined />} onClick={props.onRefresh}>
          刷新
        </Button>
        <Button icon={<DownloadOutlined />} loading={props.exporting} onClick={props.onExport}>
          导出 Excel
        </Button>
      </div>
      <Form<ReportFilterValues>
        className="report-filter"
        initialValues={props.initialValues}
        layout="vertical"
        onFinish={props.onSubmit}
      >
        <Form.Item label="统计周期" name="period">
          <RangePicker allowClear={false} />
        </Form.Item>
        <Form.Item label="汇总维度" name="dimension">
          <Select options={dimensionOptions} />
        </Form.Item>
        <Form.Item label="客户" name="customerUuid">
          <Select
            allowClear
            showSearch
            loading={props.loading}
            optionFilterProp="label"
            placeholder="全部客户"
            options={props.customers.map((item) => ({ value: item.uuid, label: item.customerName }))}
          />
        </Form.Item>
        <Form.Item label="产品/品名" name="paperName">
          <AutoComplete
            allowClear
            placeholder="全部产品"
            options={props.papers.map((item) => ({ value: item.paperName, label: item.paperName }))}
          />
        </Form.Item>
        <Form.Item label="主工艺" name="mainStepType">
          <Select allowClear options={mainStepOptions} placeholder="全部工艺" />
        </Form.Item>
        <Form.Item label="加工方式" name="processMode">
          <Select allowClear options={processModeOptions} placeholder="全部方式" />
        </Form.Item>
        <Form.Item label="机台" name="machineUuid">
          <Select
            allowClear
            showSearch
            optionFilterProp="label"
            placeholder="全部机台"
            options={props.machines.map((item) => ({ value: item.uuid, label: item.machineName }))}
          />
        </Form.Item>
        <Form.Item label="开票" name="isInvoice">
          <Select allowClear options={invoiceOptions} placeholder="全部" />
        </Form.Item>
        <Form.Item label="结算方式" name="settleType">
          <Select allowClear options={settleTypeOptions} placeholder="全部" />
        </Form.Item>
        <Form.Item label="单据状态" name="orderStatus">
          <Select allowClear options={statusOptions} placeholder="默认完成及已结算" />
        </Form.Item>
        <Form.Item className="report-filter__actions" label=" ">
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
            查询
          </Button>
        </Form.Item>
      </Form>
    </div>
  )
}

const dimensionOptions = [
  { value: 'customer', label: '客户汇总' },
  { value: 'paper', label: '产品汇总' },
  { value: 'process', label: '工艺汇总' },
  { value: 'machine', label: '机台汇总' },
  { value: 'month', label: '月度汇总' },
  { value: 'invoice', label: '开票汇总' },
  { value: 'settleType', label: '结算方式' },
  { value: 'status', label: '状态汇总' },
] satisfies Array<{ value: ReportDimension; label: string }>

const mainStepOptions = [
  { value: 1, label: '锯纸' },
  { value: 2, label: '复卷' },
]

const processModeOptions = [
  { value: 1, label: '标准加工' },
  { value: 2, label: '现场定尺' },
  { value: 3, label: '直发' },
]

const statusOptions = [
  { value: 0, label: '草稿' },
  { value: 1, label: '待下发' },
  { value: 2, label: '加工中' },
  { value: 3, label: '待回录' },
  { value: 4, label: '已完成' },
  { value: 5, label: '已结算' },
]
