import { DownloadOutlined, FilterOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import { Badge, Button, DatePicker, Form, Select } from 'antd'
import type { Dayjs } from 'dayjs'
import { useState } from 'react'
import type { ReactNode } from 'react'
import type { Customer } from '../../../types/customer'
import type { Machine } from '../../../types/machine'
import type { Paper } from '../../../types/paper'
import { DICT_TYPES, invoiceFallbackOptions, settleFallbackOptions } from '../../systemConfig/configFallbacks'
import { useNumberDictOptions } from '../../systemConfig/hooks/useRuntimeDictOptions'
import ReportPeriodPreset from './ReportPeriodPreset'
import ReportCustomerFilter from './ReportCustomerFilter'
import ReportMachineFilter from './ReportMachineFilter'
import ReportPaperFilter from './ReportPaperFilter'

const { RangePicker } = DatePicker

export interface ReportFilterValues {
  customerUuid?: string
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
  actions: {
    export: () => void
    refresh: () => void
    submit: (values: ReportFilterValues) => void
  }
  data: { customers: Customer[]; machines: Machine[]; papers: Paper[] }
  initialValues: ReportFilterValues
  status: { exporting: boolean; loading: boolean }
  headerActions?: ReactNode
  mode?: 'overview' | 'production' | 'quality-loss' | 'explorer'
}

export default function ReportFilterBar(props: Props) {
  const [form] = Form.useForm<ReportFilterValues>()
  const [advancedOpen, setAdvancedOpen] = useState(false)
  const values = Form.useWatch([], form)
  const advancedCount = countAdvancedFilters(values)
  const { options: invoiceOptions } = useNumberDictOptions(DICT_TYPES.invoiceType, invoiceFallbackOptions)
  const { options: settleOptions } = useNumberDictOptions(DICT_TYPES.settleType, settleFallbackOptions)

  const reset = () => {
    form.resetFields()
    props.actions.submit(props.initialValues)
  }

  const applyPeriod = (period: [Dayjs, Dayjs]) => {
    form.setFieldValue('period', period)
    form.submit()
  }

  return (
    <div className="report-header">
      <p className="report-header__description">{descriptionByMode[props.mode ?? 'overview']}</p>
      <div className="report-header__actions">
        <Button icon={<ReloadOutlined />} onClick={props.actions.refresh}>刷新</Button>
        {props.headerActions}
        <Button icon={<DownloadOutlined />} loading={props.status.exporting} onClick={props.actions.export}>
          导出 Excel
        </Button>
      </div>
      <Form<ReportFilterValues>
        className="report-filter"
        form={form}
        initialValues={props.initialValues}
        layout="vertical"
        onFinish={props.actions.submit}
      >
        <div className="report-filter__core">
          <Form.Item className="report-filter__period" label="统计周期（归属日期）">
            <div className="report-filter__period-control">
              <ReportPeriodPreset onApply={applyPeriod} />
              <Form.Item name="period" noStyle><RangePicker allowClear={false} /></Form.Item>
            </div>
          </Form.Item>
          <Form.Item label="客户" name="customerUuid">
            <ReportCustomerFilter initial={props.data.customers} loading={props.status.loading} />
          </Form.Item>
          <Form.Item label="产品 / 品名" name="paperName">
            <ReportPaperFilter initial={props.data.papers} />
          </Form.Item>
          <Form.Item className="report-filter__actions" label=" ">
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
            <Button icon={<FilterOutlined />} aria-expanded={advancedOpen}
              onClick={() => setAdvancedOpen((open) => !open)}>
              <span className="report-filter__advanced-trigger">
                更多筛选
                {advancedCount > 0 && <Badge count={advancedCount} size="small" />}
              </span>
            </Button>
          </Form.Item>
        </div>
        {advancedOpen && (
          <div className="report-filter__advanced">
            <Form.Item label="主工艺" name="mainStepType">
              <Select allowClear options={mainStepOptions} placeholder="全部工艺" />
            </Form.Item>
            <Form.Item label="加工方式" name="processMode">
              <Select allowClear options={processModeOptions} placeholder="全部方式" />
            </Form.Item>
            <Form.Item label="机台" name="machineUuid">
              <ReportMachineFilter initial={props.data.machines} />
            </Form.Item>
            {(props.mode ?? 'overview') === 'overview' && <>
              <Form.Item label="开票" name="isInvoice">
                <Select allowClear options={invoiceOptions} placeholder="全部" />
              </Form.Item>
              <Form.Item label="结算方式" name="settleType">
                <Select allowClear options={settleOptions} placeholder="全部" />
              </Form.Item>
            </>}
            <Form.Item label="单据状态" name="orderStatus">
              <Select allowClear options={statusOptions} placeholder="默认完成及已结算" />
            </Form.Item>
            <Form.Item className="report-filter__reset" label=" ">
              <Button type="link" onClick={reset}>重置全部条件</Button>
            </Form.Item>
          </div>
        )}
      </Form>
    </div>
  )
}

function countAdvancedFilters(values?: Partial<ReportFilterValues>) {
  const advancedValues = [values?.mainStepType, values?.processMode, values?.machineUuid,
    values?.isInvoice, values?.settleType, values?.orderStatus]
  return advancedValues.filter((value) => value !== undefined && value !== null && value !== '').length
}

const mainStepOptions = [{ value: 1, label: '锯纸' }, { value: 2, label: '复卷' }]
const processModeOptions = [
  { value: 1, label: '标准加工' }, { value: 2, label: '现场定尺' }, { value: 3, label: '直发' },
]
const statusOptions = [
  { value: 0, label: '草稿' }, { value: 1, label: '待下发' }, { value: 2, label: '加工中' },
  { value: 3, label: '待回录' }, { value: 4, label: '已完成' }, { value: 5, label: '已结算' },
  { value: 6, label: '已作废' },
]

const descriptionByMode = {
  overview: '按归属日期快速分析加工应收、吨位、损耗与回款。',
  production: '聚焦投入、成品产出、工艺结构与机台负荷。',
  'quality-loss': '聚焦损耗趋势、纸种差异与高损耗加工单追溯。',
  explorer: '自定义维度与指标组合，支持从分组行继续钻取。',
} satisfies Record<NonNullable<Props['mode']>, string>
